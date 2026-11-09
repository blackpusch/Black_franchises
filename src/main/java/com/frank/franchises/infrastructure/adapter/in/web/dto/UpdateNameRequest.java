package com.frank.franchises.infrastructure.adapter.in.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import static com.frank.franchises.domain.port.in.FranchiseUseCase.NAME_MAX_LENGTH;

public record UpdateNameRequest(
    @NotBlank(message = "El nuevo nombre es obligatorio")
    @Size(max = NAME_MAX_LENGTH, message = "El nuevo nombre no puede exceder {max} caracteres")
    String name
) {}
