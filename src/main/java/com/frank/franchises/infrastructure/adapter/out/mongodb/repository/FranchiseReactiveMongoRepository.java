package com.frank.franchises.infrastructure.adapter.out.mongodb.repository;

import com.frank.franchises.infrastructure.adapter.out.mongodb.document.FranchiseDocument;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FranchiseReactiveMongoRepository extends ReactiveMongoRepository<FranchiseDocument, String> {
}
