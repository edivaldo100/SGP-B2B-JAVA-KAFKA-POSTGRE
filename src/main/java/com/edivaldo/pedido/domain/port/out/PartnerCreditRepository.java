package com.edivaldo.pedido.domain.port.out;

import com.edivaldo.pedido.domain.model.PartnerCredit;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

public interface PartnerCreditRepository {

    Optional<PartnerCredit> findByPartnerId(UUID partnerId);

    PartnerCredit save(PartnerCredit partnerCredit);

    // Debito atomico e condicional: so aplica se availableCredit >= amount.
    // Retorna false se o saldo for insuficiente ou o parceiro nao existir — sem lock.
    boolean debit(UUID partnerId, BigDecimal amount);

    void release(UUID partnerId, BigDecimal amount);
}
