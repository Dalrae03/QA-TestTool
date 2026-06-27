package com.tms.testsuite.entity;

import com.tms.testcase.entity.TestCase;
import com.tms.testplan.entity.TestPlan;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "test_suites")
public class TestSuite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** B안: testPlan은 선택적 — 테스트 플랜 없이 독립 스위트로 사용 가능 */
    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "test_plan_id")
    private TestPlan testPlan;

    @Column(nullable = false, length = 200)
    private String name;

    @Lob
    private String description;

    @ManyToMany
    @JoinTable(
            name = "test_suite_cases",
            joinColumns = @JoinColumn(name = "test_suite_id"),
            inverseJoinColumns = @JoinColumn(name = "test_case_id")
    )
    private List<TestCase> testCases = new ArrayList<>();

    @Column(name = "project_id")
    private Long projectId;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    protected TestSuite() {}

    /** 플랜 연결 스위트 */
    public TestSuite(TestPlan testPlan, String name, String description, List<TestCase> testCases) {
        this.testPlan = testPlan;
        this.name = name;
        this.description = description;
        replaceTestCases(testCases);
    }

    /** 독립 스위트 (플랜 없음) */
    public TestSuite(String name, String description, List<TestCase> testCases) {
        this.name = name;
        this.description = description;
        replaceTestCases(testCases);
    }

    public void update(String name, String description, List<TestCase> testCases) {
        this.name = name;
        this.description = description;
        replaceTestCases(testCases);
    }

    public void detachFromPlan() {
        this.testPlan = null;
    }

    public void replaceTestCases(List<TestCase> newTestCases) {
        List<TestCase> newList = newTestCases != null ? newTestCases : List.of();
        Set<Long> newIds = newList.stream().map(TestCase::getId).collect(java.util.stream.Collectors.toSet());
        // DELETE: 새 목록에 없는 TC만 제거 (clear 대신 delta 방식 — Hibernate INSERT-before-DELETE 방지)
        this.testCases.removeIf(tc -> !newIds.contains(tc.getId()));
        // INSERT: 기존 목록에 없는 TC만 추가
        Set<Long> existingIds = this.testCases.stream().map(TestCase::getId).collect(java.util.stream.Collectors.toSet());
        for (TestCase tc : newList) {
            if (!existingIds.contains(tc.getId())) {
                this.testCases.add(tc);
            }
        }
    }

    public void removeTestCase(Long testCaseId) {
        this.testCases.removeIf(testCase -> testCase.getId().equals(testCaseId));
    }

    public Long getId() { return id; }
    public TestPlan getTestPlan() { return testPlan; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public List<TestCase> getTestCases() { return testCases; }
    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
