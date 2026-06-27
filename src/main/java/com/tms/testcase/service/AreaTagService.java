package com.tms.testcase.service;

import com.tms.testcase.dto.AreaTagResponse;
import com.tms.testcase.dto.CreateAreaTagRequest;
import com.tms.testcase.entity.AreaTag;
import com.tms.testcase.entity.TestCase;
import com.tms.testcase.repository.AreaTagRepository;
import com.tms.testcase.repository.TestCaseRepository;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AreaTagService {

    private final AreaTagRepository areaTagRepository;
    private final TestCaseRepository testCaseRepository;

    public AreaTagService(AreaTagRepository areaTagRepository, TestCaseRepository testCaseRepository) {
        this.areaTagRepository = areaTagRepository;
        this.testCaseRepository = testCaseRepository;
    }

    public List<AreaTagResponse> getAllAreaTags(Long projectId) {
        List<AreaTag> tags = projectId != null
                ? areaTagRepository.findAllByProjectId(projectId)
                : areaTagRepository.findAll();
        return tags.stream().map(AreaTagResponse::from).toList();
    }

    @Transactional
    public AreaTagResponse createAreaTag(CreateAreaTagRequest request) {
        String normalizedName = request.name().trim();
        boolean exists = request.projectId() != null
                ? areaTagRepository.existsByNameIgnoreCaseAndProjectId(normalizedName, request.projectId())
                : areaTagRepository.existsByNameIgnoreCase(normalizedName);
        if (exists) {
            throw new IllegalArgumentException("이미 존재하는 태그입니다: " + normalizedName);
        }
        return AreaTagResponse.from(areaTagRepository.save(new AreaTag(normalizedName, request.projectId())));
    }

    @Transactional
    public void deleteAreaTag(Long id) {
        AreaTag tag = areaTagRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("AreaTag not found. id=" + id));

        List<TestCase> linkedTestCases = testCaseRepository.findAllByAreaTags_Id(id);
        for (TestCase testCase : linkedTestCases) {
            testCase.removeAreaTag(id);
        }

        areaTagRepository.delete(tag);
    }
}
