package com.frank.franchises.infrastructure.adapter.out.mongodb.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductDocument {

    private String id;
    private String name;

    @Builder.Default
    private Integer stock = 0;
}
