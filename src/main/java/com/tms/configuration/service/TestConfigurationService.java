package com.tms.configuration.service;

import com.tms.audit.entity.AuditAction;
import com.tms.audit.service.AuditLogService;
import com.tms.configuration.dto.TestConfigurationRequest;
import com.tms.configuration.dto.TestConfigurationResponse;
import com.tms.configuration.entity.TestConfiguration;
import com.tms.configuration.repository.TestConfigurationRepository;
import com.tms.environment.entity.ServerEnvironment;
import com.tms.environment.repository.ServerEnvironmentRepository;
import com.tms.global.exception.InvalidRequestException;
import com.tms.testcase.repository.TestCaseRepository;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class TestConfigurationService {

    private final TestConfigurationRepository configurationRepository;
    private final ServerEnvironmentRepository serverEnvironmentRepository;
    private final TestCaseRepository testCaseRepository;
    private final AuditLogService auditLogService;

    public TestConfigurationService(
            TestConfigurationRepository configurationRepository,
            ServerEnvironmentRepository serverEnvironmentRepository,
            TestCaseRepository testCaseRepository,
            AuditLogService auditLogService
    ) {
        this.configurationRepository = configurationRepository;
        this.serverEnvironmentRepository = serverEnvironmentRepository;
        this.testCaseRepository = testCaseRepository;
        this.auditLogService = auditLogService;
    }

    public List<TestConfigurationResponse> getAll() {
        return configurationRepository.findAllByOrderByNameAsc().stream()
                .map(TestConfigurationResponse::from).toList();
    }

    public TestConfigurationResponse get(Long id) {
        return TestConfigurationResponse.from(findById(id));
    }

    @Transactional
    public TestConfigurationResponse create(TestConfigurationRequest request) {
        validateUniqueName(request.name(), null);
        TestConfiguration configuration = new TestConfiguration(
                request.name().trim(), loadEnvironment(request.serverEnvironmentId()),
                request.os(), request.osVersion(),
                request.browser(), request.browserVersion(),
                request.device(), request.runtimeVersion(), request.dbVersion(),
                request.active()
        );
        TestConfiguration saved = configurationRepository.save(configuration);
        auditLogService.log(AuditLogService.CONFIGURATION, saved.getId(), AuditAction.CREATED,
                "Configuration '" + saved.getName() + "'이(가) 생성되었습니다.");
        return TestConfigurationResponse.from(saved);
    }

    @Transactional
    public TestConfigurationResponse update(Long id, TestConfigurationRequest request) {
        TestConfiguration configuration = findById(id);
        validateUniqueName(request.name(), id);
        configuration.update(
                request.name().trim(), loadEnvironment(request.serverEnvironmentId()),
                request.os(), request.osVersion(),
                request.browser(), request.browserVersion(),
                request.device(), request.runtimeVersion(), request.dbVersion(),
                request.active()
        );
        auditLogService.log(AuditLogService.CONFIGURATION, id, AuditAction.UPDATED,
                "Configuration '" + configuration.getName() + "'이(가) 수정되었습니다.");
        return TestConfigurationResponse.from(configuration);
    }

    @Transactional
    public void delete(Long id) {
        TestConfiguration configuration = findById(id);
        testCaseRepository.findAllByTestConfigurationId(id)
                .forEach(testCase -> testCase.changeTestConfiguration(null));
        configurationRepository.delete(configuration);
        auditLogService.log(AuditLogService.CONFIGURATION, id, AuditAction.DELETED,
                "Configuration '" + configuration.getName() + "'이(가) 삭제되었습니다.");
    }

    @Transactional
    public void clearServerEnvironment(Long serverEnvironmentId) {
        configurationRepository.findAllByServerEnvironmentId(serverEnvironmentId)
                .forEach(configuration -> configuration.update(
                        configuration.getName(), null,
                        configuration.getOs(), configuration.getOsVersion(),
                        configuration.getBrowser(), configuration.getBrowserVersion(),
                        configuration.getDevice(), configuration.getRuntimeVersion(), configuration.getDbVersion(),
                        configuration.isActive()
                ));
    }

    private TestConfiguration findById(Long id) {
        return configurationRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("TestConfiguration not found. id=" + id));
    }

    private ServerEnvironment loadEnvironment(Long id) {
        if (id == null) return null;
        return serverEnvironmentRepository.findById(id)
                .orElseThrow(() -> new InvalidRequestException("존재하지 않는 서버 환경입니다. id=" + id));
    }

    private void validateUniqueName(String name, Long id) {
        String normalized = name.trim();
        boolean exists = id == null
                ? configurationRepository.existsByNameIgnoreCase(normalized)
                : configurationRepository.existsByNameIgnoreCaseAndIdNot(normalized, id);
        if (exists) throw new IllegalArgumentException("이미 존재하는 configuration입니다: " + normalized);
    }
}
