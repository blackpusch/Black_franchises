package com.frank.franchises.infrastructure.adapter.in.web;

import com.frank.franchises.domain.exception.BadRequestException;
import com.frank.franchises.domain.exception.DuplicateResourceException;
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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
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

        verifyNoInteractions(franchiseUseCase);
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

        verifyNoInteractions(franchiseUseCase);
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

        verifyNoInteractions(franchiseUseCase);
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

    @Test
    @DisplayName("Error 409: OptimisticLockingFailureException debe retornar 409 Conflict y no quedar tapado por el handler 500")
    void conflictError_OptimisticLockingFailureException() {
        when(franchiseUseCase.updateProductStock(eq("f1"), eq("b1"), eq("p1"), anyInt()))
                .thenReturn(Mono.error(new org.springframework.dao.OptimisticLockingFailureException("Conflicto de versión")));

        UpdateStockRequest request = new UpdateStockRequest(50);

        webTestClient.patch()
                .uri("/api/v1/franchises/{id}/branches/{bid}/products/{pid}/stock", "f1", "b1", "p1")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isEqualTo(409)
                .expectBody()
                .jsonPath("$.status").isEqualTo(409)
                .jsonPath("$.error").isEqualTo("Conflict")
                .jsonPath("$.path").isEqualTo("/api/v1/franchises/f1/branches/b1/products/p1/stock");
    }

    @Test
    @DisplayName("Error 409: DuplicateResourceException debe retornar 409 Conflict con el detalle del duplicado")
    void conflictError_DuplicateResourceException() {
        when(franchiseUseCase.addBranch(eq("f1"), anyString()))
                .thenReturn(Mono.error(new DuplicateResourceException("Branch already exists with name: Sucursal Norte")));

        CreateBranchRequest request = new CreateBranchRequest("Sucursal Norte");

        webTestClient.post()
                .uri("/api/v1/franchises/{id}/branches", "f1")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isEqualTo(409)
                .expectBody()
                .jsonPath("$.status").isEqualTo(409)
                .jsonPath("$.error").isEqualTo("Conflict")
                .jsonPath("$.message").isEqualTo("Branch already exists with name: Sucursal Norte")
                .jsonPath("$.path").isEqualTo("/api/v1/franchises/f1/branches");
    }

    @Test
    @DisplayName("Error 400: BadRequestException de dominio debe retornar 400 Bad Request")
    void badRequestError_DomainBadRequestException() {
        when(franchiseUseCase.createFranchise(anyString()))
                .thenReturn(Mono.error(new BadRequestException("Franchise name must not be blank")));

        CreateFranchiseRequest request = new CreateFranchiseRequest("ValidoEnDtoPeroRechazadoEnDominio");

        webTestClient.post()
                .uri("/api/v1/franchises")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.status").isEqualTo(400)
                .jsonPath("$.error").isEqualTo("Bad Request")
                .jsonPath("$.message").isEqualTo("Franchise name must not be blank")
                .jsonPath("$.path").isEqualTo("/api/v1/franchises");
    }

    // --- Tests de Consulta GET (/api/v1/franchises y /api/v1/franchises/{id}) ---

    @Test
    @DisplayName("GET /api/v1/franchises: debe retornar 200 OK y lista vacía [] cuando no hay franquicias")
    void getAllFranchises_Empty_Returns200AndEmptyArray() {
        when(franchiseUseCase.getAllFranchises()).thenReturn(Flux.empty());

        webTestClient.get()
                .uri("/api/v1/franchises")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.length()").isEqualTo(0);
    }

    @Test
    @DisplayName("GET /api/v1/franchises: debe retornar 200 OK con el árbol completo de sucursales y productos sin exponer version")
    void getAllFranchises_Success_Returns200WithFullTreeAndNoVersion() {
        Product product = Product.builder().id("p1").name("Big Mac").stock(50).build();
        Branch branch = Branch.builder().id("b1").name("Sucursal Centro").products(List.of(product)).build();
        Franchise f1 = Franchise.builder().id("f1").name("McDonalds").version(3L).branches(List.of(branch)).build();
        Franchise f2 = Franchise.builder().id("f2").name("Wendy's").version(1L).branches(List.of()).build();

        when(franchiseUseCase.getAllFranchises()).thenReturn(Flux.just(f1, f2));

        webTestClient.get()
                .uri("/api/v1/franchises")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.length()").isEqualTo(2)
                .jsonPath("$[0].id").isEqualTo("f1")
                .jsonPath("$[0].name").isEqualTo("McDonalds")
                .jsonPath("$[0].version").doesNotExist()
                .jsonPath("$[0].branches.length()").isEqualTo(1)
                .jsonPath("$[0].branches[0].id").isEqualTo("b1")
                .jsonPath("$[0].branches[0].name").isEqualTo("Sucursal Centro")
                .jsonPath("$[0].branches[0].products.length()").isEqualTo(1)
                .jsonPath("$[0].branches[0].products[0].id").isEqualTo("p1")
                .jsonPath("$[0].branches[0].products[0].name").isEqualTo("Big Mac")
                .jsonPath("$[0].branches[0].products[0].stock").isEqualTo(50)
                .jsonPath("$[1].id").isEqualTo("f2")
                .jsonPath("$[1].name").isEqualTo("Wendy's")
                .jsonPath("$[1].version").doesNotExist()
                .jsonPath("$[1].branches.length()").isEqualTo(0);
    }

    @Test
    @DisplayName("GET /api/v1/franchises/{id}: debe retornar 200 OK con la franquicia, sucursales y productos sin exponer version")
    void getFranchiseById_Success_Returns200WithFullTreeAndNoVersion() {
        Product product = Product.builder().id("p1").name("Whopper").stock(35).build();
        Branch branch = Branch.builder().id("b1").name("Sucursal Norte").products(List.of(product)).build();
        Franchise franchise = Franchise.builder().id("f1").name("Burger King").version(5L).branches(List.of(branch)).build();

        when(franchiseUseCase.getFranchiseById("f1")).thenReturn(Mono.just(franchise));

        webTestClient.get()
                .uri("/api/v1/franchises/{id}", "f1")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").isEqualTo("f1")
                .jsonPath("$.name").isEqualTo("Burger King")
                .jsonPath("$.version").doesNotExist()
                .jsonPath("$.branches.length()").isEqualTo(1)
                .jsonPath("$.branches[0].id").isEqualTo("b1")
                .jsonPath("$.branches[0].name").isEqualTo("Sucursal Norte")
                .jsonPath("$.branches[0].products.length()").isEqualTo(1)
                .jsonPath("$.branches[0].products[0].id").isEqualTo("p1")
                .jsonPath("$.branches[0].products[0].name").isEqualTo("Whopper")
                .jsonPath("$.branches[0].products[0].stock").isEqualTo(35);
    }

    @Test
    @DisplayName("GET /api/v1/franchises/{id}: debe retornar 404 Not Found con el mismo esquema ErrorResponse cuando no existe")
    void getFranchiseById_NotFound_Returns404WithErrorResponse() {
        when(franchiseUseCase.getFranchiseById("f99"))
                .thenReturn(Mono.error(new ResourceNotFoundException("Franchise not found with id: f99")));

        webTestClient.get()
                .uri("/api/v1/franchises/{id}", "f99")
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.timestamp").isNotEmpty()
                .jsonPath("$.status").isEqualTo(404)
                .jsonPath("$.error").isEqualTo("Not Found")
                .jsonPath("$.message").isEqualTo("Franchise not found with id: f99")
                .jsonPath("$.path").isEqualTo("/api/v1/franchises/f99");
    }

    // --- Hallazgo #9: ServerWebInputException y Validaciones Parametrizadas por Endpoint ---

    static Stream<Arguments> malformedInputPayloads() {
        return Stream.of(
                Arguments.of(HttpMethod.POST, "/api/v1/franchises", "{\"name\": "),
                Arguments.of(HttpMethod.POST, "/api/v1/franchises/f1/branches/b1/products", "{\"name\": \"Papas\", \"stock\": \"no-es-numero\"}"),
                Arguments.of(HttpMethod.PATCH, "/api/v1/franchises/f1/branches/b1/products/p1/stock", "{\"stock\": \"invalido\"}")
        );
    }

    @ParameterizedTest(name = "ServerWebInputException [{index}]: {0} {1} con JSON inválido debe retornar 400 sin filtrar detalles de Jackson")
    @MethodSource("malformedInputPayloads")
    void malformedJsonOrInvalidType_ShouldReturn400StandardErrorResponseWithoutLeakingJacksonDetails(
            HttpMethod method, String uri, String rawBody) {
        webTestClient.method(method)
                .uri(uri)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(rawBody)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .consumeWith(result -> {
                    String fullBody = new String(result.getResponseBodyContent(), java.nio.charset.StandardCharsets.UTF_8);
                    assertThat(fullBody)
                            .doesNotContainIgnoringCase("jackson")
                            .doesNotContainIgnoringCase("com.fasterxml")
                            .doesNotContainIgnoringCase("Cannot deserialize")
                            .doesNotContainIgnoringCase("java.");
                })
                .jsonPath("$.timestamp").isNotEmpty()
                .jsonPath("$.status").isEqualTo(400)
                .jsonPath("$.error").isEqualTo("Bad Request")
                .jsonPath("$.message").value(msg -> {
                    String text = (String) msg;
                    assertThat(text).isEqualTo("Formato de petición JSON inválido o malformado");
                    assertThat(text)
                            .doesNotContainIgnoringCase("jackson")
                            .doesNotContainIgnoringCase("JsonParseException")
                            .doesNotContainIgnoringCase("InvalidFormatException")
                            .doesNotContainIgnoringCase("Cannot deserialize")
                            .doesNotContainIgnoringCase("com.fasterxml")
                            .doesNotContainIgnoringCase("java.")
                            .doesNotContainIgnoringCase("Failed to read HTTP message");
                })
                .jsonPath("$.path").isEqualTo(uri);

        verifyNoInteractions(franchiseUseCase);
    }

    @Test
    @DisplayName("Validación @Valid: debe conservar su mensaje específico por campo y NO caer en el texto fijo de JSON malformado")
    void validationError_ShouldPreserveFieldMessageAndNotFallIntoGenericMalformedJsonText() {
        webTestClient.post()
                .uri("/api/v1/franchises")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"name\": \"   \"}")
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.status").isEqualTo(400)
                .jsonPath("$.error").isEqualTo("Bad Request")
                .jsonPath("$.message").value(msg -> {
                    String text = (String) msg;
                    assertThat(text).isEqualTo("name: El nombre de la franquicia es obligatorio");
                    assertThat(text).isNotEqualTo("Formato de petición JSON inválido o malformado");
                })
                .jsonPath("$.path").isEqualTo("/api/v1/franchises");

        verifyNoInteractions(franchiseUseCase);
    }

    static Stream<Arguments> invalidValidationPayloadsByEndpoint() {
        return Stream.of(
                // 1. POST /api/v1/franchises (CreateFranchiseRequest)
                Arguments.of(HttpMethod.POST, "/api/v1/franchises", "{\"name\": null}", "name"),
                Arguments.of(HttpMethod.POST, "/api/v1/franchises", "{\"name\": \"\"}", "name"),
                Arguments.of(HttpMethod.POST, "/api/v1/franchises", "{\"name\": \"   \"}", "name"),

                // 2. POST /api/v1/franchises/f1/branches (CreateBranchRequest)
                Arguments.of(HttpMethod.POST, "/api/v1/franchises/f1/branches", "{\"name\": null}", "name"),
                Arguments.of(HttpMethod.POST, "/api/v1/franchises/f1/branches", "{\"name\": \"\"}", "name"),
                Arguments.of(HttpMethod.POST, "/api/v1/franchises/f1/branches", "{\"name\": \"   \"}", "name"),

                // 3. POST /api/v1/franchises/f1/branches/b1/products (CreateProductRequest: name y stock)
                Arguments.of(HttpMethod.POST, "/api/v1/franchises/f1/branches/b1/products", "{\"name\": null, \"stock\": 10}", "name"),
                Arguments.of(HttpMethod.POST, "/api/v1/franchises/f1/branches/b1/products", "{\"name\": \"\", \"stock\": 10}", "name"),
                Arguments.of(HttpMethod.POST, "/api/v1/franchises/f1/branches/b1/products", "{\"name\": \"   \", \"stock\": 10}", "name"),
                Arguments.of(HttpMethod.POST, "/api/v1/franchises/f1/branches/b1/products", "{\"name\": \"Papas\", \"stock\": null}", "stock"),
                Arguments.of(HttpMethod.POST, "/api/v1/franchises/f1/branches/b1/products", "{\"name\": \"Papas\", \"stock\": -1}", "stock"),

                // 4. PATCH /api/v1/franchises/f1/branches/b1/products/p1/stock (UpdateStockRequest)
                Arguments.of(HttpMethod.PATCH, "/api/v1/franchises/f1/branches/b1/products/p1/stock", "{\"stock\": null}", "stock"),
                Arguments.of(HttpMethod.PATCH, "/api/v1/franchises/f1/branches/b1/products/p1/stock", "{\"stock\": -1}", "stock"),

                // 5. PATCH /api/v1/franchises/f1/name (UpdateNameRequest - Franquicia)
                Arguments.of(HttpMethod.PATCH, "/api/v1/franchises/f1/name", "{\"name\": null}", "name"),
                Arguments.of(HttpMethod.PATCH, "/api/v1/franchises/f1/name", "{\"name\": \"\"}", "name"),
                Arguments.of(HttpMethod.PATCH, "/api/v1/franchises/f1/name", "{\"name\": \"   \"}", "name"),

                // 6. PATCH /api/v1/franchises/f1/branches/b1/name (UpdateNameRequest - Sucursal)
                Arguments.of(HttpMethod.PATCH, "/api/v1/franchises/f1/branches/b1/name", "{\"name\": null}", "name"),
                Arguments.of(HttpMethod.PATCH, "/api/v1/franchises/f1/branches/b1/name", "{\"name\": \"\"}", "name"),
                Arguments.of(HttpMethod.PATCH, "/api/v1/franchises/f1/branches/b1/name", "{\"name\": \"   \"}", "name"),

                // 7. PATCH /api/v1/franchises/f1/branches/b1/products/p1/name (UpdateNameRequest - Producto)
                Arguments.of(HttpMethod.PATCH, "/api/v1/franchises/f1/branches/b1/products/p1/name", "{\"name\": null}", "name"),
                Arguments.of(HttpMethod.PATCH, "/api/v1/franchises/f1/branches/b1/products/p1/name", "{\"name\": \"\"}", "name"),
                Arguments.of(HttpMethod.PATCH, "/api/v1/franchises/f1/branches/b1/products/p1/name", "{\"name\": \"   \"}", "name")
        );
    }

    @ParameterizedTest(name = "Validación 400 [{index}]: {0} {1} con campo inválido ''{3}''")
    @MethodSource("invalidValidationPayloadsByEndpoint")
    void validationErrors_ParameterizedByEndpoint_ShouldReturn400AndNeverCallUseCase(
            HttpMethod method, String uri, String jsonBody, String expectedField) {
        webTestClient.method(method)
                .uri(uri)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(jsonBody)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.timestamp").isNotEmpty()
                .jsonPath("$.status").isEqualTo(400)
                .jsonPath("$.error").isEqualTo("Bad Request")
                .jsonPath("$.message").value(msg -> assertThat((String) msg).contains(expectedField))
                .jsonPath("$.path").isEqualTo(uri);

        verifyNoInteractions(franchiseUseCase);
    }

    static Stream<Arguments> zeroStockValidEndpoints() {
        return Stream.of(
                Arguments.of(HttpMethod.POST, "/api/v1/franchises/f1/branches/b1/products", "{\"name\": \"Agua\", \"stock\": 0}", 201),
                Arguments.of(HttpMethod.PATCH, "/api/v1/franchises/f1/branches/b1/products/p1/stock", "{\"stock\": 0}", 200)
        );
    }

    @ParameterizedTest(name = "Stock 0 válido [{index}]: {0} {1} debe retornar {3}")
    @MethodSource("zeroStockValidEndpoints")
    void zeroStock_ParameterizedByEndpoint_ShouldBeAcceptedAsValid(
            HttpMethod method, String uri, String jsonBody, int expectedStatus) {
        Product productZero = Product.builder().id("p1").name("Agua").stock(0).build();
        Branch branch = Branch.builder().id("b1").name("Sucursal 1").products(List.of(productZero)).build();
        Franchise saved = Franchise.builder().id("f1").name("Franquicia").branches(List.of(branch)).build();

        if (method == HttpMethod.POST) {
            when(franchiseUseCase.addProduct("f1", "b1", "Agua", 0)).thenReturn(Mono.just(saved));
        } else {
            when(franchiseUseCase.updateProductStock("f1", "b1", "p1", 0)).thenReturn(Mono.just(saved));
        }

        webTestClient.method(method)
                .uri(uri)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(jsonBody)
                .exchange()
                .expectStatus().isEqualTo(expectedStatus)
                .expectBody()
                .jsonPath("$.branches[0].products[0].stock").isEqualTo(0);

        if (method == HttpMethod.POST) {
            verify(franchiseUseCase).addProduct("f1", "b1", "Agua", 0);
        } else {
            verify(franchiseUseCase).updateProductStock("f1", "b1", "p1", 0);
        }
    }
}
