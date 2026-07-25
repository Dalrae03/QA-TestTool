package com.tms.testcase.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "test_folders")
public class TestFolder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String name;

    // TC 표시 ID 접두사(예: LOGIN, PAY). 비어 있으면 상위 폴더에서 상속한다(effectiveCode 참고).
    @Column(length = 20)
    private String code;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private TestFolder parent;

    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("name ASC")
    private List<TestFolder> children = new ArrayList<>();

    @Column(name = "project_id")
    private Long projectId;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    protected TestFolder() {
    }

    public TestFolder(String name, TestFolder parent) {
        this.name = name;
        this.parent = parent;
    }

    public TestFolder(String name, TestFolder parent, Long projectId) {
        this.name = name;
        this.parent = parent;
        this.projectId = projectId;
    }

    public void rename(String name) {
        this.name = name;
    }

    public void changeCode(String code) {
        this.code = code;
    }

    public void move(TestFolder newParent) {
        this.parent = newParent;
    }

    /** 자신의 코드가 없으면 상위 폴더로 거슬러 올라가 상속받은 접두사를 반환한다. 어디에도 없으면 null. */
    public String effectiveCode() {
        TestFolder cur = this;
        while (cur != null) {
            if (cur.code != null && !cur.code.isBlank()) {
                return cur.code;
            }
            cur = cur.parent;
        }
        return null;
    }

    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }
    public Long getId() { return id; }
    public String getName() { return name; }
    public String getCode() { return code; }
    public TestFolder getParent() { return parent; }
    public List<TestFolder> getChildren() { return children; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
