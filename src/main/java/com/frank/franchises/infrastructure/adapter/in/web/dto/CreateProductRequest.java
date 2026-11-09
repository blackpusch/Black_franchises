package com.frank.franchises.infrastructure.adapter.in.web.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import static com.frank.franchises.domain.port.in.FranchiseUseCase.NAME_MAX_LENGTH;

public record CreateProductRequest(
    @NotBlank(message = "El nombre del producto es obligatorio")
    @Size(max = NAME_MAX_LENGTH, message = "El nombre del producto no puede exceder {max} caracteres")
    String name,

    @NotNull(message = "El stock es obligatorio")
    @Min(value = 0, message = "El stock debe ser mayor o igual a 0")
    Integer stock
) {}
