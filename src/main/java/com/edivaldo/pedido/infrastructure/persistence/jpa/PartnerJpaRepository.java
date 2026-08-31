package com.edivaldo.pedido.infrastructure.persistence.jpa;

import com.edivaldo.pedido.infrastructure.persistence.entity.PartnerJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PartnerJpaRepository extends JpaRepository<PartnerJpaEntity, Integer> {

    Optional<PartnerJpaEntity> findByName(String name);

    Optional<PartnerJpaEntity> findByPartnerUuid(UUID partnerUuid);

    boolean existsByName(String name);
}
