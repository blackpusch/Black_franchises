package com.frank.franchises.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DomainModelImmutabilityTest {

    @Test
    @DisplayName("getBranches y getProducts devuelven listas inmodificables que rechazan add, remove y clear con UnsupportedOperationException")
    void franchiseAndBranchLists_ShouldBeUnmodifiableAndRejectDirectMutations() {
        Product p1 = Product.builder().id("p1").name("Cafe").stock(10).build();
        Branch b1 = Branch.builder().id("b1").name("Sucursal Centro").products(List.of(p1)).build();
        Franchise franchise = Franchise.builder().id("f1").name("Franquicia").version(1L).branches(List.of(b1)).build();

        Branch extraBranch = Branch.builder().id("b2").name("Extra").build();
        Product extraProduct = Product.builder().id("p2").name("Te").stock(5).build();

        assertThatThrownBy(() -> franchise.getBranches().add(extraBranch))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> franchise.getBranches().removeFirst())
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> franchise.getBranches().clear())
                .isInstanceOf(UnsupportedOperationException.class);

        Branch storedBranch = franchise.getBranches().getFirst();
        assertThatThrownBy(() -> storedBranch.getProducts().add(extraProduct))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> storedBranch.getProducts().removeFirst())
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> storedBranch.getProducts().clear())
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("Copia defensiva al construir: mutar la lista original pasada al builder o toBuilder no altera el estado interno de Franchise ni Branch")
    void defensiveCopyOnConstruction_MutatingSourceListAfterBuild_ShouldNotAffectDomainModel() {
        Product p1 = Product.builder().id("p1").name("Cafe").stock(10).build();
        List<Product> mutableProducts = new ArrayList<>();
        mutableProducts.add(p1);

        Branch branch = Branch.builder().id("b1").name("Sucursal 1").products(mutableProducts).build();
        mutableProducts.add(Product.builder().id("p2").name("Intruso").stock(99).build());
        mutableProducts.clear();

        assertThat(branch.getProducts()).hasSize(1);
        assertThat(branch.getProducts().getFirst().getId()).isEqualTo("p1");

        List<Branch> mutableBranches = new ArrayList<>();
        mutableBranches.add(branch);

        Franchise franchise = Franchise.builder().id("f1").name("Franquicia 1").version(0L).branches(mutableBranches).build();
        mutableBranches.add(Branch.builder().id("b2").name("Sucursal Intrusa").build());
        mutableBranches.clear();

        assertThat(franchise.getBranches()).hasSize(1);
        assertThat(franchise.getBranches().getFirst().getId()).isEqualTo("b1");
    }

    @Test
    @DisplayName("Listas null o ausentes se normalizan a lista vacía inmodificable, y elementos null se conservan sin lanzar NullPointerException")
    void nullListAndNullElementsNormalization_ShouldReturnEmptyUnmodifiableListAndPermitNullElementsWithoutThrowing() {
        Franchise franchiseWithNullBranches = Franchise.builder().id("f1").name("F1").branches(null).build();
        Franchise franchiseWithDefaultBranches = Franchise.builder().id("f2").name("F2").build();
        Branch branchWithNullProducts = Branch.builder().id("b1").name("B1").products(null).build();
        Branch branchWithDefaultProducts = Branch.builder().id("b2").name("B2").build();

        assertThat(franchiseWithNullBranches.getBranches()).isNotNull().isEmpty();
        assertThat(franchiseWithDefaultBranches.getBranches()).isNotNull().isEmpty();
        assertThat(branchWithNullProducts.getProducts()).isNotNull().isEmpty();
        assertThat(branchWithDefaultProducts.getProducts()).isNotNull().isEmpty();

        assertThatThrownBy(() -> franchiseWithNullBranches.getBranches().add(branchWithDefaultProducts))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> branchWithNullProducts.getProducts().add(Product.builder().name("P").build()))
                .isInstanceOf(UnsupportedOperationException.class);

        Product validProduct = Product.builder().id("p1").name("Valido").stock(5).build();
        Branch branchWithNullElement = Branch.builder()
                .id("b3")
                .name("Sucursal Con Null")
                .products(Arrays.asList(validProduct, null))
                .build();
        Franchise franchiseWithNullElement = Franchise.builder()
                .id("f3")
                .name("Franquicia Con Null")
                .branches(Arrays.asList(branchWithNullElement, null))
                .build();

        assertThat(branchWithNullElement.getProducts()).containsExactly(validProduct, null);
        assertThat(franchiseWithNullElement.getBranches()).containsExactly(branchWithNullElement, null);
        assertThatThrownBy(() -> franchiseWithNullElement.getBranches().add(null))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("Reflexión: Franchise, Branch, Product y BranchTopProduct tienen solo campos de instancia final y ningún método setter")
    void domainClasses_ShouldHaveOnlyFinalInstanceFieldsAndNoSetterMethods() {
        List<Class<?>> domainClasses = List.of(Franchise.class, Branch.class, Product.class, BranchTopProduct.class);

        for (Class<?> clazz : domainClasses) {
            for (Field field : clazz.getDeclaredFields()) {
                if (!Modifier.isStatic(field.getModifiers())) {
                    assertThat(Modifier.isFinal(field.getModifiers()))
                            .as("El campo %s.%s debe ser final", clazz.getSimpleName(), field.getName())
                            .isTrue();
                }
            }

            boolean hasSetters = Arrays.stream(clazz.getMethods())
                    .anyMatch(method -> method.getName().startsWith("set"));
            assertThat(hasSetters)
                    .as("La clase %s no debe exponer métodos setter", clazz.getSimpleName())
                    .isFalse();
        }
    }
}
