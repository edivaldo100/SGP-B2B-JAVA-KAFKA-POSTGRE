package com.edivaldo.pedido.infrastructure.persistence.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "partners")
public class PartnerJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "partner_uuid", nullable = false, unique = true)
    private UUID partnerUuid;

    @Column(nullable = false)
    private String name;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public PartnerJpaEntity() {}

    public PartnerJpaEntity(UUID partnerUuid, String name, LocalDateTime createdAt) {
        this.partnerUuid = partnerUuid;
        this.name = name;
        this.createdAt = createdAt;
    }

    public Integer getId()               { return id; }
    public UUID getPartnerUuid()         { return partnerUuid; }
    public String getName()              { return name; }
    public LocalDateTime getCreatedAt()  { return createdAt; }
}
