package com.tms.testcase.service;

import com.tms.testcase.dto.AreaTagResponse;
import com.tms.testcase.dto.CreateAreaTagRequest;
import com.tms.testcase.entity.AreaTag;
import com.tms.testcase.repository.AreaTagRepository;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AreaTagService {

    private final AreaTagRepository areaTagRepository;

    public AreaTagService(AreaTagRepository areaTagRepository) {
        this.areaTagRepository = areaTagRepository;
    }

    public List<AreaTagResponse> getAllAreaTags() {
        return areaTagRepository.findAll()
                .stream()
                .map(AreaTagResponse::from)
                .toList();
    }

    @Transactional
    public AreaTagResponse createAreaTag(CreateAreaTagRequest request) {
        if (areaTagRepository.existsByName(request.name())) {
            throw new IllegalArgumentException("이미 존재하는 태그입니다: " + request.name());
        }
        return AreaTagResponse.from(areaTagRepository.save(new AreaTag(request.name())));
    }

    @Transactional
    public void deleteAreaTag(Long id) {
        AreaTag tag = areaTagRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("AreaTag not found. id=" + id));
        areaTagRepository.delete(tag);
    }
}
