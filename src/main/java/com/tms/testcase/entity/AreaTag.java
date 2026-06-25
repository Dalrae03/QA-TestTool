package com.tms.testcase.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "area_tags")
public class AreaTag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(name = "project_id")
    private Long projectId;

    protected AreaTag() {
    }

    public AreaTag(String name) {
        this.name = name;
    }

    public AreaTag(String name, Long projectId) {
        this.name = name;
        this.projectId = projectId;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Long getProjectId() {
        return projectId;
    }
}
