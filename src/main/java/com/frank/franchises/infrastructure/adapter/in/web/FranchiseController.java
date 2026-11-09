package com.frank.franchises.infrastructure.adapter.in.web;

import com.frank.franchises.domain.port.in.FranchiseUseCase;
import com.frank.franchises.infrastructure.adapter.in.web.dto.*;
import com.frank.franchises.infrastructure.adapter.in.web.error.ErrorResponse;
import com.frank.franchises.infrastructure.adapter.in.web.mapper.FranchiseRestMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/franchises")
@RequiredArgsConstructor
@Tag(name = "Franchises", description = "Operaciones reactivas para gestión de franquicias, sucursales y productos")
public class FranchiseController {

    private final FranchiseUseCase franchiseUseCase;

    // a) Crear una nueva franquicia
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Crear franquicia", description = "Crea una nueva franquicia con nombre y lista inicial de sucursales vacía")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Franquicia creada exitosamente",
                    content = @Content(schema = @Schema(implementation = FranchiseResponse.class))),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
            @ApiResponse(responseCode = "409", description = "Ya existe una franquicia con el mismo nombre")
    })
    public Mono<FranchiseResponse> createFranchise(@Valid @RequestBody CreateFranchiseRequest request) {
        return franchiseUseCase.createFranchise(request.name())
                .map(FranchiseRestMapper::toResponse);
    }

    // a.1) Listar todas las franquicias
    @GetMapping
    @Operation(summary = "Listar franquicias",
            description = "Retorna el listado completo de franquicias (con su árbol de sucursales y productos) ordenado por nombre e id, sin paginación")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Listado de franquicias obtenido exitosamente",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = FranchiseResponse.class))))
    })
    public Flux<FranchiseResponse> getAllFranchises() {
        return franchiseUseCase.getAllFranchises()
                .map(FranchiseRestMapper::toResponse);
    }

    // a.2) Obtener una franquicia por su ID
    @GetMapping("/{franchiseId}")
    @Operation(summary = "Obtener franquicia por ID",
            description = "Retorna el detalle de una franquicia específica con todas sus sucursales y productos")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Franquicia encontrada",
                    content = @Content(schema = @Schema(implementation = FranchiseResponse.class))),
            @ApiResponse(responseCode = "404", description = "Franquicia no encontrada",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public Mono<FranchiseResponse> getFranchiseById(@PathVariable String franchiseId) {
        return franchiseUseCase.getFranchiseById(franchiseId)
                .map(FranchiseRestMapper::toResponse);
    }

    // b) Agregar una nueva sucursal a una franquicia
    @PostMapping("/{franchiseId}/branches")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Agregar sucursal", description = "Agrega una nueva sucursal a la franquicia identificada por franchiseId")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Sucursal agregada exitosamente",
                    content = @Content(schema = @Schema(implementation = FranchiseResponse.class))),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
            @ApiResponse(responseCode = "404", description = "Franquicia no encontrada"),
            @ApiResponse(responseCode = "409", description = "Conflicto de concurrencia o nombre de sucursal duplicado")
    })
    public Mono<FranchiseResponse> addBranch(
            @PathVariable String franchiseId,
            @Valid @RequestBody CreateBranchRequest request) {
        return franchiseUseCase.addBranch(franchiseId, request.name())
                .map(FranchiseRestMapper::toResponse);
    }

    // c) Agregar un nuevo producto a una sucursal
    @PostMapping("/{franchiseId}/branches/{branchId}/products")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Agregar producto", description = "Agrega un nuevo producto con stock a una sucursal de una franquicia")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Producto agregado exitosamente",
                    content = @Content(schema = @Schema(implementation = FranchiseResponse.class))),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
            @ApiResponse(responseCode = "404", description = "Franquicia o sucursal no encontrada"),
            @ApiResponse(responseCode = "409", description = "Conflicto de concurrencia o nombre de producto duplicado")
    })
    public Mono<FranchiseResponse> addProduct(
            @PathVariable String franchiseId,
            @PathVariable String branchId,
            @Valid @RequestBody CreateProductRequest request) {
        return franchiseUseCase.addProduct(franchiseId, branchId, request.name(), request.stock())
                .map(FranchiseRestMapper::toResponse);
    }

    // d) Eliminar un producto de una sucursal
    @DeleteMapping("/{franchiseId}/branches/{branchId}/products/{productId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Eliminar producto", description = "Elimina un producto existente de una sucursal específica")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Producto eliminado exitosamente"),
            @ApiResponse(responseCode = "404", description = "Franquicia, sucursal o producto no encontrado"),
            @ApiResponse(responseCode = "409", description = "Conflicto de concurrencia")
    })
    public Mono<Void> removeProduct(
            @PathVariable String franchiseId,
            @PathVariable String branchId,
            @PathVariable String productId) {
        return franchiseUseCase.removeProduct(franchiseId, branchId, productId);
    }

    // e) Modificar el stock de un producto
    @PatchMapping("/{franchiseId}/branches/{branchId}/products/{productId}/stock")
    @Operation(summary = "Actualizar stock de producto", description = "Modifica la cantidad de stock disponible de un producto")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Stock actualizado exitosamente",
                    content = @Content(schema = @Schema(implementation = FranchiseResponse.class))),
            @ApiResponse(responseCode = "400", description = "Stock inválido"),
            @ApiResponse(responseCode = "404", description = "Franquicia, sucursal o producto no encontrado"),
            @ApiResponse(responseCode = "409", description = "Conflicto de concurrencia")
    })
    public Mono<FranchiseResponse> updateProductStock(
            @PathVariable String franchiseId,
            @PathVariable String branchId,
            @PathVariable String productId,
            @Valid @RequestBody UpdateStockRequest request) {
        return franchiseUseCase.updateProductStock(franchiseId, branchId, productId, request.stock())
                .map(FranchiseRestMapper::toResponse);
    }

    // f) Obtener el producto con más stock por sucursal para una franquicia
    @GetMapping("/{franchiseId}/top-stock-products")
    @Operation(summary = "Producto con mayor stock por sucursal",
            description = "Retorna el producto con más existencias en inventario para cada sucursal de la franquicia")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Productos obtenidos exitosamente"),
            @ApiResponse(responseCode = "404", description = "Franquicia no encontrada")
    })
    public Flux<BranchTopProductResponse> getTopStockProductsByBranch(@PathVariable String franchiseId) {
        return franchiseUseCase.getTopStockProductsByBranch(franchiseId)
                .map(FranchiseRestMapper::toResponse);
    }

    // g) [PLUS] Actualizar el nombre de una franquicia
    @PatchMapping("/{franchiseId}/name")
    @Operation(summary = "Actualizar nombre de franquicia", description = "Permite renombrar una franquicia existente")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Nombre de franquicia actualizado",
                    content = @Content(schema = @Schema(implementation = FranchiseResponse.class))),
            @ApiResponse(responseCode = "400", description = "Nombre inválido"),
            @ApiResponse(responseCode = "404", description = "Franquicia no encontrada"),
            @ApiResponse(responseCode = "409", description = "Conflicto de concurrencia o nombre de franquicia duplicado")
    })
    public Mono<FranchiseResponse> updateFranchiseName(
            @PathVariable String franchiseId,
            @Valid @RequestBody UpdateNameRequest request) {
        return franchiseUseCase.updateFranchiseName(franchiseId, request.name())
                .map(FranchiseRestMapper::toResponse);
    }

    // h) [PLUS] Actualizar el nombre de una sucursal
    @PatchMapping("/{franchiseId}/branches/{branchId}/name")
    @Operation(summary = "Actualizar nombre de sucursal", description = "Permite renombrar una sucursal existente")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Nombre de sucursal actualizado",
                    content = @Content(schema = @Schema(implementation = FranchiseResponse.class))),
            @ApiResponse(responseCode = "400", description = "Nombre inválido"),
            @ApiResponse(responseCode = "404", description = "Franquicia o sucursal no encontrada"),
            @ApiResponse(responseCode = "409", description = "Conflicto de concurrencia o nombre de sucursal duplicado")
    })
    public Mono<FranchiseResponse> updateBranchName(
            @PathVariable String franchiseId,
            @PathVariable String branchId,
            @Valid @RequestBody UpdateNameRequest request) {
        return franchiseUseCase.updateBranchName(franchiseId, branchId, request.name())
                .map(FranchiseRestMapper::toResponse);
    }

    // i) [PLUS] Actualizar el nombre de un producto
    @PatchMapping("/{franchiseId}/branches/{branchId}/products/{productId}/name")
    @Operation(summary = "Actualizar nombre de producto", description = "Permite renombrar un producto existente en una sucursal")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Nombre de producto actualizado",
                    content = @Content(schema = @Schema(implementation = FranchiseResponse.class))),
            @ApiResponse(responseCode = "400", description = "Nombre inválido"),
            @ApiResponse(responseCode = "404", description = "Franquicia, sucursal o producto no encontrado"),
            @ApiResponse(responseCode = "409", description = "Conflicto de concurrencia o nombre de producto duplicado")
    })
    public Mono<FranchiseResponse> updateProductName(
            @PathVariable String franchiseId,
            @PathVariable String branchId,
            @PathVariable String productId,
            @Valid @RequestBody UpdateNameRequest request) {
        return franchiseUseCase.updateProductName(franchiseId, branchId, productId, request.name())
                .map(FranchiseRestMapper::toResponse);
    }
}
