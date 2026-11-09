package com.frank.franchises.domain.model;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Getter
@ToString
@EqualsAndHashCode
@Builder(toBuilder = true)
public class Branch {

    @Builder.Default
    private final String id = UUID.randomUUID().toString();

    private final String name;

    private final List<Product> products;

    public Branch(String id, String name, List<Product> products) {
        this.id = id;
        this.name = name;
        this.products = Collections.unmodifiableList(
                products == null ? new ArrayList<>() : new ArrayList<>(products)
        );
    }
}
