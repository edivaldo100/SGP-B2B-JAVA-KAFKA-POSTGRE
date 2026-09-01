package com.edivaldo.pedido.infrastructure.persistence.jpa;

import com.edivaldo.pedido.infrastructure.persistence.entity.PartnerCreditJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

public interface PartnerCreditJpaRepository extends JpaRepository<PartnerCreditJpaEntity, UUID> {

    Optional<PartnerCreditJpaEntity> findByPartnerId(UUID partnerId);

    // UPDATE atômico condicional — o próprio WHERE garante que nunca debita
    // além do saldo disponível, sem precisar de SELECT ... FOR UPDATE.
    // Retorna 0 linhas afetadas quando o saldo é insuficiente (ou o parceiro não existe).
    @Modifying
    @Query("UPDATE PartnerCreditJpaEntity pc SET pc.availableCredit = pc.availableCredit - :amount " +
           "WHERE pc.partnerId = :partnerId AND pc.availableCredit >= :amount")
    int debitIfSufficient(@Param("partnerId") UUID partnerId, @Param("amount") BigDecimal amount);

    @Modifying
    @Query("UPDATE PartnerCreditJpaEntity pc SET pc.availableCredit = pc.availableCredit + :amount " +
           "WHERE pc.partnerId = :partnerId")
    int release(@Param("partnerId") UUID partnerId, @Param("amount") BigDecimal amount);
}
