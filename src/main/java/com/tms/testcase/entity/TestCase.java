package com.tms.testcase.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "test_cases")
public class TestCase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TestCaseType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TestCasePriority priority;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TestCaseStatus status;

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

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private TestCaseOs os;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private TestCaseBrowser browser;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private TestCaseDevice device;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "test_case_area_tags",
            joinColumns = @JoinColumn(name = "test_case_id"),
            inverseJoinColumns = @JoinColumn(name = "area_tag_id")
    )
    private List<AreaTag> areaTags = new ArrayList<>();

    protected TestCase() {
    }

    public TestCase(
            TestCaseType type,
            TestCasePriority priority,
            TestCaseStatus status,
            String title,
            String description,
            String precondition,
            String steps,
            String expected,
            String notes,
            TestCaseOs os,
            TestCaseBrowser browser,
            TestCaseDevice device,
            List<AreaTag> areaTags
    ) {
        this.type = type;
        this.priority = priority;
        this.status = (status != null) ? status : TestCaseStatus.DRAFT;
        this.title = title;
        this.description = description;
        this.precondition = precondition;
        this.steps = steps;
        this.expected = expected;
        this.notes = notes;
        this.os = os;
        this.browser = browser;
        this.device = device;
        this.areaTags = (areaTags != null) ? new ArrayList<>(areaTags) : new ArrayList<>();
    }

    public void update(
            TestCaseType type,
            TestCasePriority priority,
            TestCaseStatus status,
            String title,
            String description,
            String precondition,
            String steps,
            String expected,
            String notes,
            TestCaseOs os,
            TestCaseBrowser browser,
            TestCaseDevice device,
            List<AreaTag> areaTags
    ) {
        this.type = type;
        this.priority = priority;
        this.status = status;
        this.title = title;
        this.description = description;
        this.precondition = precondition;
        this.steps = steps;
        this.expected = expected;
        this.notes = notes;
        this.os = os;
        this.browser = browser;
        this.device = device;
        this.areaTags.clear();
        if (areaTags != null) {
            this.areaTags.addAll(areaTags);
        }
    }

    public void updateStatus(TestCaseStatus status) {
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public TestCaseType getType() {
        return type;
    }

    public TestCasePriority getPriority() {
        return priority;
    }

    public TestCaseStatus getStatus() {
        return status;
    }

    public void setType(TestCaseType type) {
        this.type = type;
    }

    public void setPriority(TestCasePriority priority) {
        this.priority = priority;
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

    public TestCaseOs getOs() {
        return os;
    }

    public TestCaseBrowser getBrowser() {
        return browser;
    }

    public TestCaseDevice getDevice() {
        return device;
    }

    public List<AreaTag> getAreaTags() {
        return areaTags;
    }
}
