package com.frank.franchises.application.usecase;

import com.frank.franchises.domain.exception.ResourceNotFoundException;
import com.frank.franchises.domain.model.Branch;
import com.frank.franchises.domain.model.BranchTopProduct;
import com.frank.franchises.domain.model.Franchise;
import com.frank.franchises.domain.model.Product;
import com.frank.franchises.domain.port.in.FranchiseUseCase;
import com.frank.franchises.domain.port.out.FranchiseRepositoryPort;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Implementación de los casos de uso del dominio de franquicias.
 * Clase pura desacoplada de anotaciones de Spring (Clean Architecture).
 */
public class FranchiseUseCaseImpl implements FranchiseUseCase {

    private final FranchiseRepositoryPort repositoryPort;

    public FranchiseUseCaseImpl(FranchiseRepositoryPort repositoryPort) {
        this.repositoryPort = Objects.requireNonNull(repositoryPort, "repositoryPort must not be null");
    }

    @Override
    public Mono<Franchise> createFranchise(String name) {
        Franchise franchise = Franchise.builder()
                .name(name)
                .branches(new ArrayList<>())
                .build();
        return repositoryPort.save(franchise);
    }

    @Override
    public Mono<Franchise> addBranch(String franchiseId, String branchName) {
        return findFranchise(franchiseId)
                .map(franchise -> {
                    Branch branch = Branch.builder()
                            .name(branchName)
                            .products(new ArrayList<>())
                            .build();
                    franchise.getBranches().add(branch);
                    return franchise;
                })
                .flatMap(repositoryPort::save);
    }

    @Override
    public Mono<Franchise> addProduct(String franchiseId, String branchId, String productName, int stock) {
        return findFranchise(franchiseId)
                .flatMap(franchise -> findBranch(franchise, branchId)
                        .map(branch -> {
                            Product product = Product.builder()
                                    .name(productName)
                                    .stock(stock)
                                    .build();
                            branch.getProducts().add(product);
                            return franchise;
                        }))
                .flatMap(repositoryPort::save);
    }

    @Override
    public Mono<Void> removeProduct(String franchiseId, String branchId, String productId) {
        return findFranchise(franchiseId)
                .flatMap(franchise -> findBranch(franchise, branchId)
                        .flatMap(branch -> findProduct(branch, productId)
                                .map(product -> {
                                    branch.getProducts().remove(product);
                                    return franchise;
                                })))
                .flatMap(repositoryPort::save)
                .then();
    }

    @Override
    public Mono<Franchise> updateProductStock(String franchiseId, String branchId, String productId, int newStock) {
        return findFranchise(franchiseId)
                .flatMap(franchise -> findBranch(franchise, branchId)
                        .flatMap(branch -> findProduct(branch, productId)
                                .map(product -> {
                                    product.setStock(newStock);
                                    return franchise;
                                })))
                .flatMap(repositoryPort::save);
    }

    @Override
    public Flux<BranchTopProduct> getTopStockProductsByBranch(String franchiseId) {
        return findFranchise(franchiseId)
                .flatMapMany(franchise -> Flux.fromIterable(franchise.getBranches()))
                .filter(branch -> branch.getProducts() != null && !branch.getProducts().isEmpty())
                .map(branch -> new BranchTopProduct(
                        branch.getId(),
                        branch.getName(),
                        findTopProductWithFirstTieBreaker(branch.getProducts())
                ));
    }

    @Override
    public Mono<Franchise> updateFranchiseName(String franchiseId, String newName) {
        return findFranchise(franchiseId)
                .map(franchise -> {
                    franchise.setName(newName);
                    return franchise;
                })
                .flatMap(repositoryPort::save);
    }

    @Override
    public Mono<Franchise> updateBranchName(String franchiseId, String branchId, String newName) {
        return findFranchise(franchiseId)
                .flatMap(franchise -> findBranch(franchise, branchId)
                        .map(branch -> {
                            branch.setName(newName);
                            return franchise;
                        }))
                .flatMap(repositoryPort::save);
    }

    @Override
    public Mono<Franchise> updateProductName(String franchiseId, String branchId, String productId, String newName) {
        return findFranchise(franchiseId)
                .flatMap(franchise -> findBranch(franchise, branchId)
                        .flatMap(branch -> findProduct(branch, productId)
                                .map(product -> {
                                    product.setName(newName);
                                    return franchise;
                                })))
                .flatMap(repositoryPort::save);
    }

    // --- Helpers privados reutilizables ---

    private Mono<Franchise> findFranchise(String franchiseId) {
        return repositoryPort.findById(franchiseId)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Franchise not found with id: " + franchiseId)));
    }

    private Mono<Branch> findBranch(Franchise franchise, String branchId) {
        return Mono.justOrEmpty(
                franchise.getBranches().stream()
                        .filter(branch -> Objects.equals(branch.getId(), branchId))
                        .findFirst()
        ).switchIfEmpty(Mono.error(new ResourceNotFoundException("Branch not found with id: " + branchId)));
    }

    private Mono<Product> findProduct(Branch branch, String productId) {
        return Mono.justOrEmpty(
                branch.getProducts().stream()
                        .filter(product -> Objects.equals(product.getId(), productId))
                        .findFirst()
        ).switchIfEmpty(Mono.error(new ResourceNotFoundException("Product not found with id: " + productId)));
    }

    /**
     * Encuentra el producto con mayor stock. En caso de empate, mantiene el primer producto encontrado.
     */
    private Product findTopProductWithFirstTieBreaker(List<Product> products) {
        Product top = null;
        for (Product product : products) {
            if (top == null || (product.getStock() != null && (top.getStock() == null || product.getStock() > top.getStock()))) {
                top = product;
            }
        }
        return top;
    }
}
