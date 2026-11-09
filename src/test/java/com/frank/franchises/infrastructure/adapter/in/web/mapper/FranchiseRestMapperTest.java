package com.frank.franchises.infrastructure.adapter.in.web.mapper;

import com.frank.franchises.domain.model.Branch;
import com.frank.franchises.domain.model.BranchTopProduct;
import com.frank.franchises.domain.model.Franchise;
import com.frank.franchises.domain.model.Product;
import com.frank.franchises.infrastructure.adapter.in.web.dto.BranchResponse;
import com.frank.franchises.infrastructure.adapter.in.web.dto.BranchTopProductResponse;
import com.frank.franchises.infrastructure.adapter.in.web.dto.FranchiseResponse;
import com.frank.franchises.infrastructure.adapter.in.web.dto.ProductResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class FranchiseRestMapperTest {

    @Test
    @DisplayName("toResponse: debe retornar null cuando la entidad de entrada es null en cualquiera de las sobrecargas")
    void toResponse_WhenInputsAreNull_ShouldReturnNull() {
        assertThat(FranchiseRestMapper.toResponse((Franchise) null)).isNull();
        assertThat(FranchiseRestMapper.toResponse((Branch) null)).isNull();
        assertThat(FranchiseRestMapper.toResponse((Product) null)).isNull();
        assertThat(FranchiseRestMapper.toResponse((BranchTopProduct) null)).isNull();
    }

    @Test
    @DisplayName("toResponse: debe normalizar listas null y filtrar elementos null dentro de branches y products")
    void toResponse_WhenListsAreNullOrContainNullElements_ShouldFilterNullsAndReturnSafeLists() {
        Franchise franchiseNullBranches = Franchise.builder()
                .id("f1")
                .name("Franquicia")
                .branches(null)
                .build();

        FranchiseResponse respNullBranches = FranchiseRestMapper.toResponse(franchiseNullBranches);
        assertThat(respNullBranches).isNotNull();
        assertThat(respNullBranches.branches()).isNotNull().isEmpty();

        Branch branchNullProducts = Branch.builder()
                .id("b0")
                .name("Sucursal Sin Productos")
                .products(null)
                .build();
        BranchResponse respNullProducts = FranchiseRestMapper.toResponse(branchNullProducts);
        assertThat(respNullProducts).isNotNull();
        assertThat(respNullProducts.products()).isNotNull().isEmpty();

        Product validProduct = Product.builder().id("p1").name("Cafe").stock(12).build();
        Branch branchWithNullElements = Branch.builder()
                .id("b1")
                .name("Sucursal 1")
                .products(Arrays.asList(validProduct, null))
                .build();

        Franchise franchiseWithNullElements = Franchise.builder()
                .id("f2")
                .name("Franquicia Mixta")
                .version(9L)
                .branches(Arrays.asList(branchWithNullElements, null))
                .build();

        FranchiseResponse response = FranchiseRestMapper.toResponse(franchiseWithNullElements);
        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo("f2");
        assertThat(response.name()).isEqualTo("Franquicia Mixta");
        assertThat(response.branches()).hasSize(1).doesNotContainNull();
        assertThat(response.branches().getFirst().id()).isEqualTo("b1");
        assertThat(response.branches().getFirst().products()).hasSize(1).doesNotContainNull();
        assertThat(response.branches().getFirst().products().getFirst().id()).isEqualTo("p1");
        assertThat(response.branches().getFirst().products().getFirst().stock()).isEqualTo(12);
    }

    @Test
    @DisplayName("toResponse: Product con stock null usa 0 por defecto y BranchTopProduct mapea producto o null")
    void toResponse_ProductWithNullStockAndBranchTopProduct_ShouldDefaultStockToZeroAndMapNestedProduct() {
        Product productNullStock = Product.builder().id("p1").name("Item").stock(null).build();
        ProductResponse productResponse = FranchiseRestMapper.toResponse(productNullStock);
        assertThat(productResponse).isNotNull();
        assertThat(productResponse.stock()).isZero();

        BranchTopProduct topWithProduct = new BranchTopProduct("b1", "Sucursal Norte", productNullStock);
        BranchTopProductResponse topResp = FranchiseRestMapper.toResponse(topWithProduct);
        assertThat(topResp).isNotNull();
        assertThat(topResp.branchId()).isEqualTo("b1");
        assertThat(topResp.branchName()).isEqualTo("Sucursal Norte");
        assertThat(topResp.product()).isNotNull();
        assertThat(topResp.product().id()).isEqualTo("p1");
        assertThat(topResp.product().stock()).isZero();

        BranchTopProduct topWithNullProduct = new BranchTopProduct("b2", "Sucursal Sur", null);
        BranchTopProductResponse topNullResp = FranchiseRestMapper.toResponse(topWithNullProduct);
        assertThat(topNullResp).isNotNull();
        assertThat(topNullResp.branchId()).isEqualTo("b2");
        assertThat(topNullResp.product()).isNull();
    }
}
