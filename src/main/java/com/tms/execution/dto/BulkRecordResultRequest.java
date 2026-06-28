package com.tms.execution.dto;

import com.tms.execution.entity.ResultStatus;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/** 여러 실행 아이템에 같은 결과를 한 번에 기록하기 위한 요청. */
public record BulkRecordResultRequest(
        @NotEmpty List<Long> itemIds,
        @NotNull ResultStatus status,
        String comment,
        String failureReason
) {}
