package com.frank.franchises.infrastructure.adapter.in.web.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateBranchRequest(
    @NotBlank(message = "El nombre de la sucursal es obligatorio")
    String name
) {}
