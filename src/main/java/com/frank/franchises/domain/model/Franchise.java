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
public class Franchise {

    @Builder.Default
    private final String id = UUID.randomUUID().toString();

    private final String name;

    private final Long version;

    private final List<Branch> branches;

    public Franchise(String id, String name, Long version, List<Branch> branches) {
        this.id = id;
        this.name = name;
        this.version = version;
        this.branches = Collections.unmodifiableList(
                branches == null ? new ArrayList<>() : new ArrayList<>(branches)
        );
    }
}
