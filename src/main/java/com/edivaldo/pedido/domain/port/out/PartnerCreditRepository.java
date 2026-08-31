package com.edivaldo.pedido.domain.port.out;

import com.edivaldo.pedido.domain.model.PartnerCredit;

import java.util.Optional;
import java.util.UUID;

public interface PartnerCreditRepository {

    Optional<PartnerCredit> findByPartnerId(UUID partnerId);

    // Busca com SELECT FOR UPDATE — deve ser chamado dentro de @Transactional
    Optional<PartnerCredit> findByPartnerIdForUpdate(UUID partnerId);

    PartnerCredit save(PartnerCredit partnerCredit);
}
