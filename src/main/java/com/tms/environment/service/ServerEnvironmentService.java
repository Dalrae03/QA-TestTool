package com.tms.environment.service;

import com.tms.audit.entity.AuditAction;
import com.tms.audit.service.AuditLogService;
import com.tms.configuration.service.TestConfigurationService;
import com.tms.environment.dto.ServerEnvironmentRequest;
import com.tms.environment.dto.ServerEnvironmentResponse;
import com.tms.environment.entity.ServerEnvironment;
import com.tms.environment.repository.ServerEnvironmentRepository;
import com.tms.testcase.repository.TestCaseRepository;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ServerEnvironmentService {

    private final ServerEnvironmentRepository serverEnvironmentRepository;
    private final TestCaseRepository testCaseRepository;
    private final TestConfigurationService testConfigurationService;
    private final AuditLogService auditLogService;

    public ServerEnvironmentService(
            ServerEnvironmentRepository serverEnvironmentRepository,
            TestCaseRepository testCaseRepository,
            TestConfigurationService testConfigurationService,
            AuditLogService auditLogService
    ) {
        this.serverEnvironmentRepository = serverEnvironmentRepository;
        this.testCaseRepository = testCaseRepository;
        this.testConfigurationService = testConfigurationService;
        this.auditLogService = auditLogService;
    }

    public List<ServerEnvironmentResponse> getAll() {
        return serverEnvironmentRepository.findAllByOrderByNameAsc().stream()
                .map(ServerEnvironmentResponse::from).toList();
    }

    public ServerEnvironmentResponse get(Long id) {
        return ServerEnvironmentResponse.from(findById(id));
    }

    @Transactional
    public ServerEnvironmentResponse create(ServerEnvironmentRequest request) {
        validateUniqueName(request.name(), null);
        ServerEnvironment environment = new ServerEnvironment(
                request.name().trim(), request.type(), request.baseUrl().trim(), request.description(), request.active()
        );
        ServerEnvironment saved = serverEnvironmentRepository.save(environment);
        auditLogService.log(AuditLogService.SERVER_ENVIRONMENT, saved.getId(), AuditAction.CREATED,
                "서버 환경 '" + saved.getName() + "'이(가) 생성되었습니다.");
        return ServerEnvironmentResponse.from(saved);
    }

    @Transactional
    public ServerEnvironmentResponse update(Long id, ServerEnvironmentRequest request) {
        ServerEnvironment environment = findById(id);
        validateUniqueName(request.name(), id);
        environment.update(
                request.name().trim(), request.type(), request.baseUrl().trim(), request.description(), request.active()
        );
        auditLogService.log(AuditLogService.SERVER_ENVIRONMENT, id, AuditAction.UPDATED,
                "서버 환경 '" + environment.getName() + "'이(가) 수정되었습니다.");
        return ServerEnvironmentResponse.from(environment);
    }

    @Transactional
    public void delete(Long id) {
        ServerEnvironment environment = findById(id);
        testCaseRepository.findAllByServerEnvironmentId(id)
                .forEach(testCase -> testCase.changeServerEnvironment(null));
        testConfigurationService.clearServerEnvironment(id);
        serverEnvironmentRepository.delete(environment);
        auditLogService.log(AuditLogService.SERVER_ENVIRONMENT, id, AuditAction.DELETED,
                "서버 환경 '" + environment.getName() + "'이(가) 삭제되었습니다.");
    }

    private ServerEnvironment findById(Long id) {
        return serverEnvironmentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("ServerEnvironment not found. id=" + id));
    }

    private void validateUniqueName(String name, Long id) {
        String normalized = name.trim();
        boolean exists = id == null
                ? serverEnvironmentRepository.existsByNameIgnoreCase(normalized)
                : serverEnvironmentRepository.existsByNameIgnoreCaseAndIdNot(normalized, id);
        if (exists) throw new IllegalArgumentException("이미 존재하는 서버 환경입니다: " + normalized);
    }
}
