package com.edivaldo.pedido.domain.model;

import java.time.LocalDateTime;
import java.util.UUID;

public class Partner {

    private Integer id;
    private UUID partnerUuid;
    private String name;
    private LocalDateTime createdAt;

    private Partner() {}

    public static Partner create(String name) {
        Partner p = new Partner();
        p.partnerUuid = UUID.randomUUID();
        p.name = name;
        p.createdAt = LocalDateTime.now();
        return p;
    }

    public static Partner reconstitute(Integer id, UUID partnerUuid, String name, LocalDateTime createdAt) {
        Partner p = new Partner();
        p.id = id;
        p.partnerUuid = partnerUuid;
        p.name = name;
        p.createdAt = createdAt;
        return p;
    }

    public Integer getId()           { return id; }
    public UUID getPartnerUuid()     { return partnerUuid; }
    public String getName()          { return name; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
