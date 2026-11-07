package com.frank.franchises.infrastructure.adapter.in.web.error;

import java.time.Instant;

public record ErrorResponse(
    Instant timestamp,
    int status,
    String error,
    String message,
    String path
) {}
