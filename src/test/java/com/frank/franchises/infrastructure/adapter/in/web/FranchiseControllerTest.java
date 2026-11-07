package com.frank.franchises.infrastructure.adapter.in.web;

import com.frank.franchises.domain.exception.ResourceNotFoundException;
import com.frank.franchises.domain.model.Branch;
import com.frank.franchises.domain.model.BranchTopProduct;
import com.frank.franchises.domain.model.Franchise;
import com.frank.franchises.domain.model.Product;
import com.frank.franchises.domain.port.in.FranchiseUseCase;
import com.frank.franchises.infrastructure.adapter.in.web.dto.*;
import com.frank.franchises.infrastructure.adapter.in.web.error.GlobalExceptionHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@WebFluxTest(controllers = FranchiseController.class)
@Import(GlobalExceptionHandler.class)
class FranchiseControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private FranchiseUseCase franchiseUseCase;

    // --- 1. POST /api/v1/franchises ---
    @Test
    @DisplayName("POST /api/v1/franchises: debe crear franquicia y retornar 201 Created")
    void createFranchise_Success() {
        Franchise franchise = Franchise.builder().id("f1").name("Burger King").branches(List.of()).build();
        when(franchiseUseCase.createFranchise("Burger King")).thenReturn(Mono.just(franchise));

        CreateFranchiseRequest request = new CreateFranchiseRequest("Burger King");

        webTestClient.post()
                .uri("/api/v1/franchises")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.id").isEqualTo("f1")
                .jsonPath("$.name").isEqualTo("Burger King")
                .jsonPath("$.branches").isArray();
    }

    // --- 2. POST /api/v1/franchises/{id}/branches ---
    @Test
    @DisplayName("POST /api/v1/franchises/{id}/branches: debe agregar sucursal y retornar 201 Created")
    void addBranch_Success() {
        Branch branch = Branch.builder().id("b1").name("Sucursal Norte").products(List.of()).build();
        Franchise franchise = Franchise.builder().id("f1").name("Burger King").branches(List.of(branch)).build();

        when(franchiseUseCase.addBranch("f1", "Sucursal Norte")).thenReturn(Mono.just(franchise));

        CreateBranchRequest request = new CreateBranchRequest("Sucursal Norte");

        webTestClient.post()
                .uri("/api/v1/franchises/{id}/branches", "f1")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.id").isEqualTo("f1")
                .jsonPath("$.branches[0].id").isEqualTo("b1")
                .jsonPath("$.branches[0].name").isEqualTo("Sucursal Norte");
    }

    // --- 3. POST /api/v1/franchises/{id}/branches/{bid}/products ---
    @Test
    @DisplayName("POST /api/v1/franchises/{id}/branches/{bid}/products: debe agregar producto y retornar 201 Created")
    void addProduct_Success() {
        Product product = Product.builder().id("p1").name("Whopper").stock(50).build();
        Branch branch = Branch.builder().id("b1").name("Sucursal Norte").products(List.of(product)).build();
        Franchise franchise = Franchise.builder().id("f1").name("Burger King").branches(List.of(branch)).build();

        when(franchiseUseCase.addProduct("f1", "b1", "Whopper", 50)).thenReturn(Mono.just(franchise));

        CreateProductRequest request = new CreateProductRequest("Whopper", 50);

        webTestClient.post()
                .uri("/api/v1/franchises/{id}/branches/{bid}/products", "f1", "b1")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.branches[0].products[0].id").isEqualTo("p1")
                .jsonPath("$.branches[0].products[0].name").isEqualTo("Whopper")
                .jsonPath("$.branches[0].products[0].stock").isEqualTo(50);
    }

    // --- 4. DELETE /api/v1/franchises/{id}/branches/{bid}/products/{pid} ---
    @Test
    @DisplayName("DELETE producto: debe eliminar producto y retornar 204 No Content")
    void removeProduct_Success() {
        when(franchiseUseCase.removeProduct("f1", "b1", "p1")).thenReturn(Mono.empty());

        webTestClient.delete()
                .uri("/api/v1/franchises/{id}/branches/{bid}/products/{pid}", "f1", "b1", "p1")
                .exchange()
                .expectStatus().isNoContent()
                .expectBody().isEmpty();
    }

    // --- 5. PATCH /api/v1/franchises/{id}/branches/{bid}/products/{pid}/stock ---
    @Test
    @DisplayName("PATCH stock: debe actualizar el stock del producto y retornar 200 OK")
    void updateProductStock_Success() {
        Product product = Product.builder().id("p1").name("Whopper").stock(99).build();
        Branch branch = Branch.builder().id("b1").name("Sucursal Norte").products(List.of(product)).build();
        Franchise franchise = Franchise.builder().id("f1").name("Burger King").branches(List.of(branch)).build();

        when(franchiseUseCase.updateProductStock("f1", "b1", "p1", 99)).thenReturn(Mono.just(franchise));

        UpdateStockRequest request = new UpdateStockRequest(99);

        webTestClient.patch()
                .uri("/api/v1/franchises/{id}/branches/{bid}/products/{pid}/stock", "f1", "b1", "p1")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.branches[0].products[0].stock").isEqualTo(99);
    }

    // --- 6. GET /api/v1/franchises/{id}/top-stock-products ---
    @Test
    @DisplayName("GET top-stock-products: debe retornar los productos con mayor stock por sucursal")
    void getTopStockProductsByBranch_Success() {
        Product product = Product.builder().id("p2").name("Whopper Doble").stock(80).build();
        BranchTopProduct topProduct = new BranchTopProduct("b1", "Sucursal Norte", product);

        when(franchiseUseCase.getTopStockProductsByBranch("f1")).thenReturn(Flux.just(topProduct));

        webTestClient.get()
                .uri("/api/v1/franchises/{id}/top-stock-products", "f1")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$[0].branchId").isEqualTo("b1")
                .jsonPath("$[0].branchName").isEqualTo("Sucursal Norte")
                .jsonPath("$[0].product.id").isEqualTo("p2")
                .jsonPath("$[0].product.name").isEqualTo("Whopper Doble")
                .jsonPath("$[0].product.stock").isEqualTo(80);
    }

    // --- 7. PATCH /api/v1/franchises/{id}/name ---
    @Test
    @DisplayName("PATCH franchise name: debe actualizar nombre de franquicia y retornar 200 OK")
    void updateFranchiseName_Success() {
        Franchise franchise = Franchise.builder().id("f1").name("BK Premium").branches(List.of()).build();
        when(franchiseUseCase.updateFranchiseName("f1", "BK Premium")).thenReturn(Mono.just(franchise));

        UpdateNameRequest request = new UpdateNameRequest("BK Premium");

        webTestClient.patch()
                .uri("/api/v1/franchises/{id}/name", "f1")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.name").isEqualTo("BK Premium");
    }

    // --- 8. PATCH /api/v1/franchises/{id}/branches/{bid}/name ---
    @Test
    @DisplayName("PATCH branch name: debe actualizar nombre de sucursal y retornar 200 OK")
    void updateBranchName_Success() {
        Branch branch = Branch.builder().id("b1").name("Sucursal Sur").products(List.of()).build();
        Franchise franchise = Franchise.builder().id("f1").name("Burger King").branches(List.of(branch)).build();

        when(franchiseUseCase.updateBranchName("f1", "b1", "Sucursal Sur")).thenReturn(Mono.just(franchise));

        UpdateNameRequest request = new UpdateNameRequest("Sucursal Sur");

        webTestClient.patch()
                .uri("/api/v1/franchises/{id}/branches/{bid}/name", "f1", "b1")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.branches[0].name").isEqualTo("Sucursal Sur");
    }

    // --- 9. PATCH /api/v1/franchises/{id}/branches/{bid}/products/{pid}/name ---
    @Test
    @DisplayName("PATCH product name: debe actualizar nombre de producto y retornar 200 OK")
    void updateProductName_Success() {
        Product product = Product.builder().id("p1").name("Mega Whopper").stock(50).build();
        Branch branch = Branch.builder().id("b1").name("Sucursal Norte").products(List.of(product)).build();
        Franchise franchise = Franchise.builder().id("f1").name("Burger King").branches(List.of(branch)).build();

        when(franchiseUseCase.updateProductName("f1", "b1", "p1", "Mega Whopper")).thenReturn(Mono.just(franchise));

        UpdateNameRequest request = new UpdateNameRequest("Mega Whopper");

        webTestClient.patch()
                .uri("/api/v1/franchises/{id}/branches/{bid}/products/{pid}/name", "f1", "b1", "p1")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.branches[0].products[0].name").isEqualTo("Mega Whopper");
    }

    // --- Tests de Validación y Errores (400, 404, 500) ---

    @Test
    @DisplayName("Validación 400: nombre de franquicia en blanco debe retornar Bad Request")
    void validationError_BlankFranchiseName() {
        CreateFranchiseRequest request = new CreateFranchiseRequest("   ");

        webTestClient.post()
                .uri("/api/v1/franchises")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.status").isEqualTo(400)
                .jsonPath("$.error").isEqualTo("Bad Request")
                .jsonPath("$.message").value(msg -> org.assertj.core.api.Assertions.assertThat((String) msg).contains("name"))
                .jsonPath("$.path").isEqualTo("/api/v1/franchises");
    }

    @Test
    @DisplayName("Validación 400: stock negativo (-1) en creación de producto debe retornar Bad Request")
    void validationError_NegativeStockOnCreateProduct() {
        CreateProductRequest request = new CreateProductRequest("Papas", -1);

        webTestClient.post()
                .uri("/api/v1/franchises/{id}/branches/{bid}/products", "f1", "b1")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.status").isEqualTo(400)
                .jsonPath("$.message").value(msg -> org.assertj.core.api.Assertions.assertThat((String) msg).contains("stock"))
                .jsonPath("$.path").isEqualTo("/api/v1/franchises/f1/branches/b1/products");
    }

    @Test
    @DisplayName("Validación 400: stock negativo (-1) en actualización de stock debe retornar Bad Request")
    void validationError_NegativeStockOnUpdateStock() {
        UpdateStockRequest request = new UpdateStockRequest(-1);

        webTestClient.patch()
                .uri("/api/v1/franchises/{id}/branches/{bid}/products/{pid}/stock", "f1", "b1", "p1")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.status").isEqualTo(400)
                .jsonPath("$.path").isEqualTo("/api/v1/franchises/f1/branches/b1/products/p1/stock");
    }

    @Test
    @DisplayName("Error 404: ResourceNotFoundException debe ser capturado y retornar 404 Not Found")
    void notFoundError_ResourceNotFoundException() {
        when(franchiseUseCase.addBranch(eq("f99"), anyString()))
                .thenReturn(Mono.error(new ResourceNotFoundException("Franchise not found with id: f99")));

        CreateBranchRequest request = new CreateBranchRequest("Sucursal Este");

        webTestClient.post()
                .uri("/api/v1/franchises/{id}/branches", "f99")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.status").isEqualTo(404)
                .jsonPath("$.error").isEqualTo("Not Found")
                .jsonPath("$.message").isEqualTo("Franchise not found with id: f99")
                .jsonPath("$.path").isEqualTo("/api/v1/franchises/f99/branches");
    }

    @Test
    @DisplayName("Error 500: Excepción inesperada debe retornar 500 Internal Server Error seguro")
    void internalServerError_UnexpectedException() {
        when(franchiseUseCase.createFranchise(anyString()))
                .thenReturn(Mono.error(new RuntimeException("Error inesperado en base de datos")));

        CreateFranchiseRequest request = new CreateFranchiseRequest("Franquicia");

        webTestClient.post()
                .uri("/api/v1/franchises")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().is5xxServerError()
                .expectBody()
                .jsonPath("$.status").isEqualTo(500)
                .jsonPath("$.error").isEqualTo("Internal Server Error")
                .jsonPath("$.message").isEqualTo("Ocurrió un error interno en el servidor")
                .jsonPath("$.path").isEqualTo("/api/v1/franchises");
    }
}
