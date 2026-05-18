package com.tms.testcase.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

@Entity
@Table(name = "test_cases")
public class TestCase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TestCaseType type;

    @Column(nullable = false, length = 200)
    private String title;

    @Lob
    @Column(nullable = false)
    private String description;

    @Lob
    @Column(nullable = false)
    private String precondition;

    @Lob
    @Column(nullable = false)
    private String steps;

    @Lob
    @Column(nullable = false)
    private String expected;

    @Lob
    @Column
    private String notes;

    protected TestCase() {
    }

    public TestCase(
            TestCaseType type,
            String title,
            String description,
            String precondition,
            String steps,
            String expected,
            String notes
    ) {
        this.type = type;
        this.title = title;
        this.description = description;
        this.precondition = precondition;
        this.steps = steps;
        this.expected = expected;
        this.notes = notes;
    }

    public void update(
            TestCaseType type,
            String title,
            String description,
            String precondition,
            String steps,
            String expected,
            String notes
    ) {
        this.type = type;
        this.title = title;
        this.description = description;
        this.precondition = precondition;
        this.steps = steps;
        this.expected = expected;
        this.notes = notes;
    }

    public Long getId() {
        return id;
    }

    public TestCaseType getType() {
        return type;
    }

    public void setType(TestCaseType type) {
        this.type = type;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getPrecondition() {
        return precondition;
    }

    public void setPrecondition(String precondition) {
        this.precondition = precondition;
    }

    public String getSteps() {
        return steps;
    }

    public void setSteps(String steps) {
        this.steps = steps;
    }

    public String getExpected() {
        return expected;
    }

    public void setExpected(String expected) {
        this.expected = expected;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
