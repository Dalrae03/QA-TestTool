package com.tms.testcase.repository;

import com.tms.testcase.entity.TestCaseVersion;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TestCaseVersionRepository extends JpaRepository<TestCaseVersion, Long> {

    List<TestCaseVersion> findByTestCaseIdOrderByVersionNumberDesc(Long testCaseId);

    /** 목록 화면에서 케이스별 최신 버전 라벨을 한 번에 조회하기 위한 벌크 조회 (N+1 방지). */
    List<TestCaseVersion> findAllByTestCaseIdIn(List<Long> testCaseIds);

    Optional<TestCaseVersion> findTopByTestCaseIdOrderByVersionNumberDesc(Long testCaseId);

    Optional<TestCaseVersion> findByIdAndTestCaseId(Long id, Long testCaseId);

    /** asOf 시점에 "현재 버전"이었던 스냅샷 — 버전 스냅샷을 저장하기 전에 만들어진 런의 버전을 역추적할 때 쓴다. */
    Optional<TestCaseVersion> findFirstByTestCaseIdAndCreatedAtLessThanEqualOrderByCreatedAtDesc(
            Long testCaseId, LocalDateTime asOf);

    void deleteAllByTestCaseId(Long testCaseId);
}
