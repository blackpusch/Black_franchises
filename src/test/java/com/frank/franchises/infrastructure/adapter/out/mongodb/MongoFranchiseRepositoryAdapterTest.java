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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
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
                .branches(List.of(branch))
                .build();

        FranchiseDocument savedDocument = FranchiseDocument.builder()
                .id("f1")
                .name("Franquicia Central")
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
}
