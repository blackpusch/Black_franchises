package com.frank.franchises.infrastructure.adapter.out.mongodb;

import com.frank.franchises.domain.model.Branch;
import com.frank.franchises.domain.model.Franchise;
import com.frank.franchises.domain.model.Product;
import com.frank.franchises.infrastructure.adapter.out.mongodb.document.BranchDocument;
import com.frank.franchises.infrastructure.adapter.out.mongodb.document.FranchiseDocument;
import com.frank.franchises.infrastructure.adapter.out.mongodb.document.ProductDocument;
import com.frank.franchises.infrastructure.adapter.out.mongodb.mapper.FranchiseDocumentMapper;
import com.frank.franchises.infrastructure.adapter.out.mongodb.repository.FranchiseReactiveMongoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Sort;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MongoFranchiseRepositoryAdapterTest {

    @Mock
    private FranchiseReactiveMongoRepository repository;

    private FranchiseDocumentMapper mapper;
    private MongoFranchiseRepositoryAdapter adapter;

    @BeforeEach
    void setUp() {
        mapper = new FranchiseDocumentMapper();
        adapter = new MongoFranchiseRepositoryAdapter(repository, mapper);
    }

    @Test
    @DisplayName("Debe guardar y retornar la franquicia mapeada al modelo de dominio")
    void shouldSaveAndReturnDomainFranchise() {
        Product product = Product.builder()
                .id("p1")
                .name("Hamburguesa")
                .stock(20)
                .build();

        Branch branch = Branch.builder()
                .id("b1")
                .name("Sucursal Norte")
                .products(List.of(product))
                .build();

        Franchise franchise = Franchise.builder()
                .id("f1")
                .name("Franquicia Central")
                .version(1L)
                .branches(List.of(branch))
                .build();

        FranchiseDocument savedDocument = FranchiseDocument.builder()
                .id("f1")
                .name("Franquicia Central")
                .version(2L)
                .branches(List.of(
                        BranchDocument.builder()
                                .id("b1")
                                .name("Sucursal Norte")
                                .products(List.of(
                                        ProductDocument.builder()
                                                .id("p1")
                                                .name("Hamburguesa")
                                                .stock(20)
                                                .build()
                                ))
                                .build()
                ))
                .build();

        when(repository.save(any(FranchiseDocument.class))).thenReturn(Mono.just(savedDocument));

        Mono<Franchise> result = adapter.save(franchise);

        StepVerifier.create(result)
                .assertNext(saved -> {
                    assertThat(saved).isNotNull();
                    assertThat(saved.getId()).isEqualTo("f1");
                    assertThat(saved.getName()).isEqualTo("Franquicia Central");
                    assertThat(saved.getVersion()).isEqualTo(2L);
                    assertThat(saved.getBranches()).hasSize(1);

                    Branch savedBranch = saved.getBranches().getFirst();
                    assertThat(savedBranch.getId()).isEqualTo("b1");
                    assertThat(savedBranch.getName()).isEqualTo("Sucursal Norte");
                    assertThat(savedBranch.getProducts()).hasSize(1);

                    Product savedProduct = savedBranch.getProducts().getFirst();
                    assertThat(savedProduct.getId()).isEqualTo("p1");
                    assertThat(savedProduct.getName()).isEqualTo("Hamburguesa");
                    assertThat(savedProduct.getStock()).isEqualTo(20);
                })
                .verifyComplete();

        verify(repository).save(any(FranchiseDocument.class));
    }

    @Test
    @DisplayName("Debe buscar por ID y retornar la franquicia encontrada mapeada a dominio")
    void shouldFindByIdAndReturnDomainFranchise() {
        FranchiseDocument document = FranchiseDocument.builder()
                .id("f1")
                .name("Franquicia Express")
                .branches(List.of())
                .build();

        when(repository.findById("f1")).thenReturn(Mono.just(document));

        Mono<Franchise> result = adapter.findById("f1");

        StepVerifier.create(result)
                .assertNext(found -> {
                    assertThat(found).isNotNull();
                    assertThat(found.getId()).isEqualTo("f1");
                    assertThat(found.getName()).isEqualTo("Franquicia Express");
                    assertThat(found.getVersion()).isEqualTo(0L);
                    assertThat(found.getBranches()).isEmpty();
                })
                .verifyComplete();

        verify(repository).findById("f1");
    }

    @Test
    @DisplayName("Debe retornar Mono.empty() cuando la franquicia no existe por ID")
    void shouldReturnEmptyWhenNotFoundById() {
        when(repository.findById("f99")).thenReturn(Mono.empty());

        Mono<Franchise> result = adapter.findById("f99");

        StepVerifier.create(result)
                .verifyComplete();

        verify(repository).findById("f99");
    }

    @Test
    @DisplayName("Debe inicializar version=0L en MongoDB cuando se lee un documento legado con version null")
    void shouldInitializeVersionInMongoWhenLegacyDocumentHasNullVersion() {
        org.springframework.data.mongodb.core.ReactiveMongoTemplate mongoTemplate =
                org.mockito.Mockito.mock(org.springframework.data.mongodb.core.ReactiveMongoTemplate.class);
        MongoFranchiseRepositoryAdapter adapterWithTemplate =
                new MongoFranchiseRepositoryAdapter(repository, mapper, mongoTemplate);

        FranchiseDocument legacyDocument = FranchiseDocument.builder()
                .id("f-legacy")
                .name("Franquicia Legada")
                .version(null)
                .branches(List.of())
                .build();

        when(repository.findById("f-legacy")).thenReturn(Mono.just(legacyDocument));
        when(mongoTemplate.updateFirst(
                any(org.springframework.data.mongodb.core.query.Query.class),
                any(org.springframework.data.mongodb.core.query.Update.class),
                org.mockito.ArgumentMatchers.eq(FranchiseDocument.class)
        )).thenReturn(Mono.just(com.mongodb.client.result.UpdateResult.acknowledged(1L, 1L, null)));

        StepVerifier.create(adapterWithTemplate.findById("f-legacy"))
                .assertNext(found -> {
                    assertThat(found.getId()).isEqualTo("f-legacy");
                    assertThat(found.getVersion()).isEqualTo(0L);
                })
                .verifyComplete();

        verify(mongoTemplate).updateFirst(
                any(org.springframework.data.mongodb.core.query.Query.class),
                any(org.springframework.data.mongodb.core.query.Update.class),
                org.mockito.ArgumentMatchers.eq(FranchiseDocument.class)
        );
    }

    @Test
    @DisplayName("Debe mapear DuplicateKeyException del índice único uk_franchise_name_ci a DuplicateResourceException de dominio")
    void shouldMapDuplicateKeyExceptionToDuplicateResourceExceptionWhenNameIndexViolated() {
        Franchise franchise = Franchise.builder()
                .id("f1")
                .name("McDonalds")
                .branches(List.of())
                .build();

        when(repository.save(any(FranchiseDocument.class)))
                .thenReturn(Mono.error(new DuplicateKeyException(
                        "E11000 duplicate key error collection: franchises.franchises index: "
                                + FranchiseDocument.NAME_UNIQUE_INDEX + " dup key: { name: \"McDonalds\" }")));

        StepVerifier.create(adapter.save(franchise))
                .expectErrorMatches(err -> err instanceof com.frank.franchises.domain.exception.DuplicateResourceException
                        && err.getMessage().equals("Franchise already exists with name: McDonalds"))
                .verify();
    }

    @Test
    @DisplayName("No debe traducir DuplicateKeyException a DuplicateResourceException cuando proviene de otro índice distinto a uk_franchise_name_ci")
    void shouldNotMapDuplicateKeyExceptionWhenOtherIndexViolated() {
        Franchise franchise = Franchise.builder()
                .id("f1")
                .name("McDonalds")
                .branches(List.of())
                .build();

        DuplicateKeyException idDuplicateEx = new DuplicateKeyException(
                "E11000 duplicate key error collection: franchises.franchises index: _id_ dup key: { _id: \"f1\" }");
        when(repository.save(any(FranchiseDocument.class)))
                .thenReturn(Mono.error(idDuplicateEx));

        StepVerifier.create(adapter.save(franchise))
                .expectErrorMatches(err -> err == idDuplicateEx)
                .verify();
    }

    @Test
    @DisplayName("Debe delegar existsByNameIgnoreCase y existsByNameIgnoreCaseAndIdNot al repositorio reactivo")
    void shouldDelegateExistenceQueriesToRepository() {
        when(repository.existsByNameIgnoreCase("KFC")).thenReturn(Mono.just(true));
        when(repository.existsByNameIgnoreCaseAndIdNot("KFC", "f1")).thenReturn(Mono.just(false));

        StepVerifier.create(adapter.existsByNameIgnoreCase("KFC"))
                .expectNext(true)
                .verifyComplete();

        StepVerifier.create(adapter.existsByNameIgnoreCaseAndIdNot("KFC", "f1"))
                .expectNext(false)
                .verifyComplete();
    }

    @Test
    @DisplayName("FranchiseDocumentMapper.toDomain: debe normalizar listas null de branches y products a listas vacías")
    void shouldNormalizeNullBranchesAndProductsToEmptyLists() {
        FranchiseDocument docWithNullBranches = FranchiseDocument.builder()
                .id("f1")
                .name("Franquicia Sin Lista")
                .version(1L)
                .branches(null)
                .build();

        Franchise domain1 = mapper.toDomain(docWithNullBranches);
        assertThat(domain1.getBranches()).isNotNull().isEmpty();

        BranchDocument branchWithNullProducts = BranchDocument.builder()
                .id("b1")
                .name("Sucursal Sin Lista")
                .products(null)
                .build();
        FranchiseDocument docWithNullProducts = FranchiseDocument.builder()
                .id("f2")
                .name("Franquicia Con Sucursal")
                .version(1L)
                .branches(List.of(branchWithNullProducts))
                .build();

        Franchise domain2 = mapper.toDomain(docWithNullProducts);
        assertThat(domain2.getBranches()).hasSize(1);
        assertThat(domain2.getBranches().getFirst().getProducts()).isNotNull().isEmpty();
    }

    @Test
    @DisplayName("findAll: debe ordenar por name ASC y luego id ASC sin realizar interacciones de escritura")
    void shouldFindAllOrderedByNameAndIdWithoutWriteInteractions() {
        org.springframework.data.mongodb.core.ReactiveMongoTemplate mongoTemplate =
                org.mockito.Mockito.mock(org.springframework.data.mongodb.core.ReactiveMongoTemplate.class);
        MongoFranchiseRepositoryAdapter adapterWithTemplate =
                new MongoFranchiseRepositoryAdapter(repository, mapper, mongoTemplate);

        FranchiseDocument doc1 = FranchiseDocument.builder()
                .id("f2")
                .name("Acme")
                .version(1L)
                .branches(List.of(
                        BranchDocument.builder()
                                .id("b1")
                                .name("Sucursal Central")
                                .products(List.of(
                                        ProductDocument.builder().id("p1").name("Combo 1").stock(10).build()
                                ))
                                .build()
                ))
                .build();
        FranchiseDocument doc2 = FranchiseDocument.builder()
                .id("f1")
                .name("Burger King")
                .version(2L)
                .branches(List.of())
                .build();

        when(repository.findAll(any(Sort.class))).thenReturn(Flux.just(doc1, doc2));

        StepVerifier.create(adapterWithTemplate.findAll())
                .assertNext(first -> {
                    assertThat(first.getId()).isEqualTo("f2");
                    assertThat(first.getName()).isEqualTo("Acme");
                    assertThat(first.getBranches()).hasSize(1);
                    assertThat(first.getBranches().getFirst().getProducts()).hasSize(1);
                    assertThat(first.getBranches().getFirst().getProducts().getFirst().getName()).isEqualTo("Combo 1");
                })
                .assertNext(second -> {
                    assertThat(second.getId()).isEqualTo("f1");
                    assertThat(second.getName()).isEqualTo("Burger King");
                    assertThat(second.getBranches()).isEmpty();
                })
                .verifyComplete();

        ArgumentCaptor<Sort> sortCaptor = ArgumentCaptor.forClass(Sort.class);
        verify(repository).findAll(sortCaptor.capture());
        Sort capturedSort = sortCaptor.getValue();
        assertThat(capturedSort.toList())
                .containsExactly(
                        Sort.Order.asc("name"),
                        Sort.Order.asc("id")
                );

        verify(repository, never()).save(any());
        verifyNoInteractions(mongoTemplate);
    }

    @Test
    @DisplayName("findAll: documento legado con version null no dispara inicialización ni escrituras en MongoDB")
    void shouldFindAllKeepNullVersionWithoutInitializingInMongo() {
        org.springframework.data.mongodb.core.ReactiveMongoTemplate mongoTemplate =
                org.mockito.Mockito.mock(org.springframework.data.mongodb.core.ReactiveMongoTemplate.class);
        MongoFranchiseRepositoryAdapter adapterWithTemplate =
                new MongoFranchiseRepositoryAdapter(repository, mapper, mongoTemplate);

        FranchiseDocument legacyDoc = FranchiseDocument.builder()
                .id("f-legacy")
                .name("Franquicia Legada")
                .version(null)
                .branches(List.of())
                .build();

        when(repository.findAll(any(Sort.class))).thenReturn(Flux.just(legacyDoc));

        StepVerifier.create(adapterWithTemplate.findAll())
                .assertNext(found -> {
                    assertThat(found.getId()).isEqualTo("f-legacy");
                    assertThat(found.getName()).isEqualTo("Franquicia Legada");
                    assertThat(found.getVersion()).isNull();
                })
                .verifyComplete();

        assertThat(legacyDoc.getVersion()).isNull();
        verify(repository, never()).save(any());
        verifyNoInteractions(mongoTemplate);
    }

    @Test
    @DisplayName("findByIdReadOnly: documento legado con version null se retorna sin inicializar version ni escribir en MongoDB")
    void shouldFindByIdReadOnlyKeepNullVersionWithoutWriteInteractions() {
        org.springframework.data.mongodb.core.ReactiveMongoTemplate mongoTemplate =
                org.mockito.Mockito.mock(org.springframework.data.mongodb.core.ReactiveMongoTemplate.class);
        MongoFranchiseRepositoryAdapter adapterWithTemplate =
                new MongoFranchiseRepositoryAdapter(repository, mapper, mongoTemplate);

        FranchiseDocument legacyDoc = FranchiseDocument.builder()
                .id("f-legacy-ro")
                .name("Franquicia Legada RO")
                .version(null)
                .branches(List.of())
                .build();

        when(repository.findById("f-legacy-ro")).thenReturn(Mono.just(legacyDoc));

        StepVerifier.create(adapterWithTemplate.findByIdReadOnly("f-legacy-ro"))
                .assertNext(found -> {
                    assertThat(found.getId()).isEqualTo("f-legacy-ro");
                    assertThat(found.getName()).isEqualTo("Franquicia Legada RO");
                    assertThat(found.getVersion()).isNull();
                })
                .verifyComplete();

        assertThat(legacyDoc.getVersion()).isNull();
        verify(repository, never()).save(any());
        verifyNoInteractions(mongoTemplate);
    }
}
