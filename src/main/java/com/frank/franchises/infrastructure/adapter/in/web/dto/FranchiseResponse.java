package com.frank.franchises.infrastructure.adapter.in.web.dto;

import java.util.List;

public record FranchiseResponse(
    String id,
    String name,
    List<BranchResponse> branches
) {}
