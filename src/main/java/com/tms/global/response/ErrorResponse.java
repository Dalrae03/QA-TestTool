package com.tms.global.response;

import java.util.Map;

public record ErrorResponse(
        String message,
        Map<String, String> errors
) {
    public static ErrorResponse of(String message) {
        return new ErrorResponse(message, null);
    }

    public static ErrorResponse of(String message, Map<String, String> errors) {
        return new ErrorResponse(message, errors);
    }
}
