package com.edivaldo.pedido.application.service;

import com.edivaldo.pedido.application.dto.PartnerResponse;
import com.edivaldo.pedido.domain.model.Partner;
import com.edivaldo.pedido.domain.port.out.PartnerCreditRepository;
import com.edivaldo.pedido.domain.port.out.PartnerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PartnerService {

    private static final BigDecimal DEFAULT_CREDIT_LIMIT = new BigDecimal("100000000.00");

    private final PartnerRepository partnerRepository;
    private final PartnerCreditRepository partnerCreditRepository;

    @Transactional
    public PartnerResponse create(String name) {
        if (partnerRepository.existsByName(name)) {
            throw new IllegalArgumentException("Parceiro com nome '" + name + "' já existe");
        }
        Partner partner = Partner.create(name);
        Partner saved = partnerRepository.save(partner);

        com.edivaldo.pedido.domain.model.PartnerCredit credit =
                com.edivaldo.pedido.domain.model.PartnerCredit.of(
                        saved.getPartnerUuid(), DEFAULT_CREDIT_LIMIT, DEFAULT_CREDIT_LIMIT);
        partnerCreditRepository.save(credit);

        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<PartnerResponse> findAll() {
        return partnerRepository.findAll().stream().map(this::toResponse).toList();
    }

    private PartnerResponse toResponse(Partner p) {
        return new PartnerResponse(p.getId(), p.getPartnerUuid(), p.getName(), p.getCreatedAt());
    }
}
