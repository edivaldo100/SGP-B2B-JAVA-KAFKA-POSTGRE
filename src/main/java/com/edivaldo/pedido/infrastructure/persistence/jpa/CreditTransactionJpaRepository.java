package com.edivaldo.pedido.infrastructure.persistence.jpa;

import com.edivaldo.pedido.infrastructure.persistence.entity.CreditTransactionJpaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CreditTransactionJpaRepository extends JpaRepository<CreditTransactionJpaEntity, UUID> {

    Page<CreditTransactionJpaEntity> findByPartnerId(UUID partnerId, Pageable pageable);
}
