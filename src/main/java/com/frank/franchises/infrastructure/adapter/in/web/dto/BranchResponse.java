package com.frank.franchises.infrastructure.adapter.in.web.dto;

import java.util.List;

public record BranchResponse(
    String id,
    String name,
    List<ProductResponse> products
) {}
