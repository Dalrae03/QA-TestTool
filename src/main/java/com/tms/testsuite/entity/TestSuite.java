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
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "test_suites")
public class TestSuite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "test_plan_id", nullable = false)
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
    @OrderColumn(name = "case_order")
    private List<TestCase> testCases = new ArrayList<>();

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    protected TestSuite() {
    }

    public TestSuite(TestPlan testPlan, String name, String description, List<TestCase> testCases) {
        this.testPlan = testPlan;
        this.name = name;
        this.description = description;
        replaceTestCases(testCases);
    }

    public void update(String name, String description, List<TestCase> testCases) {
        this.name = name;
        this.description = description;
        replaceTestCases(testCases);
    }

    public void replaceTestCases(List<TestCase> testCases) {
        this.testCases.clear();
        if (testCases != null) this.testCases.addAll(testCases);
    }

    public void removeTestCase(Long testCaseId) {
        this.testCases.removeIf(testCase -> testCase.getId().equals(testCaseId));
    }

    public Long getId() { return id; }
    public TestPlan getTestPlan() { return testPlan; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public List<TestCase> getTestCases() { return testCases; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
