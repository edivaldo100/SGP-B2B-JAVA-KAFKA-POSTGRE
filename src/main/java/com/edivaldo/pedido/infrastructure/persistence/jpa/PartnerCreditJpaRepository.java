package com.edivaldo.pedido.infrastructure.persistence.jpa;

import com.edivaldo.pedido.infrastructure.persistence.entity.PartnerCreditJpaEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface PartnerCreditJpaRepository extends JpaRepository<PartnerCreditJpaEntity, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT pc FROM PartnerCreditJpaEntity pc WHERE pc.partnerId = :partnerId")
    Optional<PartnerCreditJpaEntity> findByPartnerIdForUpdate(UUID partnerId);

    Optional<PartnerCreditJpaEntity> findByPartnerId(UUID partnerId);
}
