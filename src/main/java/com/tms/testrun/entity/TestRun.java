package com.tms.testrun.entity;

import com.tms.testcase.entity.TestCase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "test_runs")
public class TestRun {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "test_case_id", nullable = false)
    private TestCase testCase;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TestRunStatus status;

    @Lob
    @Column(nullable = false)
    private String actualResult;

    @Lob
    @Column
    private String notes;

    @Column(nullable = false)
    private LocalDateTime executedAt;

    protected TestRun() {
    }

    public TestRun(
            TestCase testCase,
            TestRunStatus status,
            String actualResult,
            String notes,
            LocalDateTime executedAt
    ) {
        this.testCase = testCase;
        this.status = status;
        this.actualResult = actualResult;
        this.notes = notes;
        this.executedAt = executedAt;
    }

    public Long getId() {
        return id;
    }

    public TestCase getTestCase() {
        return testCase;
    }

    public TestRunStatus getStatus() {
        return status;
    }

    public String getActualResult() {
        return actualResult;
    }

    public String getNotes() {
        return notes;
    }

    public LocalDateTime getExecutedAt() {
        return executedAt;
    }

    public void setStatus(TestRunStatus status) {
        this.status = status;
    }

    public void setActualResult(String actualResult) {
        this.actualResult = actualResult;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
