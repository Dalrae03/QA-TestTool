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
import java.time.LocalDateTime;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.Length;

@Entity
@Table(name = "test_case_versions")
public class TestCaseVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long testCaseId;

    @Column(nullable = false)
    private Integer versionNumber;

    @Column(nullable = false, length = 200)
    private String label;

    @Column(length = 500)
    private String changeSummary;

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

    @Column(length = 50)
    private String version;

    @Lob
    @Column(nullable = false, length = Length.LONG32)
    private String description;

    @Lob
    @Column(nullable = false, length = Length.LONG32)
    private String precondition;

    @Lob
    @Column(nullable = false, length = Length.LONG32)
    private String steps;

    @Lob
    @Column(length = Length.LONG32)
    private String expectedResult;

    @Lob
    @Column(length = Length.LONG32)
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

    @Column(length = 100)
    private String assignee;

    @Column
    private Long folderId;

    @Column(length = 200)
    private String folderName;

    @Column
    private Long serverEnvironmentId;

    @Column(length = 100)
    private String serverEnvironmentName;

    @Column
    private Long testConfigurationId;

    @Column(length = 100)
    private String testConfigurationName;

    @Lob
    @Column(length = Length.LONG32)
    private String areaTagIds;

    @Lob
    @Column(length = Length.LONG32)
    private String areaTagNames;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    protected TestCaseVersion() {
    }

    public TestCaseVersion(
            Long testCaseId,
            Integer versionNumber,
            String label,
            String changeSummary,
            TestCaseType type,
            TestCasePriority priority,
            TestCaseStatus status,
            String title,
            String version,
            String description,
            String precondition,
            String steps,
            String notes,
            TestCaseOs os,
            TestCaseBrowser browser,
            TestCaseDevice device,
            String assignee,
            Long folderId,
            String folderName,
            Long serverEnvironmentId,
            String serverEnvironmentName,
            Long testConfigurationId,
            String testConfigurationName,
            String areaTagIds,
            String areaTagNames,
            String expectedResult
    ) {
        this.testCaseId = testCaseId;
        this.versionNumber = versionNumber;
        this.label = label;
        this.changeSummary = changeSummary;
        this.type = type;
        this.priority = priority;
        this.status = status;
        this.title = title;
        this.version = version;
        this.description = description;
        this.precondition = precondition;
        this.steps = steps;
        this.expectedResult = expectedResult;
        this.notes = notes;
        this.os = os;
        this.browser = browser;
        this.device = device;
        this.assignee = assignee;
        this.folderId = folderId;
        this.folderName = folderName;
        this.serverEnvironmentId = serverEnvironmentId;
        this.serverEnvironmentName = serverEnvironmentName;
        this.testConfigurationId = testConfigurationId;
        this.testConfigurationName = testConfigurationName;
        this.areaTagIds = areaTagIds;
        this.areaTagNames = areaTagNames;
    }

    public Long getId() { return id; }
    public Long getTestCaseId() { return testCaseId; }
    public Integer getVersionNumber() { return versionNumber; }
    public String getLabel() { return label; }
    public String getChangeSummary() { return changeSummary; }
    public TestCaseType getType() { return type; }
    public TestCasePriority getPriority() { return priority; }
    public TestCaseStatus getStatus() { return status; }
    public String getTitle() { return title; }
    public String getVersion() { return version; }
    public String getDescription() { return description; }
    public String getPrecondition() { return precondition; }
    public String getSteps() { return steps; }
    public String getExpectedResult() { return expectedResult; }
    public String getNotes() { return notes; }
    public TestCaseOs getOs() { return os; }
    public TestCaseBrowser getBrowser() { return browser; }
    public TestCaseDevice getDevice() { return device; }
    public String getAssignee() { return assignee; }
    public Long getFolderId() { return folderId; }
    public String getFolderName() { return folderName; }
    public Long getServerEnvironmentId() { return serverEnvironmentId; }
    public String getServerEnvironmentName() { return serverEnvironmentName; }
    public Long getTestConfigurationId() { return testConfigurationId; }
    public String getTestConfigurationName() { return testConfigurationName; }
    public String getAreaTagIds() { return areaTagIds; }
    public String getAreaTagNames() { return areaTagNames; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
