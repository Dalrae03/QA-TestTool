package com.tms.testcase.entity;

import com.tms.configuration.entity.TestConfiguration;
import com.tms.defect.entity.Defect;
import com.tms.environment.entity.ServerEnvironment;
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
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

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

    @Column(length = 50)
    private String version;

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
    @Column(name = "expected", nullable = false)
    private String legacyExpected;

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

    @Column(length = 100)
    private String assignee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "folder_id")
    private TestFolder folder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "server_environment_id")
    private ServerEnvironment serverEnvironment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "test_configuration_id")
    private TestConfiguration testConfiguration;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "test_case_area_tags",
            joinColumns = @JoinColumn(name = "test_case_id"),
            inverseJoinColumns = @JoinColumn(name = "area_tag_id")
    )
    private List<AreaTag> areaTags = new ArrayList<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "test_case_defects",
            joinColumns = @JoinColumn(name = "test_case_id"),
            inverseJoinColumns = @JoinColumn(name = "defect_id")
    )
    private List<Defect> defects = new ArrayList<>();

    @Column(name = "project_id")
    private Long projectId;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column
    private LocalDateTime updatedAt;

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
            String notes,
            TestCaseOs os,
            TestCaseBrowser browser,
            TestCaseDevice device,
            List<AreaTag> areaTags
    ) {
        this(type, priority, status, title, description, precondition, steps, notes,
                os, browser, device, areaTags, null, null, null);
    }

    public TestCase(
            TestCaseType type,
            TestCasePriority priority,
            TestCaseStatus status,
            String title,
            String description,
            String precondition,
            String steps,
            String notes,
            TestCaseOs os,
            TestCaseBrowser browser,
            TestCaseDevice device,
            List<AreaTag> areaTags,
            ServerEnvironment serverEnvironment
    ) {
        this(type, priority, status, title, description, precondition, steps, notes,
                os, browser, device, areaTags, serverEnvironment, null, null);
    }

    public TestCase(
            TestCaseType type,
            TestCasePriority priority,
            TestCaseStatus status,
            String title,
            String description,
            String precondition,
            String steps,
            String notes,
            TestCaseOs os,
            TestCaseBrowser browser,
            TestCaseDevice device,
            List<AreaTag> areaTags,
            ServerEnvironment serverEnvironment,
            TestConfiguration testConfiguration
    ) {
        this(type, priority, status, title, description, precondition, steps, notes,
                os, browser, device, areaTags, serverEnvironment, testConfiguration, null);
    }

    public TestCase(
            TestCaseType type,
            TestCasePriority priority,
            TestCaseStatus status,
            String title,
            String description,
            String precondition,
            String steps,
            String notes,
            TestCaseOs os,
            TestCaseBrowser browser,
            TestCaseDevice device,
            List<AreaTag> areaTags,
            ServerEnvironment serverEnvironment,
            TestConfiguration testConfiguration,
            String assignee
    ) {
        this(type, priority, status, title, description, precondition, steps, notes,
                os, browser, device, areaTags, serverEnvironment, testConfiguration, assignee, null);
    }

    public TestCase(
            TestCaseType type,
            TestCasePriority priority,
            TestCaseStatus status,
            String title,
            String description,
            String precondition,
            String steps,
            String notes,
            TestCaseOs os,
            TestCaseBrowser browser,
            TestCaseDevice device,
            List<AreaTag> areaTags,
            ServerEnvironment serverEnvironment,
            TestConfiguration testConfiguration,
            String assignee,
            String version
    ) {
        this.type = type;
        this.priority = priority;
        this.status = (status != null) ? status : TestCaseStatus.DRAFT;
        this.title = title;
        this.description = description;
        this.precondition = precondition;
        this.steps = steps;
        this.legacyExpected = "";
        this.notes = notes;
        this.os = os;
        this.browser = browser;
        this.device = device;
        this.serverEnvironment = serverEnvironment;
        this.testConfiguration = testConfiguration;
        this.assignee = assignee;
        this.version = version;
        this.areaTags = (areaTags != null) ? new ArrayList<>(areaTags) : new ArrayList<>();
    }

    public void moveToFolder(TestFolder folder) {
        this.folder = folder;
    }

    public void update(
            TestCaseType type,
            TestCasePriority priority,
            TestCaseStatus status,
            String title,
            String description,
            String precondition,
            String steps,
            String notes,
            TestCaseOs os,
            TestCaseBrowser browser,
            TestCaseDevice device,
            List<AreaTag> areaTags
    ) {
        update(type, priority, status, title, description, precondition, steps, notes,
                os, browser, device, areaTags, null, null, null);
    }

    public void update(
            TestCaseType type,
            TestCasePriority priority,
            TestCaseStatus status,
            String title,
            String description,
            String precondition,
            String steps,
            String notes,
            TestCaseOs os,
            TestCaseBrowser browser,
            TestCaseDevice device,
            List<AreaTag> areaTags,
            ServerEnvironment serverEnvironment
    ) {
        update(type, priority, status, title, description, precondition, steps, notes,
                os, browser, device, areaTags, serverEnvironment, null, null);
    }

    public void update(
            TestCaseType type,
            TestCasePriority priority,
            TestCaseStatus status,
            String title,
            String description,
            String precondition,
            String steps,
            String notes,
            TestCaseOs os,
            TestCaseBrowser browser,
            TestCaseDevice device,
            List<AreaTag> areaTags,
            ServerEnvironment serverEnvironment,
            TestConfiguration testConfiguration
    ) {
        update(type, priority, status, title, description, precondition, steps, notes,
                os, browser, device, areaTags, serverEnvironment, testConfiguration, null);
    }

    public void update(
            TestCaseType type,
            TestCasePriority priority,
            TestCaseStatus status,
            String title,
            String description,
            String precondition,
            String steps,
            String notes,
            TestCaseOs os,
            TestCaseBrowser browser,
            TestCaseDevice device,
            List<AreaTag> areaTags,
            ServerEnvironment serverEnvironment,
            TestConfiguration testConfiguration,
            String assignee
    ) {
        update(type, priority, status, title, description, precondition, steps, notes,
                os, browser, device, areaTags, serverEnvironment, testConfiguration, assignee, null);
    }

    public void update(
            TestCaseType type,
            TestCasePriority priority,
            TestCaseStatus status,
            String title,
            String description,
            String precondition,
            String steps,
            String notes,
            TestCaseOs os,
            TestCaseBrowser browser,
            TestCaseDevice device,
            List<AreaTag> areaTags,
            ServerEnvironment serverEnvironment,
            TestConfiguration testConfiguration,
            String assignee,
            String version
    ) {
        this.type = type;
        this.priority = priority;
        this.status = status;
        this.title = title;
        this.description = description;
        this.precondition = precondition;
        this.steps = steps;
        this.legacyExpected = "";
        this.notes = notes;
        this.os = os;
        this.browser = browser;
        this.device = device;
        this.serverEnvironment = serverEnvironment;
        this.testConfiguration = testConfiguration;
        this.assignee = assignee;
        this.version = version;
        replaceAreaTags(areaTags);
        this.updatedAt = LocalDateTime.now();
    }

    public void updateStatus(TestCaseStatus status) {
        this.status = status;
        this.updatedAt = LocalDateTime.now();
    }

    public void replaceAreaTags(List<AreaTag> areaTags) {
        this.areaTags.clear();
        if (areaTags != null) {
            this.areaTags.addAll(areaTags);
        }
    }

    public void removeAreaTag(Long areaTagId) {
        this.areaTags.removeIf(tag -> tag.getId().equals(areaTagId));
    }

    public void changeServerEnvironment(ServerEnvironment serverEnvironment) {
        this.serverEnvironment = serverEnvironment;
    }

    public void changeTestConfiguration(TestConfiguration testConfiguration) {
        this.testConfiguration = testConfiguration;
    }

    public void linkDefect(Defect defect) {
        if (!defects.contains(defect)) {
            defects.add(defect);
        }
    }

    public void unlinkDefect(Defect defect) {
        defects.remove(defect);
    }

    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }

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

    public String getAssignee() { return assignee; }
    public String getVersion() { return version; }
    public TestFolder getFolder() { return folder; }
    public ServerEnvironment getServerEnvironment() { return serverEnvironment; }
    public TestConfiguration getTestConfiguration() { return testConfiguration; }

    public List<AreaTag> getAreaTags() {
        return areaTags;
    }

    public List<Defect> getDefects() {
        return defects;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
