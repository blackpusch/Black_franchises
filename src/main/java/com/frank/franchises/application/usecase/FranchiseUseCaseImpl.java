package com.frank.franchises.application.usecase;

import com.frank.franchises.domain.exception.BadRequestException;
import com.frank.franchises.domain.exception.DuplicateResourceException;
import com.frank.franchises.domain.exception.ResourceNotFoundException;
import com.frank.franchises.domain.model.Branch;
import com.frank.franchises.domain.model.BranchTopProduct;
import com.frank.franchises.domain.model.Franchise;
import com.frank.franchises.domain.model.Product;
import com.frank.franchises.domain.port.in.FranchiseUseCase;
import com.frank.franchises.domain.port.out.FranchiseRepositoryPort;
import org.springframework.dao.OptimisticLockingFailureException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Implementación de los casos de uso del dominio de franquicias.
 * Clase pura desacoplada de anotaciones de Spring (Clean Architecture).
 */
public class FranchiseUseCaseImpl implements FranchiseUseCase {

    private static final int MAX_OPTIMISTIC_LOCK_RETRIES = 3;

    private final FranchiseRepositoryPort repositoryPort;

    public FranchiseUseCaseImpl(FranchiseRepositoryPort repositoryPort) {
        this.repositoryPort = Objects.requireNonNull(repositoryPort, "repositoryPort must not be null");
    }

    @Override
    public Mono<Franchise> createFranchise(String name) {
        return Mono.defer(() -> {
            String normalized = requireNonBlankName(name, "Franchise name must not be blank");
            return repositoryPort.existsByNameIgnoreCase(normalized)
                    .flatMap(exists -> {
                        if (Boolean.TRUE.equals(exists)) {
                            return Mono.error(new DuplicateResourceException("Franchise already exists with name: " + normalized));
                        }
                        Franchise franchise = Franchise.builder()
                                .name(normalized)
                                .branches(new ArrayList<>())
                                .build();
                        return repositoryPort.save(franchise);
                    });
        });
    }

    @Override
    public Flux<Franchise> getAllFranchises() {
        return repositoryPort.findAll();
    }

    @Override
    public Mono<Franchise> getFranchiseById(String franchiseId) {
        return findFranchise(franchiseId);
    }

    @Override
    public Mono<Franchise> addBranch(String franchiseId, String branchName) {
        return withOptimisticLockRetry(() -> {
            String normalized = requireNonBlankName(branchName, "Branch name must not be blank");
            return findFranchise(franchiseId)
                    .flatMap(franchise -> {
                        ensureBranchNameAvailable(franchise, normalized, null);
                        List<Branch> updatedBranches = new ArrayList<>(branchesOf(franchise));
                        updatedBranches.add(Branch.builder()
                                .name(normalized)
                                .products(new ArrayList<>())
                                .build());
                        Franchise updatedFranchise = franchise.toBuilder()
                                .branches(updatedBranches)
                                .build();
                        return repositoryPort.save(updatedFranchise);
                    });
        });
    }

    @Override
    public Mono<Franchise> addProduct(String franchiseId, String branchId, String productName, int stock) {
        return withOptimisticLockRetry(() -> {
            String normalized = requireNonBlankName(productName, "Product name must not be blank");
            requireNonNegativeStock(stock);
            return findFranchise(franchiseId)
                    .flatMap(franchise -> findBranch(franchise, branchId)
                            .flatMap(branch -> {
                                ensureProductNameAvailable(branch, normalized, null);
                                List<Product> updatedProducts = new ArrayList<>(productsOf(branch));
                                updatedProducts.add(Product.builder()
                                        .name(normalized)
                                        .stock(stock)
                                        .build());
                                Branch updatedBranch = branch.toBuilder()
                                        .products(updatedProducts)
                                        .build();
                                Franchise updatedFranchise = replaceBranch(franchise, updatedBranch);
                                return repositoryPort.save(updatedFranchise);
                            }));
        });
    }

    @Override
    public Mono<Void> removeProduct(String franchiseId, String branchId, String productId) {
        return withOptimisticLockRetry(() -> findFranchise(franchiseId)
                .flatMap(franchise -> findBranch(franchise, branchId)
                        .flatMap(branch -> findProduct(branch, productId)
                                .map(product -> {
                                    List<Product> updatedProducts = productsOf(branch).stream()
                                            .filter(p -> !Objects.equals(p.getId(), product.getId()))
                                            .collect(Collectors.toCollection(ArrayList::new));
                                    Branch updatedBranch = branch.toBuilder()
                                            .products(updatedProducts)
                                            .build();
                                    return replaceBranch(franchise, updatedBranch);
                                })))
                .flatMap(repositoryPort::save)
                .then());
    }

