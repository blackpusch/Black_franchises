package com.frank.franchises.infrastructure.adapter.out.mongodb.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "franchises", collation = "{ 'locale': 'en', 'strength': 2 }")
public class FranchiseDocument {

    @Id
    private String id;

    @Indexed(name = "uk_franchise_name_ci", unique = true, collation = "{ 'locale': 'en', 'strength': 2 }")
    private String name;

    @Version
    private Long version;

    @Builder.Default
    private List<BranchDocument> branches = new ArrayList<>();
}
