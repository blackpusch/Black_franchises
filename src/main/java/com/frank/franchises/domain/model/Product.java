package com.frank.franchises.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.UUID;

@Getter
@ToString
@EqualsAndHashCode
@Builder(toBuilder = true)
@AllArgsConstructor
public class Product {

    @Builder.Default
    private final String id = UUID.randomUUID().toString();

    private final String name;

    @Builder.Default
    private final Integer stock = 0;
}
