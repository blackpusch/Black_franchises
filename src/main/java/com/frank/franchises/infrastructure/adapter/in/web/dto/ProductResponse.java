package com.frank.franchises.infrastructure.adapter.in.web.dto;

public record ProductResponse(
    String id,
    String name,
    Integer stock
) {}
