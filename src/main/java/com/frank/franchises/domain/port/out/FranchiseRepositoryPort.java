package com.frank.franchises.domain.port.out;

import com.frank.franchises.domain.model.Franchise;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface FranchiseRepositoryPort {

    Mono<Franchise> save(Franchise franchise);

    /**
     * Recupera una franquicia por su ID para flujos de mutación (escritura).
     * Si el documento en MongoDB es un documento legado sin campo {@code version},
     * inicializa atómicamente {@code version = 0L} para habilitar el bloqueo optimista.
     */
    Mono<Franchise> findById(String id);

    /**
     * Recupera una franquicia por su ID en modo estrictamente de solo lectura (para consultas GET),
     * sin realizar escrituras ni inicializar el campo {@code version} en documentos legados.
     */
    Mono<Franchise> findByIdReadOnly(String id);

    Flux<Franchise> findAll();

    Mono<Boolean> existsByNameIgnoreCase(String name);

    Mono<Boolean> existsByNameIgnoreCaseAndIdNot(String name, String excludedId);
}
