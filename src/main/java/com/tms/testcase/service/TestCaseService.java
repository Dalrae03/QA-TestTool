package com.tms.testcase.service;

import com.tms.global.exception.InvalidRequestException;
import com.tms.testcase.dto.CreateTestCaseRequest;
import com.tms.testcase.dto.TestCaseResponse;
import com.tms.testcase.dto.UpdateTestCaseRequest;
import com.tms.testcase.dto.UpdateTestCaseStatusRequest;
import com.tms.testcase.entity.AreaTag;
import com.tms.testcase.entity.TestCase;
import com.tms.testcase.entity.TestCaseBrowser;
import com.tms.testcase.entity.TestCaseDevice;
import com.tms.testcase.entity.TestCaseOs;
import com.tms.testcase.entity.TestCasePriority;
import com.tms.testcase.entity.TestCaseStatus;
import com.tms.testcase.entity.TestCaseType;
import com.tms.testcase.repository.AreaTagRepository;
import com.tms.testcase.repository.TestCaseRepository;
import com.tms.testcase.repository.TestCaseSpecification;
import jakarta.persistence.EntityNotFoundException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class TestCaseService {

    private final TestCaseRepository testCaseRepository;
    private final AreaTagRepository areaTagRepository;

    public TestCaseService(TestCaseRepository testCaseRepository, AreaTagRepository areaTagRepository) {
        this.testCaseRepository = testCaseRepository;
        this.areaTagRepository = areaTagRepository;
    }

    public List<TestCaseResponse> getAllTestCases(
            TestCaseType type,
            TestCasePriority priority,
            TestCaseStatus status,
            TestCaseOs os,
            TestCaseBrowser browser,
            TestCaseDevice device,
            Long areaTagId,
            String keyword
    ) {
        Specification<TestCase> spec = (root, query, cb) -> cb.conjunction();
        if (type != null) spec = spec.and(TestCaseSpecification.hasType(type));
        if (priority != null) spec = spec.and(TestCaseSpecification.hasPriority(priority));
        if (status != null) spec = spec.and(TestCaseSpecification.hasStatus(status));
        if (os != null) spec = spec.and(TestCaseSpecification.hasOs(os));
        if (browser != null) spec = spec.and(TestCaseSpecification.hasBrowser(browser));
        if (device != null) spec = spec.and(TestCaseSpecification.hasDevice(device));
        if (areaTagId != null) spec = spec.and(TestCaseSpecification.hasAreaTag(areaTagId));
        if (keyword != null && !keyword.isBlank()) spec = spec.and(TestCaseSpecification.containsKeyword(keyword));

        return testCaseRepository.findAll(spec)
                .stream()
                .map(TestCaseResponse::from)
                .toList();
    }

    public TestCaseResponse getTestCase(Long id) {
        return TestCaseResponse.from(findById(id));
    }

    @Transactional
    public TestCaseResponse createTestCase(CreateTestCaseRequest request) {
        List<AreaTag> areaTags = loadAreaTags(request.areaTagIds());
        TestCase testCase = new TestCase(
                request.type(),
                request.priority(),
                request.status(),
                request.title(),
                request.description(),
                request.precondition(),
                request.steps(),
                request.expected(),
                request.notes(),
                request.os(),
                request.browser(),
                request.device(),
                areaTags
        );
        return TestCaseResponse.from(testCaseRepository.save(testCase));
    }

    @Transactional
    public TestCaseResponse updateTestCase(Long id, UpdateTestCaseRequest request) {
        TestCase testCase = findById(id);
        List<AreaTag> areaTags = loadAreaTags(request.areaTagIds());
        testCase.update(
                request.type(),
                request.priority(),
                request.status(),
                request.title(),
                request.description(),
                request.precondition(),
                request.steps(),
                request.expected(),
                request.notes(),
                request.os(),
                request.browser(),
                request.device(),
                areaTags
        );
        return TestCaseResponse.from(testCase);
    }

    @Transactional
    public TestCaseResponse updateTestCaseStatus(Long id, UpdateTestCaseStatusRequest request) {
        TestCase testCase = findById(id);
        testCase.updateStatus(request.status());
        return TestCaseResponse.from(testCase);
    }

    @Transactional
    public void deleteTestCase(Long id) {
        TestCase testCase = findById(id);
        testCaseRepository.delete(testCase);
    }

    private TestCase findById(Long id) {
        return testCaseRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("TestCase not found. id=" + id));
    }

    private List<AreaTag> loadAreaTags(List<Long> areaTagIds) {
        if (areaTagIds == null || areaTagIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> uniqueTagIds = new ArrayList<>(new LinkedHashSet<>(areaTagIds));
        List<AreaTag> areaTags = areaTagRepository.findAllByIdIn(uniqueTagIds);
        Map<Long, AreaTag> areaTagsById = areaTags.stream()
                .collect(Collectors.toMap(AreaTag::getId, Function.identity()));

        if (areaTagsById.size() != uniqueTagIds.size()) {
            List<Long> missingTagIds = uniqueTagIds.stream()
                    .filter(id -> !areaTagsById.containsKey(id))
                    .toList();
            throw new InvalidRequestException("존재하지 않는 태그 ID가 포함되어 있습니다: " + missingTagIds);
        }

        return uniqueTagIds.stream()
                .map(areaTagsById::get)
                .toList();
    }
}
