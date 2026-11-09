package com.frank.franchises.infrastructure.adapter.out.mongodb.mapper;

import com.frank.franchises.domain.model.Branch;
import com.frank.franchises.domain.model.Franchise;
import com.frank.franchises.domain.model.Product;
import com.frank.franchises.infrastructure.adapter.out.mongodb.document.BranchDocument;
import com.frank.franchises.infrastructure.adapter.out.mongodb.document.FranchiseDocument;
import com.frank.franchises.infrastructure.adapter.out.mongodb.document.ProductDocument;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FranchiseDocumentMapperTest {

    private FranchiseDocumentMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new FranchiseDocumentMapper();
    }

    @Test
    @DisplayName("toDocument y toDomain: todos los métodos deben retornar null cuando reciben null")
    void mapperMethods_WhenInputsAreNull_ShouldReturnNull() {
        assertThat(mapper.toDocument(null)).isNull();
        assertThat(mapper.toDomain(null)).isNull();
        assertThat(mapper.toBranchDocument(null)).isNull();
        assertThat(mapper.toBranchDomain(null)).isNull();
        assertThat(mapper.toProductDocument(null)).isNull();
        assertThat(mapper.toProductDomain(null)).isNull();
    }

    @Test
    @DisplayName("toDocument y toDomain: deben normalizar listas null, filtrar elementos null y convertir stock null en 0")
    void toDocumentAndToDomain_WhenListsAreNullOrContainNullElements_ShouldNormalizeAndFilterNulls() {
        Product productNullStock = Product.builder().id("p1").name("Agua").stock(null).build();
        Branch branchWithNullElement = Branch.builder()
                .id("b1")
                .name("Sucursal 1")
                .products(Arrays.asList(productNullStock, null))
                .build();
        Franchise domainWithNullElements = Franchise.builder()
                .id("f1")
                .name("Franquicia")
                .version(4L)
                .branches(Arrays.asList(branchWithNullElement, null))
                .build();

        FranchiseDocument doc = mapper.toDocument(domainWithNullElements);
        assertThat(doc).isNotNull();
        assertThat(doc.getBranches()).hasSize(1).doesNotContainNull();
        assertThat(doc.getBranches().getFirst().getProducts()).hasSize(1).doesNotContainNull();
        assertThat(doc.getBranches().getFirst().getProducts().getFirst().getStock()).isZero();

        Franchise domainNullBranches = Franchise.builder().id("f0").name("Vacia").branches(null).build();
        assertThat(mapper.toDocument(domainNullBranches).getBranches()).isNotNull().isEmpty();

        Branch branchNullProducts = Branch.builder().id("b0").name("Vacia").products(null).build();
        assertThat(mapper.toBranchDocument(branchNullProducts).getProducts()).isNotNull().isEmpty();

        ProductDocument productDocNullStock = ProductDocument.builder().id("p2").name("Jugo").stock(null).build();
        BranchDocument branchDocWithNullElement = BranchDocument.builder()
                .id("b2")
                .name("Sucursal Doc")
                .products(Arrays.asList(productDocNullStock, null))
                .build();
        FranchiseDocument docWithNullElements = FranchiseDocument.builder()
                .id("f2")
                .name("Franquicia Doc")
                .version(8L)
                .branches(Arrays.asList(branchDocWithNullElement, null))
                .build();

        Franchise mappedDomain = mapper.toDomain(docWithNullElements);
        assertThat(mappedDomain).isNotNull();
        assertThat(mappedDomain.getBranches()).hasSize(1).doesNotContainNull();
        assertThat(mappedDomain.getBranches().getFirst().getProducts()).hasSize(1).doesNotContainNull();
        assertThat(mappedDomain.getBranches().getFirst().getProducts().getFirst().getStock()).isZero();
    }

    @Test
    @DisplayName("Ida y vuelta de version: toDocument y toDomain preservan version (no nulo y nulo) junto con todo el árbol")
    void versionRoundTrip_ShouldPreserveVersionFromDomainToDocumentAndBack() {
        Product product = Product.builder().id("p1").name("Hamburguesa").stock(30).build();
        Branch branch = Branch.builder().id("b1").name("Sucursal Centro").products(List.of(product)).build();
        Franchise original = Franchise.builder()
                .id("f1")
                .name("McDonalds")
                .version(42L)
                .branches(List.of(branch))
                .build();

        FranchiseDocument document = mapper.toDocument(original);
        assertThat(document.getVersion()).isEqualTo(42L);

        Franchise roundTrip = mapper.toDomain(document);
        assertThat(roundTrip.getVersion()).isEqualTo(42L);
        assertThat(roundTrip.getId()).isEqualTo("f1");
        assertThat(roundTrip.getName()).isEqualTo("McDonalds");
        assertThat(roundTrip.getBranches()).hasSize(1);
        assertThat(roundTrip.getBranches().getFirst().getProducts()).hasSize(1);
        assertThat(roundTrip.getBranches().getFirst().getProducts().getFirst().getStock()).isEqualTo(30);

        Franchise unversioned = Franchise.builder().id("f2").name("Nueva").version(null).branches(List.of()).build();
        FranchiseDocument unversionedDoc = mapper.toDocument(unversioned);
        assertThat(unversionedDoc.getVersion()).isNull();
        assertThat(mapper.toDomain(unversionedDoc).getVersion()).isNull();
    }
}
