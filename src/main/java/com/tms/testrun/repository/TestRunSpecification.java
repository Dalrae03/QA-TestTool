package com.tms.testrun.repository;

import com.tms.execution.entity.ResultStatus;
import com.tms.testrun.entity.TestRun;
import java.time.LocalDateTime;
import org.springframework.data.jpa.domain.Specification;

public class TestRunSpecification {

    private TestRunSpecification() {
    }

    public static Specification<TestRun> hasTestCaseId(Long testCaseId) {
        return (root, query, cb) -> cb.equal(root.get("testCase").get("id"), testCaseId);
    }

    public static Specification<TestRun> hasStatus(ResultStatus status) {
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    public static Specification<TestRun> hasAssignee(String assignee) {
        return (root, query, cb) ->
                cb.like(cb.lower(root.get("assignee")), "%" + assignee.toLowerCase() + "%");
    }

    public static Specification<TestRun> executedFrom(LocalDateTime from) {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("executedAt"), from);
    }

    public static Specification<TestRun> executedTo(LocalDateTime to) {
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("executedAt"), to);
    }

    public static Specification<TestRun> containsKeyword(String keyword) {
        return (root, query, cb) -> {
            String pattern = "%" + keyword.toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("actualResult").as(String.class)), pattern),
                    cb.like(cb.lower(root.get("notes").as(String.class)), pattern),
                    cb.like(cb.lower(root.get("failureReason").as(String.class)), pattern)
            );
        };
    }
}
