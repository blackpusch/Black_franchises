package com.frank.franchises.infrastructure.adapter.out.mongodb.mapper;

import com.frank.franchises.domain.model.Branch;
import com.frank.franchises.domain.model.Franchise;
import com.frank.franchises.domain.model.Product;
import com.frank.franchises.infrastructure.adapter.out.mongodb.document.BranchDocument;
import com.frank.franchises.infrastructure.adapter.out.mongodb.document.FranchiseDocument;
import com.frank.franchises.infrastructure.adapter.out.mongodb.document.ProductDocument;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Component
public class FranchiseDocumentMapper {

    public FranchiseDocument toDocument(Franchise domain) {
        if (domain == null) {
            return null;
        }

        List<BranchDocument> branchDocuments = domain.getBranches() == null
                ? new ArrayList<>()
                : domain.getBranches().stream()
                        .filter(Objects::nonNull)
                        .map(this::toBranchDocument)
                        .toList();

        return FranchiseDocument.builder()
                .id(domain.getId())
                .name(domain.getName())
                .version(domain.getVersion())
                .branches(new ArrayList<>(branchDocuments))
                .build();
    }

    public Franchise toDomain(FranchiseDocument document) {
        if (document == null) {
            return null;
        }

        List<Branch> branches = document.getBranches() == null
                ? new ArrayList<>()
                : document.getBranches().stream()
                        .filter(Objects::nonNull)
                        .map(this::toBranchDomain)
                        .toList();

        return Franchise.builder()
                .id(document.getId())
                .name(document.getName())
                .version(document.getVersion())
                .branches(new ArrayList<>(branches))
                .build();
    }

    public BranchDocument toBranchDocument(Branch domain) {
        if (domain == null) {
            return null;
        }

        List<ProductDocument> productDocuments = domain.getProducts() == null
                ? new ArrayList<>()
                : domain.getProducts().stream()
                        .filter(Objects::nonNull)
                        .map(this::toProductDocument)
                        .toList();

        return BranchDocument.builder()
                .id(domain.getId())
                .name(domain.getName())
                .products(new ArrayList<>(productDocuments))
                .build();
    }

    public Branch toBranchDomain(BranchDocument document) {
        if (document == null) {
            return null;
        }

        List<Product> products = document.getProducts() == null
                ? new ArrayList<>()
                : document.getProducts().stream()
                        .filter(Objects::nonNull)
                        .map(this::toProductDomain)
                        .toList();

        return Branch.builder()
                .id(document.getId())
                .name(document.getName())
                .products(new ArrayList<>(products))
                .build();
    }

    public ProductDocument toProductDocument(Product domain) {
        if (domain == null) {
            return null;
        }

        return ProductDocument.builder()
                .id(domain.getId())
                .name(domain.getName())
                .stock(domain.getStock() != null ? domain.getStock() : 0)
                .build();
    }

    public Product toProductDomain(ProductDocument document) {
        if (document == null) {
            return null;
        }

        return Product.builder()
                .id(document.getId())
                .name(document.getName())
                .stock(document.getStock() != null ? document.getStock() : 0)
                .build();
    }
}
