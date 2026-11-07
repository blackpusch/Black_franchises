package com.frank.franchises.infrastructure.adapter.out.mongodb;

import com.frank.franchises.domain.model.Franchise;
import com.frank.franchises.domain.port.out.FranchiseRepositoryPort;
import com.frank.franchises.infrastructure.adapter.out.mongodb.mapper.FranchiseDocumentMapper;
import com.frank.franchises.infrastructure.adapter.out.mongodb.repository.FranchiseReactiveMongoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
@RequiredArgsConstructor
public class MongoFranchiseRepositoryAdapter implements FranchiseRepositoryPort {

    private final FranchiseReactiveMongoRepository repository;
    private final FranchiseDocumentMapper mapper;

    @Override
    public Mono<Franchise> save(Franchise franchise) {
        return Mono.justOrEmpty(franchise)
                .map(mapper::toDocument)
                .flatMap(repository::save)
                .map(mapper::toDomain);
    }

    @Override
    public Mono<Franchise> findById(String id) {
        return repository.findById(id)
                .map(mapper::toDomain);
    }
}
