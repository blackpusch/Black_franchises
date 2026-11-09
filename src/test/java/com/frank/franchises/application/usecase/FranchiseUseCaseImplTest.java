package com.frank.franchises.application.usecase;

import com.frank.franchises.domain.exception.BadRequestException;
import com.frank.franchises.domain.exception.DuplicateResourceException;
import com.frank.franchises.domain.exception.ResourceNotFoundException;
import com.frank.franchises.domain.model.Branch;
import com.frank.franchises.domain.model.Franchise;
import com.frank.franchises.domain.model.Product;
import com.frank.franchises.domain.port.out.FranchiseRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.OptimisticLockingFailureException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

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
        lenient().when(repositoryPort.existsByNameIgnoreCase(anyString())).thenReturn(Mono.just(false));
        lenient().when(repositoryPort.existsByNameIgnoreCaseAndIdNot(anyString(), anyString())).thenReturn(Mono.just(false));
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

        ArgumentCaptor<Franchise> captor = ArgumentCaptor.forClass(Franchise.class);
        verify(repositoryPort).save(captor.capture());
        Franchise captured = captor.getValue();
        assertThat(captured.getId()).isEqualTo("f1");
        assertThat(captured.getName()).isEqualTo("KFC");
        assertThat(captured.getBranches()).hasSize(1);
        assertThat(captured.getBranches().getFirst().getId()).isNotBlank();
        assertThat(captured.getBranches().getFirst().getName()).isEqualTo("Sucursal Centro");
        assertThat(captured.getBranches().getFirst().getProducts()).isEmpty();
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

        ArgumentCaptor<Franchise> captor = ArgumentCaptor.forClass(Franchise.class);
        verify(repositoryPort).save(captor.capture());
        Franchise captured = captor.getValue();
        assertThat(captured.getId()).isEqualTo("f1");
        assertThat(captured.getName()).isEqualTo("Burger King");
        assertThat(captured.getBranches()).hasSize(1);
        Branch capturedBranch = captured.getBranches().getFirst();
        assertThat(capturedBranch.getId()).isEqualTo("b1");
        assertThat(capturedBranch.getName()).isEqualTo("Sucursal 1");
        assertThat(capturedBranch.getProducts()).hasSize(1);
        Product capturedProduct = capturedBranch.getProducts().getFirst();
        assertThat(capturedProduct.getId()).isNotBlank();
        assertThat(capturedProduct.getName()).isEqualTo("Whopper");
        assertThat(capturedProduct.getStock()).isEqualTo(50);
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

        ArgumentCaptor<Franchise> captor = ArgumentCaptor.forClass(Franchise.class);
        verify(repositoryPort).save(captor.capture());
        Franchise captured = captor.getValue();
        assertThat(captured.getId()).isEqualTo("f1");
        assertThat(captured.getName()).isEqualTo("Franquicia");
        assertThat(captured.getBranches()).hasSize(1);
        assertThat(captured.getBranches().getFirst().getId()).isEqualTo("b1");
        assertThat(captured.getBranches().getFirst().getName()).isEqualTo("Sucursal 1");
        assertThat(captured.getBranches().getFirst().getProducts()).isEmpty();
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

        ArgumentCaptor<Franchise> captor = ArgumentCaptor.forClass(Franchise.class);
        verify(repositoryPort).save(captor.capture());
        Franchise captured = captor.getValue();
        assertThat(captured.getId()).isEqualTo("f1");
        assertThat(captured.getName()).isEqualTo("Franquicia");
        assertThat(captured.getBranches()).hasSize(1);
        Product capturedProduct = captured.getBranches().getFirst().getProducts().getFirst();
        assertThat(capturedProduct.getId()).isEqualTo("p1");
        assertThat(capturedProduct.getName()).isEqualTo("Gaseosa");
        assertThat(capturedProduct.getStock()).isEqualTo(99);
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

        ArgumentCaptor<Franchise> captor = ArgumentCaptor.forClass(Franchise.class);
        verify(repositoryPort).save(captor.capture());
        Franchise captured = captor.getValue();
        assertThat(captured.getId()).isEqualTo("f1");
        assertThat(captured.getName()).isEqualTo("Nuevo Nombre");
        assertThat(captured.getBranches()).isEmpty();
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

        ArgumentCaptor<Franchise> captor = ArgumentCaptor.forClass(Franchise.class);
        verify(repositoryPort).save(captor.capture());
        Franchise captured = captor.getValue();
        assertThat(captured.getId()).isEqualTo("f1");
        assertThat(captured.getBranches()).hasSize(1);
        assertThat(captured.getBranches().getFirst().getId()).isEqualTo("b1");
        assertThat(captured.getBranches().getFirst().getName()).isEqualTo("Sucursal Nueva");
        assertThat(captured.getBranches().getFirst().getProducts()).isEmpty();
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

        ArgumentCaptor<Franchise> captor = ArgumentCaptor.forClass(Franchise.class);
        verify(repositoryPort).save(captor.capture());
        Franchise captured = captor.getValue();
        assertThat(captured.getId()).isEqualTo("f1");
        assertThat(captured.getBranches()).hasSize(1);
        Product capturedProduct = captured.getBranches().getFirst().getProducts().getFirst();
        assertThat(capturedProduct.getId()).isEqualTo("p1");
        assertThat(capturedProduct.getName()).isEqualTo("Cafe Expreso");
        assertThat(capturedProduct.getStock()).isEqualTo(10);
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

    // --- 10. Concurrencia y Bloqueo Optimista (@Version + Retry) ---

    @Test
    @DisplayName("Concurrencia: dos actualizaciones simultáneas sobre productos distintos de la misma franquicia reintentan con Mono.defer y conservan ambos cambios")
    void concurrentProductStockUpdates_ShouldRetryAndPreserveBothUpdates() {
        Franchise initialStored = Franchise.builder()
                .id("f1")
                .name("Franquicia Concurrente")
                .version(0L)
                .branches(List.of(
                        Branch.builder()
                                .id("b1")
                                .name("Sucursal Central")
                                .products(List.of(
                                        Product.builder().id("p1").name("Hamburguesa").stock(10).build(),
                                        Product.builder().id("p2").name("Papas").stock(20).build()
                                ))
                                .build()
                ))
                .build();

        AtomicReference<Franchise> databaseState = new AtomicReference<>(initialStored);
        AtomicInteger saveAttempts = new AtomicInteger(0);

        // Cada findById devuelve una copia fresca del documento actual en BD (simulando lectura real en MongoDB)
        when(repositoryPort.findById("f1")).thenAnswer(inv -> Mono.fromCallable(() -> copyFranchise(databaseState.get())));

        // Las dos primeras lecturas ven version=0L; el primer save pasa a version=1L y el segundo save con version=0L lanza OptimisticLockingFailureException
        when(repositoryPort.save(any(Franchise.class))).thenAnswer(inv -> Mono.defer(() -> {
            Franchise incoming = inv.getArgument(0);
            saveAttempts.incrementAndGet();

            while (true) {
                Franchise current = databaseState.get();
                if (!java.util.Objects.equals(incoming.getVersion(), current.getVersion())) {
                    return Mono.error(new OptimisticLockingFailureException(
                            "Version mismatch: expected " + current.getVersion() + " but got " + incoming.getVersion()));
                }
                Franchise nextState = copyFranchise(incoming).toBuilder()
                        .version(current.getVersion() + 1L)
                        .build();
                if (databaseState.compareAndSet(current, nextState)) {
                    return Mono.just(copyFranchise(nextState));
                }
            }
        }));

        // Simulamos que ambas peticiones leen inicialmente la versión 0 antes de que la primera termine de guardar
        Franchise snapshotRead1 = copyFranchise(databaseState.get());
        Franchise snapshotRead2 = copyFranchise(databaseState.get());
        AtomicInteger readCount = new AtomicInteger(0);
        when(repositoryPort.findById("f1")).thenAnswer(inv -> Mono.fromCallable(() -> {
            int r = readCount.incrementAndGet();
            if (r == 1) {
                return snapshotRead1;
            }
            if (r == 2) {
                return snapshotRead2;
            }
            return copyFranchise(databaseState.get());
        }));

        Mono<Franchise> updateP1 = useCase.updateProductStock("f1", "b1", "p1", 50);
        Mono<Franchise> updateP2 = useCase.updateProductStock("f1", "b1", "p2", 80);

        StepVerifier.create(Mono.zip(updateP1, updateP2))
                .assertNext(tuple -> {
                    Franchise finalState = databaseState.get();
                    assertThat(finalState.getVersion()).isEqualTo(2L);

                    List<Product> finalProducts = finalState.getBranches().getFirst().getProducts();
                    assertThat(finalProducts)
                            .extracting(Product::getId, Product::getStock)
                            .containsExactly(
                                    org.assertj.core.groups.Tuple.tuple("p1", 50),
                                    org.assertj.core.groups.Tuple.tuple("p2", 80)
                            );
                })
                .verifyComplete();

        // 2 lecturas iniciales en versión 0 + 1 relectura en el retry = 3 lecturas y 3 intentos de save
        assertThat(readCount.get()).isEqualTo(3);
        assertThat(saveAttempts.get()).isEqualTo(3);
    }

    @Test
    @DisplayName("Concurrencia: cuando se agotan los 3 reintentos propaga OptimisticLockingFailureException sin envolver")
    void optimisticLocking_WhenRetriesExhausted_ShouldPropagateOriginalException() {
        AtomicInteger findCalls = new AtomicInteger(0);
        when(repositoryPort.findById("f1")).thenAnswer(inv -> {
            findCalls.incrementAndGet();
            Product p1 = Product.builder().id("p1").name("Cafe").stock(10).build();
            Branch b1 = Branch.builder().id("b1").name("Sucursal").products(new ArrayList<>(List.of(p1))).build();
            return Mono.just(Franchise.builder().id("f1").name("Franquicia").version(1L).branches(new ArrayList<>(List.of(b1))).build());
        });
        when(repositoryPort.save(any(Franchise.class)))
                .thenReturn(Mono.error(new OptimisticLockingFailureException("Conflicto persistente")));

        StepVerifier.create(useCase.updateProductStock("f1", "b1", "p1", 99))
                .expectError(OptimisticLockingFailureException.class)
                .verify();

        // 1 intento original + 3 reintentos = 4 ejecuciones completas del ciclo findById -> modify -> save
        assertThat(findCalls.get()).isEqualTo(4);
        verify(repositoryPort, times(4)).save(any(Franchise.class));
    }

    // --- 11. Unicidad de nombres, Renombrado a sí mismo y No-Reintento de Excepciones de Negocio ---

    @Test
    @DisplayName("addBranch: debe lanzar DuplicateResourceException (sin reintentar) si ya existe sucursal con el mismo nombre ignorando mayúsculas y espacios")
    void addBranch_DuplicateNameIgnoreCaseAndSpaces_ShouldFailWithoutRetry() {
        Branch existing = Branch.builder().id("b1").name("Sucursal Centro").products(new ArrayList<>()).build();
        Franchise franchise = Franchise.builder()
                .id("f1")
                .name("KFC")
                .branches(new ArrayList<>(List.of(existing)))
                .build();

        when(repositoryPort.findById("f1")).thenReturn(Mono.just(franchise));

        StepVerifier.create(useCase.addBranch("f1", "  sucursal CENTRO  "))
                .expectErrorMatches(err -> err instanceof DuplicateResourceException
                        && err.getMessage().contains("sucursal CENTRO"))
                .verify();

        verify(repositoryPort, times(1)).findById("f1");
        verify(repositoryPort, never()).save(any());
    }

    @Test
    @DisplayName("updateBranchName: debe lanzar DuplicateResourceException si otra sucursal ya tiene ese nombre")
    void updateBranchName_DuplicateWithAnotherBranch_ShouldFailWithoutRetry() {
        Branch b1 = Branch.builder().id("b1").name("Sucursal Norte").build();
        Branch b2 = Branch.builder().id("b2").name("Sucursal Sur").build();
        Franchise franchise = Franchise.builder()
                .id("f1")
                .branches(new ArrayList<>(List.of(b1, b2)))
                .build();

        when(repositoryPort.findById("f1")).thenReturn(Mono.just(franchise));

        StepVerifier.create(useCase.updateBranchName("f1", "b2", "  SUCURSAL NORTE "))
                .expectError(DuplicateResourceException.class)
                .verify();

        verify(repositoryPort, times(1)).findById("f1");
        verify(repositoryPort, never()).save(any());
    }

    @Test
    @DisplayName("updateBranchName: debe permitir renombrar una sucursal a su propio nombre (excluyendo el propio elemento)")
    void updateBranchName_RenameToSelf_ShouldSucceed() {
        Branch b1 = Branch.builder().id("b1").name("Sucursal Norte").build();
        Franchise franchise = Franchise.builder()
                .id("f1")
                .branches(new ArrayList<>(List.of(b1)))
                .build();

        when(repositoryPort.findById("f1")).thenReturn(Mono.just(franchise));
        when(repositoryPort.save(any(Franchise.class))).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        StepVerifier.create(useCase.updateBranchName("f1", "b1", "  Sucursal Norte  "))
                .assertNext(saved -> assertThat(saved.getBranches().getFirst().getName()).isEqualTo("Sucursal Norte"))
                .verifyComplete();

        verify(repositoryPort, times(1)).findById("f1");
        verify(repositoryPort, times(1)).save(franchise);
    }

    @Test
    @DisplayName("addProduct: debe lanzar DuplicateResourceException (sin reintentar) si ya existe producto con el mismo nombre en la sucursal")
    void addProduct_DuplicateNameInSameBranch_ShouldFailWithoutRetry() {
        Product existing = Product.builder().id("p1").name("Big Mac").stock(20).build();
        Branch branch = Branch.builder()
                .id("b1")
                .name("Sucursal 1")
                .products(new ArrayList<>(List.of(existing)))
                .build();
        Franchise franchise = Franchise.builder()
                .id("f1")
                .branches(new ArrayList<>(List.of(branch)))
                .build();

        when(repositoryPort.findById("f1")).thenReturn(Mono.just(franchise));

        StepVerifier.create(useCase.addProduct("f1", "b1", "  big mac ", 15))
                .expectError(DuplicateResourceException.class)
                .verify();

        verify(repositoryPort, times(1)).findById("f1");
        verify(repositoryPort, never()).save(any());
    }

    @Test
    @DisplayName("updateProductName: debe lanzar DuplicateResourceException si otro producto de la misma sucursal ya tiene ese nombre")
    void updateProductName_DuplicateWithAnotherProduct_ShouldFailWithoutRetry() {
        Product p1 = Product.builder().id("p1").name("Whopper").stock(10).build();
        Product p2 = Product.builder().id("p2").name("Papas").stock(25).build();
        Branch branch = Branch.builder()
                .id("b1")
                .name("Sucursal 1")
                .products(new ArrayList<>(List.of(p1, p2)))
                .build();
        Franchise franchise = Franchise.builder()
                .id("f1")
                .branches(new ArrayList<>(List.of(branch)))
                .build();

        when(repositoryPort.findById("f1")).thenReturn(Mono.just(franchise));

        StepVerifier.create(useCase.updateProductName("f1", "b1", "p2", " WHOPPER "))
                .expectError(DuplicateResourceException.class)
                .verify();

        verify(repositoryPort, times(1)).findById("f1");
        verify(repositoryPort, never()).save(any());
    }

    @Test
    @DisplayName("updateProductName: debe permitir renombrar un producto a su propio nombre (excluyendo el propio elemento)")
    void updateProductName_RenameToSelf_ShouldSucceed() {
        Product p1 = Product.builder().id("p1").name("Whopper").stock(10).build();
        Branch branch = Branch.builder()
                .id("b1")
                .name("Sucursal 1")
                .products(new ArrayList<>(List.of(p1)))
                .build();
        Franchise franchise = Franchise.builder()
                .id("f1")
                .branches(new ArrayList<>(List.of(branch)))
                .build();

        when(repositoryPort.findById("f1")).thenReturn(Mono.just(franchise));
        when(repositoryPort.save(any(Franchise.class))).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        StepVerifier.create(useCase.updateProductName("f1", "b1", "p1", "  Whopper  "))
                .assertNext(saved -> assertThat(saved.getBranches().getFirst().getProducts().getFirst().getName()).isEqualTo("Whopper"))
                .verifyComplete();

        verify(repositoryPort, times(1)).findById("f1");
        verify(repositoryPort, times(1)).save(franchise);
    }

    @Test
    @DisplayName("No-reintento de excepciones de negocio: ResourceNotFoundException y BadRequestException nunca se reintentan")
    void businessExceptions_ShouldNeverBeRetried() {
        when(repositoryPort.findById("f-missing")).thenReturn(Mono.empty());

        StepVerifier.create(useCase.addBranch("f-missing", "Sucursal 1"))
                .expectError(ResourceNotFoundException.class)
                .verify();
        verify(repositoryPort, times(1)).findById("f-missing");

        StepVerifier.create(useCase.addBranch("f1", "   "))
                .expectError(BadRequestException.class)
                .verify();

        StepVerifier.create(useCase.updateProductStock("f1", "b1", "p1", -5))
                .expectError(BadRequestException.class)
                .verify();

        verify(repositoryPort, never()).save(any());
    }

    @Test
    @DisplayName("createFranchise: debe lanzar DuplicateResourceException si ya existe una franquicia con el mismo nombre")
    void createFranchise_DuplicateName_ShouldThrowDuplicateResourceException() {
        when(repositoryPort.existsByNameIgnoreCase("McDonalds")).thenReturn(Mono.just(true));

        StepVerifier.create(useCase.createFranchise("  McDonalds  "))
                .expectErrorMatches(err -> err instanceof DuplicateResourceException
                        && err.getMessage().contains("McDonalds"))
                .verify();

        verify(repositoryPort, times(1)).existsByNameIgnoreCase("McDonalds");
        verify(repositoryPort, never()).save(any());
    }

    @Test
    @DisplayName("updateFranchiseName: debe lanzar DuplicateResourceException sin reintentar si otra franquicia ya tiene ese nombre")
    void updateFranchiseName_DuplicateWithAnotherFranchise_ShouldFailWithoutRetry() {
        Franchise franchise = Franchise.builder().id("f1").name("Burger King").build();
        when(repositoryPort.findById("f1")).thenReturn(Mono.just(franchise));
        when(repositoryPort.existsByNameIgnoreCaseAndIdNot("McDonalds", "f1")).thenReturn(Mono.just(true));

        StepVerifier.create(useCase.updateFranchiseName("f1", "  McDonalds  "))
                .expectError(DuplicateResourceException.class)
                .verify();

        verify(repositoryPort, times(1)).findById("f1");
        verify(repositoryPort, times(1)).existsByNameIgnoreCaseAndIdNot("McDonalds", "f1");
        verify(repositoryPort, never()).save(any());
    }

    @Test
    @DisplayName("updateFranchiseName: debe permitir renombrar una franquicia a su propio nombre (excluyendo su propio id)")
    void updateFranchiseName_RenameToSelf_ShouldSucceed() {
        Franchise franchise = Franchise.builder().id("f1").name("McDonalds").build();
        when(repositoryPort.findById("f1")).thenReturn(Mono.just(franchise));
        when(repositoryPort.existsByNameIgnoreCaseAndIdNot("mcdonalds", "f1")).thenReturn(Mono.just(false));
        when(repositoryPort.save(any(Franchise.class))).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        StepVerifier.create(useCase.updateFranchiseName("f1", "  mcdonalds  "))
                .assertNext(saved -> assertThat(saved.getName()).isEqualTo("mcdonalds"))
                .verifyComplete();

        verify(repositoryPort, times(1)).existsByNameIgnoreCaseAndIdNot("mcdonalds", "f1");
        ArgumentCaptor<Franchise> captor = ArgumentCaptor.forClass(Franchise.class);
        verify(repositoryPort, times(1)).save(captor.capture());
        Franchise captured = captor.getValue();
        assertThat(captured.getId()).isEqualTo("f1");
        assertThat(captured.getName()).isEqualTo("mcdonalds");
        assertThat(captured.getBranches()).isEmpty();
    }

    @Test
    @DisplayName("requireNonNegativeStock: debe permitir stock igual a 0 en creación y actualización de producto")
    void requireNonNegativeStock_ZeroStock_ShouldSucceed() {
        Branch branch = Branch.builder().id("b1").name("Sucursal 1").products(new ArrayList<>()).build();
        Franchise franchise = Franchise.builder().id("f1").name("KFC").branches(new ArrayList<>(List.of(branch))).build();

        when(repositoryPort.findById("f1")).thenReturn(Mono.just(franchise));
        when(repositoryPort.save(any(Franchise.class))).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        StepVerifier.create(useCase.addProduct("f1", "b1", "Agua", 0))
                .assertNext(saved -> {
                    Product added = saved.getBranches().getFirst().getProducts().getFirst();
                    assertThat(added.getStock()).isZero();
                })
                .verifyComplete();
    }

    // --- 12. Inmutabilidad (toBuilder), preservación de version en las 7 mutaciones y bordes de top-stock ---

    @Test
    @DisplayName("toBuilder: las 7 mutaciones preservan version y construyen listas nuevas sin mutar las originales")
    void allSevenMutations_ShouldPreserveVersionAndNotMutateOriginalLists() {
        Product originalProduct = Product.builder().id("p1").name("Cafe").stock(10).build();
        List<Product> originalProducts = new ArrayList<>(List.of(originalProduct));
        Branch originalBranch = Branch.builder().id("b1").name("Sucursal 1").products(originalProducts).build();
        List<Branch> originalBranches = new ArrayList<>(List.of(originalBranch));
        Franchise originalFranchise = Franchise.builder()
                .id("f1")
                .name("Franquicia Original")
                .version(7L)
                .branches(originalBranches)
                .build();

        when(repositoryPort.findById("f1")).thenReturn(Mono.just(originalFranchise));
        AtomicReference<Franchise> lastSaved = new AtomicReference<>();
        when(repositoryPort.save(any(Franchise.class))).thenAnswer(inv -> {
            Franchise arg = inv.getArgument(0);
            lastSaved.set(arg);
            return Mono.just(arg);
        });

        // 1. addBranch
        StepVerifier.create(useCase.addBranch("f1", "Sucursal 2"))
                .assertNext(saved -> {
                    assertThat(saved.getVersion()).isEqualTo(7L);
                    assertThat(saved.getBranches()).hasSize(2).isNotSameAs(originalBranches);
                    assertThat(originalBranches).hasSize(1);
                })
                .verifyComplete();

        // 2. addProduct
        StepVerifier.create(useCase.addProduct("f1", "b1", "Te", 15))
                .assertNext(saved -> {
                    assertThat(saved.getVersion()).isEqualTo(7L);
                    assertThat(saved.getBranches().getFirst().getProducts()).hasSize(2).isNotSameAs(originalProducts);
                    assertThat(originalProducts).hasSize(1);
                })
                .verifyComplete();

        // 3. removeProduct
        StepVerifier.create(useCase.removeProduct("f1", "b1", "p1"))
                .verifyComplete();
        assertThat(lastSaved.get().getVersion()).isEqualTo(7L);
        assertThat(lastSaved.get().getBranches().getFirst().getProducts()).isEmpty();
        assertThat(originalProducts).hasSize(1);

        // 4. updateProductStock
        StepVerifier.create(useCase.updateProductStock("f1", "b1", "p1", 99))
                .assertNext(saved -> {
                    assertThat(saved.getVersion()).isEqualTo(7L);
                    assertThat(saved.getBranches().getFirst().getProducts().getFirst().getStock()).isEqualTo(99);
                    assertThat(originalProduct.getStock()).isEqualTo(10);
                })
                .verifyComplete();

        // 5. updateFranchiseName
        StepVerifier.create(useCase.updateFranchiseName("f1", "Franquicia Renombrada"))
                .assertNext(saved -> {
                    assertThat(saved.getVersion()).isEqualTo(7L);
                    assertThat(saved.getName()).isEqualTo("Franquicia Renombrada");
                    assertThat(originalFranchise.getName()).isEqualTo("Franquicia Original");
                })
                .verifyComplete();

        // 6. updateBranchName
        StepVerifier.create(useCase.updateBranchName("f1", "b1", "Sucursal Renombrada"))
                .assertNext(saved -> {
                    assertThat(saved.getVersion()).isEqualTo(7L);
                    assertThat(saved.getBranches().getFirst().getName()).isEqualTo("Sucursal Renombrada");
                    assertThat(originalBranch.getName()).isEqualTo("Sucursal 1");
                })
                .verifyComplete();

        // 7. updateProductName
        StepVerifier.create(useCase.updateProductName("f1", "b1", "p1", "Cafe Premium"))
                .assertNext(saved -> {
                    assertThat(saved.getVersion()).isEqualTo(7L);
                    assertThat(saved.getBranches().getFirst().getProducts().getFirst().getName()).isEqualTo("Cafe Premium");
                    assertThat(originalProduct.getName()).isEqualTo("Cafe");
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("getTopStockProductsByBranch: maneja de forma segura listas nulas en franquicia o sucursal")
    void getTopStockProductsByBranch_NullLists_ShouldNotThrowNpe() {
        Branch branchWithNullProducts = Branch.builder().id("b1").name("Sucursal Null").products(null).build();
        Product validProduct = Product.builder().id("p1").name("Combo").stock(25).build();
        Branch validBranch = Branch.builder().id("b2").name("Sucursal Valida").products(List.of(validProduct)).build();

        Franchise franchise = Franchise.builder()
                .id("f1")
                .name("Franquicia")
                .branches(List.of(branchWithNullProducts, validBranch))
                .build();

        when(repositoryPort.findById("f1")).thenReturn(Mono.just(franchise));

        StepVerifier.create(useCase.getTopStockProductsByBranch("f1"))
                .assertNext(top -> {
                    assertThat(top.branchId()).isEqualTo("b2");
                    assertThat(top.product().getId()).isEqualTo("p1");
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("getTopStockProductsByBranch: ignora stock null cuando existe otro producto con stock válido")
    void getTopStockProductsByBranch_MixedNullAndValidStock_ShouldReturnValidStockProduct() {
        Product nullStockProduct = Product.builder().id("p1").name("Sin Stock Definido").stock(null).build();
        Product validStockProduct = Product.builder().id("p2").name("Con Stock").stock(5).build();
        Branch branch = Branch.builder().id("b1").name("Sucursal").products(List.of(nullStockProduct, validStockProduct)).build();

        Franchise franchise = Franchise.builder().id("f1").branches(List.of(branch)).build();
        when(repositoryPort.findById("f1")).thenReturn(Mono.just(franchise));

        StepVerifier.create(useCase.getTopStockProductsByBranch("f1"))
                .assertNext(top -> {
                    assertThat(top.product().getId()).isEqualTo("p2");
                    assertThat(top.product().getStock()).isEqualTo(5);
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("getTopStockProductsByBranch: cuando todos los productos tienen stock null conserva el primero")
    void getTopStockProductsByBranch_AllNullStocks_ShouldReturnFirstProduct() {
        Product firstNull = Product.builder().id("p1").name("Primero Null").stock(null).build();
        Product secondNull = Product.builder().id("p2").name("Segundo Null").stock(null).build();
        Branch branch = Branch.builder().id("b1").name("Sucursal").products(List.of(firstNull, secondNull)).build();

        Franchise franchise = Franchise.builder().id("f1").branches(List.of(branch)).build();
        when(repositoryPort.findById("f1")).thenReturn(Mono.just(franchise));

        StepVerifier.create(useCase.getTopStockProductsByBranch("f1"))
                .assertNext(top -> {
                    assertThat(top.product().getId()).isEqualTo("p1");
                    assertThat(top.product().getName()).isEqualTo("Primero Null");
                })
                .verifyComplete();
    }

    // --- 13. Consultas GET (getAllFranchises y getFranchiseById) ---

    @Test
    @DisplayName("getAllFranchises: debe retornar Flux vacío cuando no existen franquicias")
    void getAllFranchises_Empty_ShouldReturnEmptyFlux() {
        when(repositoryPort.findAll()).thenReturn(Flux.empty());

        StepVerifier.create(useCase.getAllFranchises())
                .verifyComplete();

        verify(repositoryPort, times(1)).findAll();
        verify(repositoryPort, never()).save(any());
    }

    @Test
    @DisplayName("getAllFranchises: debe retornar todas las franquicias con su árbol de sucursales y productos")
    void getAllFranchises_WithData_ShouldReturnFranchisesWithFullTree() {
        Product p1 = Product.builder().id("p1").name("Whopper").stock(25).build();
        Branch b1 = Branch.builder().id("b1").name("Sucursal Norte").products(List.of(p1)).build();
        Franchise f1 = Franchise.builder().id("f1").name("Burger King").branches(List.of(b1)).build();
        Franchise f2 = Franchise.builder().id("f2").name("KFC").branches(List.of()).build();

        when(repositoryPort.findAll()).thenReturn(Flux.just(f1, f2));

        StepVerifier.create(useCase.getAllFranchises())
                .assertNext(first -> {
                    assertThat(first.getId()).isEqualTo("f1");
                    assertThat(first.getName()).isEqualTo("Burger King");
                    assertThat(first.getBranches()).hasSize(1);
                    assertThat(first.getBranches().getFirst().getProducts()).hasSize(1);
                    assertThat(first.getBranches().getFirst().getProducts().getFirst().getName()).isEqualTo("Whopper");
                })
                .assertNext(second -> {
                    assertThat(second.getId()).isEqualTo("f2");
                    assertThat(second.getName()).isEqualTo("KFC");
                    assertThat(second.getBranches()).isEmpty();
                })
                .verifyComplete();

        verify(repositoryPort, times(1)).findAll();
        verify(repositoryPort, never()).save(any());
    }

    @Test
    @DisplayName("getFranchiseById: debe retornar la franquicia existente con sus sucursales y productos")
    void getFranchiseById_Existing_ShouldReturnFranchise() {
        Product p1 = Product.builder().id("p1").name("Big Mac").stock(40).build();
        Branch b1 = Branch.builder().id("b1").name("Sucursal Centro").products(List.of(p1)).build();
        Franchise franchise = Franchise.builder().id("f1").name("McDonalds").branches(List.of(b1)).build();

        when(repositoryPort.findById("f1")).thenReturn(Mono.just(franchise));

        StepVerifier.create(useCase.getFranchiseById("f1"))
                .assertNext(found -> {
                    assertThat(found.getId()).isEqualTo("f1");
                    assertThat(found.getName()).isEqualTo("McDonalds");
                    assertThat(found.getBranches()).hasSize(1);
                    assertThat(found.getBranches().getFirst().getProducts()).hasSize(1);
                    assertThat(found.getBranches().getFirst().getProducts().getFirst().getName()).isEqualTo("Big Mac");
                })
                .verifyComplete();

        verify(repositoryPort, times(1)).findById("f1");
        verify(repositoryPort, never()).save(any());
    }

    @Test
    @DisplayName("getFranchiseById: debe lanzar ResourceNotFoundException si la franquicia no existe")
    void getFranchiseById_NotFound_ShouldThrowResourceNotFoundException() {
        when(repositoryPort.findById("f99")).thenReturn(Mono.empty());

        StepVerifier.create(useCase.getFranchiseById("f99"))
                .expectErrorMatches(err -> err instanceof ResourceNotFoundException
                        && err.getMessage().contains("f99"))
                .verify();

        verify(repositoryPort, times(1)).findById("f99");
        verify(repositoryPort, never()).save(any());
    }

    // --- 14. Hallazgo #12: Límite máximo de longitud en requireNonBlankName (NAME_MAX_LENGTH = 100, medido sin trim) ---

    @Test
    @DisplayName("requireNonBlankName: acepta exactamente 100 caracteres y rechaza 101 caracteres (incluyendo espacios sin trim) con BadRequestException")
    void requireNonBlankName_MaxLength100And101_ShouldEnforceLimitBeforeTrim() {
        String name100 = "A".repeat(FranchiseUseCaseImpl.NAME_MAX_LENGTH);
        when(repositoryPort.existsByNameIgnoreCase(name100)).thenReturn(Mono.just(false));
        when(repositoryPort.save(any(Franchise.class))).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        StepVerifier.create(useCase.createFranchise(name100))
                .assertNext(saved -> assertThat(saved.getName()).hasSize(100).isEqualTo(name100))
                .verifyComplete();

        reset(repositoryPort);

        String name101 = "A".repeat(FranchiseUseCaseImpl.NAME_MAX_LENGTH + 1);
        String name101WithSpacesUntrimmed = " " + "B".repeat(FranchiseUseCaseImpl.NAME_MAX_LENGTH - 1) + " ";
        assertThat(name101WithSpacesUntrimmed).hasSize(101);

        List<Mono<?>> overLimitCalls = List.of(
                useCase.createFranchise(name101),
                useCase.createFranchise(name101WithSpacesUntrimmed),
                useCase.addBranch("f1", name101),
                useCase.addProduct("f1", "b1", name101, 10),
                useCase.updateFranchiseName("f1", name101),
                useCase.updateBranchName("f1", "b1", name101),
                useCase.updateProductName("f1", "b1", "p1", name101)
        );

        for (Mono<?> call : overLimitCalls) {
            StepVerifier.create(call)
                    .expectErrorSatisfies(ex -> {
                        assertThat(ex).isInstanceOf(BadRequestException.class);
                        assertThat(ex.getMessage()).isEqualTo("Name must not exceed 100 characters");
                        assertThat(ex.getMessage()).doesNotContain(name101);
                    })
                    .verify();
        }

        verifyNoInteractions(repositoryPort);
    }

    private Franchise copyFranchise(Franchise source) {
        List<Branch> branchesCopy = new ArrayList<>();
        for (Branch b : source.getBranches()) {
            List<Product> productsCopy = new ArrayList<>();
            for (Product p : b.getProducts()) {
                productsCopy.add(Product.builder()
                        .id(p.getId())
                        .name(p.getName())
                        .stock(p.getStock())
                        .build());
            }
            branchesCopy.add(Branch.builder()
                    .id(b.getId())
                    .name(b.getName())
                    .products(productsCopy)
                    .build());
        }
        return Franchise.builder()
                .id(source.getId())
                .name(source.getName())
                .version(source.getVersion())
                .branches(branchesCopy)
                .build();
    }
}
