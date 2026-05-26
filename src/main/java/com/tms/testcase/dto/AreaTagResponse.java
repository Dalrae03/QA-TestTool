package com.tms.testcase.dto;

import com.tms.testcase.entity.AreaTag;

public record AreaTagResponse(Long id, String name) {

    public static AreaTagResponse from(AreaTag tag) {
        return new AreaTagResponse(tag.getId(), tag.getName());
    }
}
