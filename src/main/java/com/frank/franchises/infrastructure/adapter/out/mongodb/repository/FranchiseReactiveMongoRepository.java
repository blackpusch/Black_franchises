package com.frank.franchises.infrastructure.adapter.out.mongodb.repository;

import com.frank.franchises.infrastructure.adapter.out.mongodb.document.FranchiseDocument;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface FranchiseReactiveMongoRepository extends ReactiveMongoRepository<FranchiseDocument, String> {

    Mono<Boolean> existsByNameIgnoreCase(String name);

    Mono<Boolean> existsByNameIgnoreCaseAndIdNot(String name, String id);
}