    @Override
    public Mono<Franchise> updateProductStock(String franchiseId, String branchId, String productId, int newStock) {
        return withOptimisticLockRetry(() -> {
            requireNonNegativeStock(newStock);
            return findFranchise(franchiseId)
                    .flatMap(franchise -> findBranch(franchise, branchId)
                            .flatMap(branch -> findProduct(branch, productId)
                                    .map(product -> {
                                        Product updatedProduct = product.toBuilder()
                                                .stock(newStock)
                                                .build();
                                        Branch updatedBranch = replaceProduct(branch, updatedProduct);
                                        return replaceBranch(franchise, updatedBranch);
                                    })))
                    .flatMap(repositoryPort::save);
        });
    }

    @Override
    public Flux<BranchTopProduct> getTopStockProductsByBranch(String franchiseId) {
        return findFranchise(franchiseId)
                .flatMapMany(franchise -> Flux.fromIterable(branchesOf(franchise)))
                .filter(branch -> !productsOf(branch).isEmpty())
                .flatMap(branch -> Mono.justOrEmpty(findTopProductWithFirstTieBreaker(productsOf(branch)))
                        .map(topProduct -> new BranchTopProduct(
                                branch.getId(),
                                branch.getName(),
                                topProduct
                        )));
    }

    @Override
    public Mono<Franchise> updateFranchiseName(String franchiseId, String newName) {
        return withOptimisticLockRetry(() -> {
            String normalized = requireNonBlankName(newName, "Franchise name must not be blank");
            return findFranchise(franchiseId)
                    .flatMap(franchise -> repositoryPort.existsByNameIgnoreCaseAndIdNot(normalized, franchiseId)
                            .flatMap(exists -> {
                                if (Boolean.TRUE.equals(exists)) {
                                    return Mono.error(new DuplicateResourceException("Franchise already exists with name: " + normalized));
                                }
                                Franchise updatedFranchise = franchise.toBuilder()
                                        .name(normalized)
                                        .branches(new ArrayList<>(branchesOf(franchise)))
                                        .build();
                                return repositoryPort.save(updatedFranchise);
                            }));
        });
    }

    @Override
    public Mono<Franchise> updateBranchName(String franchiseId, String branchId, String newName) {
        return withOptimisticLockRetry(() -> {
            String normalized = requireNonBlankName(newName, "Branch name must not be blank");
            return findFranchise(franchiseId)
                    .flatMap(franchise -> findBranch(franchise, branchId)
                            .flatMap(branch -> {
                                ensureBranchNameAvailable(franchise, normalized, branchId);
                                Branch updatedBranch = branch.toBuilder()
                                        .name(normalized)
                                        .products(new ArrayList<>(productsOf(branch)))
                                        .build();
                                Franchise updatedFranchise = replaceBranch(franchise, updatedBranch);
                                return repositoryPort.save(updatedFranchise);
                            }));
        });
    }

    @Override
    public Mono<Franchise> updateProductName(String franchiseId, String branchId, String productId, String newName) {
        return withOptimisticLockRetry(() -> {
            String normalized = requireNonBlankName(newName, "Product name must not be blank");
            return findFranchise(franchiseId)
                    .flatMap(franchise -> findBranch(franchise, branchId)
                            .flatMap(branch -> findProduct(branch, productId)
                                    .flatMap(product -> {
                                        ensureProductNameAvailable(branch, normalized, productId);
                                        Product updatedProduct = product.toBuilder()
                                                .name(normalized)
                                                .build();
                                        Branch updatedBranch = replaceProduct(branch, updatedProduct);
                                        Franchise updatedFranchise = replaceBranch(franchise, updatedBranch);
                                        return repositoryPort.save(updatedFranchise);
                                    })));
        });
    }

    // --- Helpers privados reutilizables ---

    private <T> Mono<T> withOptimisticLockRetry(Supplier<Mono<T>> operation) {
        return Mono.defer(operation)
                .retryWhen(Retry.max(MAX_OPTIMISTIC_LOCK_RETRIES)
                        .filter(OptimisticLockingFailureException.class::isInstance)
                        .onRetryExhaustedThrow((retrySpec, retrySignal) -> retrySignal.failure()));
    }

    private List<Branch> branchesOf(Franchise franchise) {
        return franchise != null && franchise.getBranches() != null
                ? franchise.getBranches()
                : Collections.emptyList();
    }

