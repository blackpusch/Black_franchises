package com.frank.franchises.application.usecase;

import com.frank.franchises.domain.exception.ResourceNotFoundException;
import com.frank.franchises.domain.model.Branch;
import com.frank.franchises.domain.model.BranchTopProduct;
import com.frank.franchises.domain.model.Franchise;
import com.frank.franchises.domain.model.Product;
import com.frank.franchises.domain.port.out.FranchiseRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FranchiseUseCaseImplTest {

    @Mock
    private FranchiseRepositoryPort repositoryPort;

    private FranchiseUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        useCase = new FranchiseUseCaseImpl(repositoryPort);
    }

    // --- 1. createFranchise ---

    @Test
    @DisplayName("createFranchise: debe crear franquicia con ID generado y ramas vacías")
    void createFranchise_Success() {
        when(repositoryPort.save(any(Franchise.class)))
                .thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        Mono<Franchise> result = useCase.createFranchise("McDonalds");

        StepVerifier.create(result)
                .assertNext(franchise -> {
                    assertThat(franchise.getId()).isNotBlank();
                    assertThat(franchise.getName()).isEqualTo("McDonalds");
                    assertThat(franchise.getBranches()).isEmpty();
                })
                .verifyComplete();

        verify(repositoryPort).save(any(Franchise.class));
    }

    // --- 2. addBranch ---

    @Test
    @DisplayName("addBranch: debe agregar sucursal a la franquicia existente y persistir")
    void addBranch_Success() {
        Franchise franchise = Franchise.builder()
                .id("f1")
                .name("KFC")
                .branches(new ArrayList<>())
                .build();

        when(repositoryPort.findById("f1")).thenReturn(Mono.just(franchise));
        when(repositoryPort.save(any(Franchise.class)))
                .thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        Mono<Franchise> result = useCase.addBranch("f1", "Sucursal Centro");

        StepVerifier.create(result)
                .assertNext(saved -> {
                    assertThat(saved.getBranches()).hasSize(1);
                    Branch branch = saved.getBranches().getFirst();
                    assertThat(branch.getId()).isNotBlank();
                    assertThat(branch.getName()).isEqualTo("Sucursal Centro");
                    assertThat(branch.getProducts()).isEmpty();
                })
                .verifyComplete();

        verify(repositoryPort).save(franchise);
    }

    @Test
    @DisplayName("addBranch: debe lanzar ResourceNotFoundException si la franquicia no existe")
    void addBranch_FranchiseNotFound() {
        when(repositoryPort.findById("f99")).thenReturn(Mono.empty());

        Mono<Franchise> result = useCase.addBranch("f99", "Sucursal Centro");

        StepVerifier.create(result)
                .expectErrorMatches(err -> err instanceof ResourceNotFoundException
                        && err.getMessage().contains("f99"))
                .verify();

        verify(repositoryPort, never()).save(any());
    }

    // --- 3. addProduct ---

    @Test
    @DisplayName("addProduct: debe agregar producto a la sucursal indicada y persistir")
    void addProduct_Success() {
        Branch branch = Branch.builder()
                .id("b1")
                .name("Sucursal 1")
                .products(new ArrayList<>())
                .build();

        Franchise franchise = Franchise.builder()
                .id("f1")
                .name("Burger King")
                .branches(new ArrayList<>(List.of(branch)))
                .build();

        when(repositoryPort.findById("f1")).thenReturn(Mono.just(franchise));
        when(repositoryPort.save(any(Franchise.class)))
                .thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        Mono<Franchise> result = useCase.addProduct("f1", "b1", "Whopper", 50);

        StepVerifier.create(result)
                .assertNext(saved -> {
                    Branch updatedBranch = saved.getBranches().getFirst();
                    assertThat(updatedBranch.getProducts()).hasSize(1);
                    Product product = updatedBranch.getProducts().getFirst();
                    assertThat(product.getId()).isNotBlank();
                    assertThat(product.getName()).isEqualTo("Whopper");
                    assertThat(product.getStock()).isEqualTo(50);
                })
                .verifyComplete();

        verify(repositoryPort).save(franchise);
    }

    @Test
    @DisplayName("addProduct: debe lanzar ResourceNotFoundException si la franquicia no existe")
    void addProduct_FranchiseNotFound() {
        when(repositoryPort.findById("f99")).thenReturn(Mono.empty());

        Mono<Franchise> result = useCase.addProduct("f99", "b1", "Whopper", 50);

        StepVerifier.create(result)
                .expectErrorMatches(err -> err instanceof ResourceNotFoundException
                        && err.getMessage().contains("f99"))
                .verify();

        verify(repositoryPort, never()).save(any());
    }

    @Test
    @DisplayName("addProduct: debe lanzar ResourceNotFoundException si la sucursal no existe")
    void addProduct_BranchNotFound() {
        Franchise franchise = Franchise.builder()
                .id("f1")
                .name("Burger King")
                .branches(new ArrayList<>())
                .build();

        when(repositoryPort.findById("f1")).thenReturn(Mono.just(franchise));

        Mono<Franchise> result = useCase.addProduct("f1", "b99", "Whopper", 50);

        StepVerifier.create(result)
                .expectErrorMatches(err -> err instanceof ResourceNotFoundException
                        && err.getMessage().contains("b99"))
                .verify();

        verify(repositoryPort, never()).save(any());
    }

    // --- 4. removeProduct ---

    @Test
    @DisplayName("removeProduct: debe remover el producto de la sucursal, guardar franquicia y retornar Mono<Void>")
    void removeProduct_Success() {
        Product product = Product.builder().id("p1").name("Papas").stock(30).build();
        Branch branch = Branch.builder()
                .id("b1")
                .name("Sucursal 1")
                .products(new ArrayList<>(List.of(product)))
                .build();
        Franchise franchise = Franchise.builder()
                .id("f1")
                .name("Franquicia")
                .branches(new ArrayList<>(List.of(branch)))
                .build();

        when(repositoryPort.findById("f1")).thenReturn(Mono.just(franchise));
        when(repositoryPort.save(any(Franchise.class)))
                .thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        Mono<Void> result = useCase.removeProduct("f1", "b1", "p1");

        StepVerifier.create(result)
                .verifyComplete();

        assertThat(branch.getProducts()).isEmpty();
        verify(repositoryPort).save(franchise);
    }

    @Test
    @DisplayName("removeProduct: debe lanzar ResourceNotFoundException si la franquicia no existe y NO guardar")
    void removeProduct_FranchiseNotFound() {
        when(repositoryPort.findById("f99")).thenReturn(Mono.empty());

        Mono<Void> result = useCase.removeProduct("f99", "b1", "p1");

        StepVerifier.create(result)
                .expectErrorMatches(err -> err instanceof ResourceNotFoundException
                        && err.getMessage().contains("f99"))
                .verify();

        verify(repositoryPort, never()).save(any());
    }

    @Test
    @DisplayName("removeProduct: debe lanzar ResourceNotFoundException si la sucursal no existe y NO guardar")
    void removeProduct_BranchNotFound() {
        Franchise franchise = Franchise.builder()
                .id("f1")
                .name("Franquicia")
                .branches(new ArrayList<>())
                .build();

        when(repositoryPort.findById("f1")).thenReturn(Mono.just(franchise));

        Mono<Void> result = useCase.removeProduct("f1", "b99", "p1");

        StepVerifier.create(result)
                .expectErrorMatches(err -> err instanceof ResourceNotFoundException
                        && err.getMessage().contains("b99"))
                .verify();

        verify(repositoryPort, never()).save(any());
    }

    @Test
    @DisplayName("removeProduct: debe lanzar ResourceNotFoundException si el producto no existe y NO guardar")
    void removeProduct_ProductNotFound() {
        Branch branch = Branch.builder()
                .id("b1")
                .name("Sucursal 1")
                .products(new ArrayList<>())
                .build();
        Franchise franchise = Franchise.builder()
                .id("f1")
                .name("Franquicia")
                .branches(new ArrayList<>(List.of(branch)))
                .build();

        when(repositoryPort.findById("f1")).thenReturn(Mono.just(franchise));

        Mono<Void> result = useCase.removeProduct("f1", "b1", "p99");

        StepVerifier.create(result)
                .expectErrorMatches(err -> err instanceof ResourceNotFoundException
                        && err.getMessage().contains("p99"))
                .verify();

        verify(repositoryPort, never()).save(any());
    }

    // --- 5. updateProductStock ---

    @Test
    @DisplayName("updateProductStock: debe actualizar el stock del producto y persistir")
    void updateProductStock_Success() {
        Product product = Product.builder().id("p1").name("Gaseosa").stock(10).build();
        Branch branch = Branch.builder().id("b1").name("Sucursal 1").products(new ArrayList<>(List.of(product))).build();
        Franchise franchise = Franchise.builder().id("f1").name("Franquicia").branches(new ArrayList<>(List.of(branch))).build();

        when(repositoryPort.findById("f1")).thenReturn(Mono.just(franchise));
        when(repositoryPort.save(any(Franchise.class)))
                .thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        Mono<Franchise> result = useCase.updateProductStock("f1", "b1", "p1", 99);

        StepVerifier.create(result)
                .assertNext(saved -> {
                    Product updated = saved.getBranches().getFirst().getProducts().getFirst();
                    assertThat(updated.getStock()).isEqualTo(99);
                })
                .verifyComplete();

        verify(repositoryPort).save(franchise);
    }

    @Test
    @DisplayName("updateProductStock: 404 si franquicia no existe")
    void updateProductStock_FranchiseNotFound() {
        when(repositoryPort.findById("f99")).thenReturn(Mono.empty());

        StepVerifier.create(useCase.updateProductStock("f99", "b1", "p1", 50))
                .expectError(ResourceNotFoundException.class)
                .verify();

        verify(repositoryPort, never()).save(any());
    }

    @Test
    @DisplayName("updateProductStock: 404 si sucursal no existe")
    void updateProductStock_BranchNotFound() {
        Franchise franchise = Franchise.builder().id("f1").branches(new ArrayList<>()).build();
        when(repositoryPort.findById("f1")).thenReturn(Mono.just(franchise));

        StepVerifier.create(useCase.updateProductStock("f1", "b99", "p1", 50))
                .expectError(ResourceNotFoundException.class)
                .verify();

        verify(repositoryPort, never()).save(any());
    }

    @Test
    @DisplayName("updateProductStock: 404 si producto no existe")
    void updateProductStock_ProductNotFound() {
        Branch branch = Branch.builder().id("b1").products(new ArrayList<>()).build();
        Franchise franchise = Franchise.builder().id("f1").branches(new ArrayList<>(List.of(branch))).build();
        when(repositoryPort.findById("f1")).thenReturn(Mono.just(franchise));

        StepVerifier.create(useCase.updateProductStock("f1", "b1", "p99", 50))
                .expectError(ResourceNotFoundException.class)
                .verify();

        verify(repositoryPort, never()).save(any());
    }

    // --- 6. getTopStockProductsByBranch ---

    @Test
    @DisplayName("getTopStockProductsByBranch: debe retornar el producto con mayor stock por sucursal en orden original")
    void getTopStockProductsByBranch_Success() {
        Product p1 = Product.builder().id("p1").name("Prod 1").stock(10).build();
        Product p2 = Product.builder().id("p2").name("Prod 2").stock(50).build();
        Branch b1 = Branch.builder().id("b1").name("Sucursal A").products(List.of(p1, p2)).build();

        Product p3 = Product.builder().id("p3").name("Prod 3").stock(100).build();
        Product p4 = Product.builder().id("p4").name("Prod 4").stock(20).build();
        Branch b2 = Branch.builder().id("b2").name("Sucursal B").products(List.of(p3, p4)).build();

        Franchise franchise = Franchise.builder()
                .id("f1")
                .name("Franquicia Top")
                .branches(List.of(b1, b2))
                .build();

        when(repositoryPort.findById("f1")).thenReturn(Mono.just(franchise));

        StepVerifier.create(useCase.getTopStockProductsByBranch("f1"))
                .assertNext(top1 -> {
                    assertThat(top1.branchId()).isEqualTo("b1");
                    assertThat(top1.branchName()).isEqualTo("Sucursal A");
                    assertThat(top1.product().getId()).isEqualTo("p2");
                    assertThat(top1.product().getStock()).isEqualTo(50);
                })
                .assertNext(top2 -> {
                    assertThat(top2.branchId()).isEqualTo("b2");
                    assertThat(top2.branchName()).isEqualTo("Sucursal B");
                    assertThat(top2.product().getId()).isEqualTo("p3");
                    assertThat(top2.product().getStock()).isEqualTo(100);
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("getTopStockProductsByBranch: omite sucursales sin productos")
    void getTopStockProductsByBranch_OmitsBranchesWithoutProducts() {
        Branch emptyBranch = Branch.builder().id("b1").name("Sucursal Vacia").products(new ArrayList<>()).build();
        Product p = Product.builder().id("p1").name("Item").stock(15).build();
        Branch branchWithProducts = Branch.builder().id("b2").name("Sucursal Con Prod").products(List.of(p)).build();

        Franchise franchise = Franchise.builder()
                .id("f1")
                .branches(List.of(emptyBranch, branchWithProducts))
                .build();

        when(repositoryPort.findById("f1")).thenReturn(Mono.just(franchise));

        StepVerifier.create(useCase.getTopStockProductsByBranch("f1"))
                .assertNext(top -> {
                    assertThat(top.branchId()).isEqualTo("b2");
                    assertThat(top.product().getName()).isEqualTo("Item");
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("getTopStockProductsByBranch: en caso de empate de stock, mantiene el primer producto encontrado")
    void getTopStockProductsByBranch_TieBreakerReturnsFirstProduct() {
        Product first = Product.builder().id("p1").name("Primero").stock(40).build();
        Product second = Product.builder().id("p2").name("Segundo").stock(40).build();
        Branch branch = Branch.builder().id("b1").name("Sucursal Empate").products(List.of(first, second)).build();

        Franchise franchise = Franchise.builder().id("f1").branches(List.of(branch)).build();
        when(repositoryPort.findById("f1")).thenReturn(Mono.just(franchise));

        StepVerifier.create(useCase.getTopStockProductsByBranch("f1"))
                .assertNext(top -> {
                    assertThat(top.product().getId()).isEqualTo("p1");
                    assertThat(top.product().getName()).isEqualTo("Primero");
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("getTopStockProductsByBranch: lanza ResourceNotFoundException si la franquicia no existe")
    void getTopStockProductsByBranch_FranchiseNotFound() {
        when(repositoryPort.findById("f99")).thenReturn(Mono.empty());

        StepVerifier.create(useCase.getTopStockProductsByBranch("f99"))
                .expectError(ResourceNotFoundException.class)
                .verify();

        verify(repositoryPort, never()).save(any());
    }

    // --- 7. updateFranchiseName ---

    @Test
    @DisplayName("updateFranchiseName: debe actualizar nombre de franquicia y persistir")
    void updateFranchiseName_Success() {
        Franchise franchise = Franchise.builder().id("f1").name("Viejo Nombre").build();
        when(repositoryPort.findById("f1")).thenReturn(Mono.just(franchise));
        when(repositoryPort.save(any(Franchise.class)))
                .thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        Mono<Franchise> result = useCase.updateFranchiseName("f1", "Nuevo Nombre");

        StepVerifier.create(result)
                .assertNext(saved -> assertThat(saved.getName()).isEqualTo("Nuevo Nombre"))
                .verifyComplete();

        verify(repositoryPort).save(franchise);
    }

    @Test
    @DisplayName("updateFranchiseName: 404 si franquicia no existe")
    void updateFranchiseName_FranchiseNotFound() {
        when(repositoryPort.findById("f99")).thenReturn(Mono.empty());

        StepVerifier.create(useCase.updateFranchiseName("f99", "Nuevo Nombre"))
                .expectError(ResourceNotFoundException.class)
                .verify();

        verify(repositoryPort, never()).save(any());
    }

    // --- 8. updateBranchName ---

    @Test
    @DisplayName("updateBranchName: debe actualizar nombre de sucursal y persistir")
    void updateBranchName_Success() {
        Branch branch = Branch.builder().id("b1").name("Sucursal Vieja").build();
        Franchise franchise = Franchise.builder().id("f1").branches(new ArrayList<>(List.of(branch))).build();

        when(repositoryPort.findById("f1")).thenReturn(Mono.just(franchise));
        when(repositoryPort.save(any(Franchise.class)))
                .thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        Mono<Franchise> result = useCase.updateBranchName("f1", "b1", "Sucursal Nueva");

        StepVerifier.create(result)
                .assertNext(saved -> assertThat(saved.getBranches().getFirst().getName()).isEqualTo("Sucursal Nueva"))
                .verifyComplete();

        verify(repositoryPort).save(franchise);
    }

    @Test
    @DisplayName("updateBranchName: 404 si franquicia no existe")
    void updateBranchName_FranchiseNotFound() {
        when(repositoryPort.findById("f99")).thenReturn(Mono.empty());

        StepVerifier.create(useCase.updateBranchName("f99", "b1", "Sucursal Nueva"))
                .expectError(ResourceNotFoundException.class)
                .verify();

        verify(repositoryPort, never()).save(any());
    }

    @Test
    @DisplayName("updateBranchName: 404 si sucursal no existe")
    void updateBranchName_BranchNotFound() {
        Franchise franchise = Franchise.builder().id("f1").branches(new ArrayList<>()).build();
        when(repositoryPort.findById("f1")).thenReturn(Mono.just(franchise));

        StepVerifier.create(useCase.updateBranchName("f1", "b99", "Sucursal Nueva"))
                .expectError(ResourceNotFoundException.class)
                .verify();

        verify(repositoryPort, never()).save(any());
    }

    // --- 9. updateProductName ---

    @Test
    @DisplayName("updateProductName: debe actualizar nombre de producto y persistir")
    void updateProductName_Success() {
        Product product = Product.builder().id("p1").name("Cafe Normal").stock(10).build();
        Branch branch = Branch.builder().id("b1").name("Sucursal 1").products(new ArrayList<>(List.of(product))).build();
        Franchise franchise = Franchise.builder().id("f1").branches(new ArrayList<>(List.of(branch))).build();

        when(repositoryPort.findById("f1")).thenReturn(Mono.just(franchise));
        when(repositoryPort.save(any(Franchise.class)))
                .thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        Mono<Franchise> result = useCase.updateProductName("f1", "b1", "p1", "Cafe Expreso");

        StepVerifier.create(result)
                .assertNext(saved -> {
                    Product updated = saved.getBranches().getFirst().getProducts().getFirst();
                    assertThat(updated.getName()).isEqualTo("Cafe Expreso");
                })
                .verifyComplete();

        verify(repositoryPort).save(franchise);
    }

    @Test
    @DisplayName("updateProductName: 404 si franquicia no existe")
    void updateProductName_FranchiseNotFound() {
        when(repositoryPort.findById("f99")).thenReturn(Mono.empty());

        StepVerifier.create(useCase.updateProductName("f99", "b1", "p1", "Nuevo"))
                .expectError(ResourceNotFoundException.class)
                .verify();

        verify(repositoryPort, never()).save(any());
    }

    @Test
    @DisplayName("updateProductName: 404 si sucursal no existe")
    void updateProductName_BranchNotFound() {
        Franchise franchise = Franchise.builder().id("f1").branches(new ArrayList<>()).build();
        when(repositoryPort.findById("f1")).thenReturn(Mono.just(franchise));

        StepVerifier.create(useCase.updateProductName("f1", "b99", "p1", "Nuevo"))
                .expectError(ResourceNotFoundException.class)
                .verify();

        verify(repositoryPort, never()).save(any());
    }

    @Test
    @DisplayName("updateProductName: 404 si producto no existe")
    void updateProductName_ProductNotFound() {
        Branch branch = Branch.builder().id("b1").products(new ArrayList<>()).build();
        Franchise franchise = Franchise.builder().id("f1").branches(new ArrayList<>(List.of(branch))).build();
        when(repositoryPort.findById("f1")).thenReturn(Mono.just(franchise));

        StepVerifier.create(useCase.updateProductName("f1", "b1", "p99", "Nuevo"))
                .expectError(ResourceNotFoundException.class)
                .verify();

        verify(repositoryPort, never()).save(any());
    }
}
