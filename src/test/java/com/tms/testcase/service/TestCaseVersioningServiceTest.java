package com.tms.testcase.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.tms.testcase.dto.CreateTestCaseRequest;
import com.tms.testcase.dto.UpdateTestCaseRequest;
import com.tms.testcase.dto.UpdateTestCaseStatusRequest;
import com.tms.testcase.entity.TestCasePriority;
import com.tms.testcase.entity.TestCaseStatus;
import com.tms.testcase.entity.TestCaseType;
import com.tms.testcase.repository.TestCaseRepository;
import com.tms.testcase.repository.TestCaseVersionRepository;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * 버전 스냅샷은 실제 변경이 있을 때만 생성되어야 한다.
 * (수정 없이 저장/같은 상태로 변경/같은 폴더로 이동 시 버전이 오르지 않아야 함)
 */
@SpringBootTest
class TestCaseVersioningServiceTest {

    @Autowired TestCaseService testCaseService;
    @Autowired TestCaseRepository testCaseRepository;
    @Autowired TestCaseVersionRepository testCaseVersionRepository;

    @AfterEach
    void tearDown() {
        testCaseVersionRepository.deleteAll();
        testCaseRepository.deleteAll();
    }

    @Test
    void doesNotCreateVersionWhenSavedWithoutChanges() {
        Long id = createCase();
        int baseline = versionCount(id); // 최초 생성 1건

        testCaseService.updateTestCase(id, sameValuesRequest());

        assertThat(versionCount(id)).isEqualTo(baseline);
    }

    @Test
    void createsExactlyOneVersionWhenContentChanges() {
        Long id = createCase();
        int baseline = versionCount(id);

        testCaseService.updateTestCase(id, requestWithTitle("로그인 성공 — 수정됨"));

        assertThat(versionCount(id)).isEqualTo(baseline + 1);
    }

    @Test
    void doesNotCreateVersionWhenStatusUnchanged() {
        Long id = createCase(); // 상태 READY
        int baseline = versionCount(id);

        testCaseService.updateTestCaseStatus(id, new UpdateTestCaseStatusRequest(TestCaseStatus.READY));

        assertThat(versionCount(id)).isEqualTo(baseline);
    }

    @Test
    void createsVersionWhenStatusActuallyChanges() {
        Long id = createCase(); // READY
        int baseline = versionCount(id);

        testCaseService.updateTestCaseStatus(id, new UpdateTestCaseStatusRequest(TestCaseStatus.COMPLETED));

        assertThat(versionCount(id)).isEqualTo(baseline + 1);
    }

    @Test
    void doesNotCreateVersionWhenFolderUnchanged() {
        Long id = createCase(); // 폴더 없음(null)
        int baseline = versionCount(id);

        testCaseService.moveToFolder(id, null);

        assertThat(versionCount(id)).isEqualTo(baseline);
    }

    private Long createCase() {
        return testCaseService.createTestCase(new CreateTestCaseRequest(
                TestCaseType.FUNCTIONAL, TestCasePriority.HIGH, TestCaseStatus.READY,
                "로그인 성공", "설명", "선행조건", "절차", null,
                null, null, null, List.of()
        )).id();
    }

    private UpdateTestCaseRequest sameValuesRequest() {
        return requestWithTitle("로그인 성공");
    }

    private UpdateTestCaseRequest requestWithTitle(String title) {
        return new UpdateTestCaseRequest(
                TestCaseType.FUNCTIONAL, TestCasePriority.HIGH, TestCaseStatus.READY,
                title, "설명", "선행조건", "절차", null,
                null, null, null, List.of()
        );
    }

    private int versionCount(Long id) {
        return testCaseService.getVersions(id).size();
    }
}
