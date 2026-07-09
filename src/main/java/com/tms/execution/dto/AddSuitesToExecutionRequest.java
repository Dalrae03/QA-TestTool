package com.tms.execution.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

/** 기존 테스트런에 스위트를 추가할 때 — 선택한 스위트들의 테스트케이스를 병합해 항목으로 붙인다. */
public record AddSuitesToExecutionRequest(
        @NotEmpty List<Long> suiteIds
) {
}
