package com.frank.franchises.infrastructure.adapter.in.web.mapper;

import com.frank.franchises.domain.model.Branch;
import com.frank.franchises.domain.model.BranchTopProduct;
import com.frank.franchises.domain.model.Franchise;
import com.frank.franchises.domain.model.Product;
import com.frank.franchises.infrastructure.adapter.in.web.dto.BranchResponse;
import com.frank.franchises.infrastructure.adapter.in.web.dto.BranchTopProductResponse;
import com.frank.franchises.infrastructure.adapter.in.web.dto.FranchiseResponse;
import com.frank.franchises.infrastructure.adapter.in.web.dto.ProductResponse;

import java.util.Collections;
import java.util.List;

public final class FranchiseRestMapper {

    private FranchiseRestMapper() {
    }

    public static FranchiseResponse toResponse(Franchise domain) {
        if (domain == null) {
            return null;
        }

        List<BranchResponse> branches = domain.getBranches() == null
                ? Collections.emptyList()
                : domain.getBranches().stream()
                        .map(FranchiseRestMapper::toResponse)
                        .toList();

        return new FranchiseResponse(
                domain.getId(),
                domain.getName(),
                branches
        );
    }

    public static BranchResponse toResponse(Branch domain) {
        if (domain == null) {
            return null;
        }

        List<ProductResponse> products = domain.getProducts() == null
                ? Collections.emptyList()
                : domain.getProducts().stream()
                        .map(FranchiseRestMapper::toResponse)
                        .toList();

        return new BranchResponse(
                domain.getId(),
                domain.getName(),
                products
        );
    }

    public static ProductResponse toResponse(Product domain) {
        if (domain == null) {
            return null;
        }

        return new ProductResponse(
                domain.getId(),
                domain.getName(),
                domain.getStock() != null ? domain.getStock() : 0
        );
    }

    public static BranchTopProductResponse toResponse(BranchTopProduct domain) {
        if (domain == null) {
            return null;
        }

        return new BranchTopProductResponse(
                domain.branchId(),
                domain.branchName(),
                toResponse(domain.product())
        );
    }
}
