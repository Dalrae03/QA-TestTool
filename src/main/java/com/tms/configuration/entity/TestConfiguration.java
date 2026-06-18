package com.tms.configuration.entity;

import com.tms.environment.entity.ServerEnvironment;
import com.tms.testcase.entity.TestCaseBrowser;
import com.tms.testcase.entity.TestCaseDevice;
import com.tms.testcase.entity.TestCaseOs;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "test_configurations")
public class TestConfiguration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "server_environment_id")
    private ServerEnvironment serverEnvironment;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private TestCaseOs os;

    @Column(length = 50)
    private String osVersion;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private TestCaseBrowser browser;

    @Column(length = 50)
    private String browserVersion;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private TestCaseDevice device;

    @Column(length = 50)
    private String runtimeVersion;

    @Column(length = 50)
    private String dbVersion;

    @Column(nullable = false)
    private boolean active;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    protected TestConfiguration() {
    }

    public TestConfiguration(
            String name,
            ServerEnvironment serverEnvironment,
            TestCaseOs os,
            String osVersion,
            TestCaseBrowser browser,
            String browserVersion,
            TestCaseDevice device,
            String runtimeVersion,
            String dbVersion,
            boolean active
    ) {
        update(name, serverEnvironment, os, osVersion, browser, browserVersion, device, runtimeVersion, dbVersion, active);
    }

    public void update(
            String name,
            ServerEnvironment serverEnvironment,
            TestCaseOs os,
            String osVersion,
            TestCaseBrowser browser,
            String browserVersion,
            TestCaseDevice device,
            String runtimeVersion,
            String dbVersion,
            boolean active
    ) {
        this.name = name;
        this.serverEnvironment = serverEnvironment;
        this.os = os;
        this.osVersion = osVersion;
        this.browser = browser;
        this.browserVersion = browserVersion;
        this.device = device;
        this.runtimeVersion = runtimeVersion;
        this.dbVersion = dbVersion;
        this.active = active;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public ServerEnvironment getServerEnvironment() { return serverEnvironment; }
    public TestCaseOs getOs() { return os; }
    public String getOsVersion() { return osVersion; }
    public TestCaseBrowser getBrowser() { return browser; }
    public String getBrowserVersion() { return browserVersion; }
    public TestCaseDevice getDevice() { return device; }
    public String getRuntimeVersion() { return runtimeVersion; }
    public String getDbVersion() { return dbVersion; }
    public boolean isActive() { return active; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
