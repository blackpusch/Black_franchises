package com.frank.franchises.infrastructure.adapter.in.web.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateFranchiseRequest(
    @NotBlank(message = "El nombre de la franquicia es obligatorio")
    String name
) {}