    private List<Product> productsOf(Branch branch) {
        return branch != null && branch.getProducts() != null
                ? branch.getProducts()
                : Collections.emptyList();
    }

    private Franchise replaceBranch(Franchise franchise, Branch updatedBranch) {
        List<Branch> newBranches = branchesOf(franchise).stream()
                .map(b -> Objects.equals(b.getId(), updatedBranch.getId()) ? updatedBranch : b)
                .collect(Collectors.toCollection(ArrayList::new));
        return franchise.toBuilder()
                .branches(newBranches)
                .build();
    }

    private Branch replaceProduct(Branch branch, Product updatedProduct) {
        List<Product> newProducts = productsOf(branch).stream()
                .map(p -> Objects.equals(p.getId(), updatedProduct.getId()) ? updatedProduct : p)
                .collect(Collectors.toCollection(ArrayList::new));
        return branch.toBuilder()
                .products(newProducts)
                .build();
    }

    private String normalizeForComparison(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private String requireNonBlankName(String name, String message) {
        if (name == null || name.trim().isEmpty()) {
            throw new BadRequestException(message);
        }
        return name.trim();
    }

    private void requireNonNegativeStock(int stock) {
        if (stock < 0) {
            throw new BadRequestException("Stock must be greater than or equal to 0");
        }
    }

    private void ensureBranchNameAvailable(Franchise franchise, String candidateName, String excludedBranchId) {
        String normalizedCandidate = normalizeForComparison(candidateName);
        boolean duplicateExists = branchesOf(franchise).stream()
                .filter(Objects::nonNull)
                .filter(branch -> !Objects.equals(branch.getId(), excludedBranchId))
                .anyMatch(branch -> normalizeForComparison(branch.getName()).equals(normalizedCandidate));

        if (duplicateExists) {
            throw new DuplicateResourceException("Branch already exists with name: " + candidateName.trim());
        }
    }

    private void ensureProductNameAvailable(Branch branch, String candidateName, String excludedProductId) {
        String normalizedCandidate = normalizeForComparison(candidateName);
        boolean duplicateExists = productsOf(branch).stream()
                .filter(Objects::nonNull)
                .filter(product -> !Objects.equals(product.getId(), excludedProductId))
                .anyMatch(product -> normalizeForComparison(product.getName()).equals(normalizedCandidate));

        if (duplicateExists) {
            throw new DuplicateResourceException("Product already exists in branch with name: " + candidateName.trim());
        }
    }

    private Mono<Franchise> findFranchise(String franchiseId) {
        return repositoryPort.findById(franchiseId)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Franchise not found with id: " + franchiseId)));
    }

    private Mono<Branch> findBranch(Franchise franchise, String branchId) {
        return Mono.justOrEmpty(
                branchesOf(franchise).stream()
                        .filter(Objects::nonNull)
                        .filter(branch -> Objects.equals(branch.getId(), branchId))
                        .findFirst()
        ).switchIfEmpty(Mono.error(new ResourceNotFoundException("Branch not found with id: " + branchId)));
    }

    private Mono<Product> findProduct(Branch branch, String productId) {
        return Mono.justOrEmpty(
                productsOf(branch).stream()
                        .filter(Objects::nonNull)
                        .filter(product -> Objects.equals(product.getId(), productId))
                        .findFirst()
        ).switchIfEmpty(Mono.error(new ResourceNotFoundException("Product not found with id: " + productId)));
    }

    /**
     * Encuentra de forma funcional el producto con mayor stock dentro de la lista.
     *
     * Regla explícita de desempate (first-seen tie-breaker):
     * - Se evalúa con desigualdad estricta (secondStock > firstStock).
     * - En caso de empate exacto (secondStock == firstStock), se preserva siempre el primer producto encontrado ('first').
     * - Cualquier producto con stock no nulo tiene precedencia sobre uno con stock null.
     * - Si todos los productos tienen stock null, se conserva el primer producto no nulo de la lista.
     */
    private Product findTopProductWithFirstTieBreaker(List<Product> products) {
        if (products == null || products.isEmpty()) {
            return null;
        }
        return products.stream()
                .filter(Objects::nonNull)
                .reduce((first, second) -> {
                    Integer firstStock = first.getStock();
                    Integer secondStock = second.getStock();
                    if (secondStock == null) {
                        return first;
                    }
                    if (firstStock == null || secondStock > firstStock) {
                        return second;
                    }
                    return first;
                })
                .orElse(null);
    }
}
