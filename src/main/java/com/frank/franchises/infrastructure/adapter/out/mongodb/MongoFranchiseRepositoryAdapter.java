package com.frank.franchises.infrastructure.adapter.out.mongodb;

import com.frank.franchises.domain.exception.DuplicateResourceException;
import com.frank.franchises.domain.model.Franchise;
import com.frank.franchises.domain.port.out.FranchiseRepositoryPort;
import com.frank.franchises.infrastructure.adapter.out.mongodb.document.FranchiseDocument;
import com.frank.franchises.infrastructure.adapter.out.mongodb.mapper.FranchiseDocumentMapper;
import com.frank.franchises.infrastructure.adapter.out.mongodb.repository.FranchiseReactiveMongoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public class MongoFranchiseRepositoryAdapter implements FranchiseRepositoryPort {

    private final FranchiseReactiveMongoRepository repository;
    private final FranchiseDocumentMapper mapper;
    private final ReactiveMongoTemplate mongoTemplate;

    public MongoFranchiseRepositoryAdapter(
            FranchiseReactiveMongoRepository repository,
            FranchiseDocumentMapper mapper) {
        this(repository, mapper, null);
    }

    @Autowired
    public MongoFranchiseRepositoryAdapter(
            FranchiseReactiveMongoRepository repository,
            FranchiseDocumentMapper mapper,
            ReactiveMongoTemplate mongoTemplate) {
        this.repository = repository;
        this.mapper = mapper;
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public Mono<Franchise> save(Franchise franchise) {
        return Mono.justOrEmpty(franchise)
                .map(mapper::toDocument)
                .flatMap(repository::save)
                .onErrorMap(DuplicateKeyException.class, ex ->
                        new DuplicateResourceException("Franchise already exists with name: " + franchise.getName()))
                .map(mapper::toDomain);
    }

    @Override
    public Mono<Franchise> findById(String id) {
        return repository.findById(id)
                .flatMap(this::ensureVersionInitialized)
                .map(mapper::toDomain);
    }

    @Override
    public Flux<Franchise> findAll() {
        return repository.findAll(Sort.by(Sort.Order.asc("name"), Sort.Order.asc("id")))
                .map(mapper::toDomain);
    }

    @Override
    public Mono<Boolean> existsByNameIgnoreCase(String name) {
        return repository.existsByNameIgnoreCase(name);
    }

    @Override
    public Mono<Boolean> existsByNameIgnoreCaseAndIdNot(String name, String excludedId) {
        return repository.existsByNameIgnoreCaseAndIdNot(name, excludedId);
    }

    /**
     * Si un documento legado en MongoDB fue creado sin campo 'version' (version == null),
     * Spring Data interpretaría save() como un INSERT nuevo y lanzaría DuplicateKeyException.
     * Se inicializa atómicamente version = 0L en BD y en memoria al leerlo.
     */
    private Mono<FranchiseDocument> ensureVersionInitialized(FranchiseDocument document) {
        if (document.getVersion() != null) {
            return Mono.just(document);
        }
        document.setVersion(0L);
        if (mongoTemplate == null) {
            return Mono.just(document);
        }
        Query legacyQuery = Query.query(Criteria.where("_id").is(document.getId()).and("version").isNull());
        Update initVersion = new Update().set("version", 0L);
        return mongoTemplate.updateFirst(legacyQuery, initVersion, FranchiseDocument.class)
                .thenReturn(document);
    }
}
