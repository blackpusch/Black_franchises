package com.frank.franchises.domain.port.in;

import com.frank.franchises.domain.model.BranchTopProduct;
import com.frank.franchises.domain.model.Franchise;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface FranchiseUseCase {

    int NAME_MAX_LENGTH = 100;

    Mono<Franchise> createFranchise(String name);

    Flux<Franchise> getAllFranchises();

    Mono<Franchise> getFranchiseById(String franchiseId);

    Mono<Franchise> addBranch(String franchiseId, String branchName);

    Mono<Franchise> addProduct(String franchiseId, String branchId, String productName, int stock);

    Mono<Void> removeProduct(String franchiseId, String branchId, String productId);

    Mono<Franchise> updateProductStock(String franchiseId, String branchId, String productId, int newStock);

    Flux<BranchTopProduct> getTopStockProductsByBranch(String franchiseId);

    Mono<Franchise> updateFranchiseName(String franchiseId, String newName);

    Mono<Franchise> updateBranchName(String franchiseId, String branchId, String newName);

    Mono<Franchise> updateProductName(String franchiseId, String branchId, String productId, String newName);
}
