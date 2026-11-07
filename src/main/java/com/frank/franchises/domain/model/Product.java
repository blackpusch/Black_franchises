package com.frank.franchises.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Product {

    @Builder.Default
    private String id = UUID.randomUUID().toString();

    private String name;

    @Builder.Default
    private Integer stock = 0;
}
