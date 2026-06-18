package com.tms.testcase.repository;

import com.tms.testcase.entity.AreaTag;
import com.tms.testcase.entity.TestCase;
import com.tms.testcase.entity.TestCaseBrowser;
import com.tms.testcase.entity.TestCaseDevice;
import com.tms.testcase.entity.TestCaseOs;
import com.tms.testcase.entity.TestCasePriority;
import com.tms.testcase.entity.TestCaseStatus;
import com.tms.testcase.entity.TestCaseType;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;

public class TestCaseSpecification {

    private TestCaseSpecification() {
    }

    public static Specification<TestCase> hasType(TestCaseType type) {
        return (root, query, cb) -> cb.equal(root.get("type"), type);
    }

    public static Specification<TestCase> hasPriority(TestCasePriority priority) {
        return (root, query, cb) -> cb.equal(root.get("priority"), priority);
    }

    public static Specification<TestCase> hasStatus(TestCaseStatus status) {
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    public static Specification<TestCase> hasOs(TestCaseOs os) {
        return (root, query, cb) -> cb.equal(root.get("os"), os);
    }

    public static Specification<TestCase> hasBrowser(TestCaseBrowser browser) {
        return (root, query, cb) -> cb.equal(root.get("browser"), browser);
    }

    public static Specification<TestCase> hasDevice(TestCaseDevice device) {
        return (root, query, cb) -> cb.equal(root.get("device"), device);
    }

    public static Specification<TestCase> hasAreaTag(Long areaTagId) {
        return (root, query, cb) -> {
            query.distinct(true);
            Join<TestCase, AreaTag> join = root.join("areaTags", JoinType.INNER);
            return cb.equal(join.get("id"), areaTagId);
        };
    }

    public static Specification<TestCase> hasFolder(Long folderId) {
        return (root, query, cb) -> cb.equal(root.get("folder").get("id"), folderId);
    }

    public static Specification<TestCase> containsKeyword(String keyword) {
        return (root, query, cb) -> {
            String pattern = "%" + keyword.toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("title").as(String.class)), pattern),
                    cb.like(cb.lower(root.get("description").as(String.class)), pattern)
            );
        };
    }
}
