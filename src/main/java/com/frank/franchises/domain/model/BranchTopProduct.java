package com.frank.franchises.domain.model;

/**
 * Representa el resultado de dominio del producto con mayor stock por sucursal.
 */
public record BranchTopProduct(
    String branchId,
    String branchName,
    Product product
) {}
