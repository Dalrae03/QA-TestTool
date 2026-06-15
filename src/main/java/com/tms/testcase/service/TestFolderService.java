package com.tms.testcase.service;

import com.tms.global.exception.InvalidRequestException;
import com.tms.testcase.dto.TestFolderRequest;
import com.tms.testcase.dto.TestFolderResponse;
import com.tms.testcase.entity.TestFolder;
import com.tms.testcase.repository.TestCaseRepository;
import com.tms.testcase.repository.TestFolderRepository;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class TestFolderService {

    private final TestFolderRepository testFolderRepository;
    private final TestCaseRepository testCaseRepository;

    public TestFolderService(TestFolderRepository testFolderRepository, TestCaseRepository testCaseRepository) {
        this.testFolderRepository = testFolderRepository;
        this.testCaseRepository = testCaseRepository;
    }

    public List<TestFolderResponse> getRootFolders() {
        return testFolderRepository.findAllByParentIsNullOrderByNameAsc()
                .stream()
                .map(TestFolderResponse::from)
                .toList();
    }

    public TestFolderResponse getFolder(Long id) {
        return TestFolderResponse.from(findById(id));
    }

    @Transactional
    public TestFolderResponse createFolder(TestFolderRequest request) {
        TestFolder parent = loadParent(request.parentId());
        TestFolder folder = new TestFolder(request.name(), parent);
        return TestFolderResponse.from(testFolderRepository.save(folder));
    }

    @Transactional
    public TestFolderResponse updateFolder(Long id, TestFolderRequest request) {
        TestFolder folder = findById(id);
        if (request.parentId() != null && request.parentId().equals(id)) {
            throw new InvalidRequestException("폴더는 자기 자신의 하위 폴더가 될 수 없습니다.");
        }
        if (request.parentId() != null && isDescendant(id, request.parentId())) {
            throw new InvalidRequestException("순환 참조가 발생합니다. 하위 폴더를 상위 폴더로 지정할 수 없습니다.");
        }
        TestFolder parent = loadParent(request.parentId());
        folder.rename(request.name());
        folder.move(parent);
        return TestFolderResponse.from(folder);
    }

    @Transactional
    public void deleteFolder(Long id) {
        TestFolder folder = findById(id);
        if (!folder.getChildren().isEmpty()) {
            throw new InvalidRequestException("하위 폴더가 있는 폴더는 삭제할 수 없습니다. 하위 폴더를 먼저 삭제하세요.");
        }
        if (testCaseRepository.existsByFolderId(id)) {
            throw new InvalidRequestException("테스트 케이스가 포함된 폴더는 삭제할 수 없습니다. 케이스를 먼저 이동하거나 삭제하세요.");
        }
        testFolderRepository.delete(folder);
    }

    public TestFolder findById(Long id) {
        return testFolderRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("폴더를 찾을 수 없습니다. id=" + id));
    }

    private TestFolder loadParent(Long parentId) {
        if (parentId == null) return null;
        return testFolderRepository.findById(parentId)
                .orElseThrow(() -> new InvalidRequestException("존재하지 않는 부모 폴더입니다. id=" + parentId));
    }

    /** targetId가 sourceId의 자손인지 확인 (순환 감지) */
    private boolean isDescendant(Long sourceId, Long targetId) {
        TestFolder current = testFolderRepository.findById(targetId).orElse(null);
        while (current != null) {
            if (current.getId().equals(sourceId)) return true;
            current = current.getParent();
        }
        return false;
    }
}
