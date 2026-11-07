package com.frank.franchises.infrastructure.adapter.in.web.dto;

public record BranchTopProductResponse(
    String branchId,
    String branchName,
    ProductResponse product
) {}
