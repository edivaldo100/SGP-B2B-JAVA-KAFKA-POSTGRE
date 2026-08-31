package com.edivaldo.pedido.infrastructure.persistence.adapter;

import com.edivaldo.pedido.domain.model.PartnerCredit;
import com.edivaldo.pedido.domain.port.out.PartnerCreditRepository;
import com.edivaldo.pedido.infrastructure.persistence.entity.PartnerCreditJpaEntity;
import com.edivaldo.pedido.infrastructure.persistence.jpa.PartnerCreditJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class PartnerCreditRepositoryAdapter implements PartnerCreditRepository {

    private final PartnerCreditJpaRepository jpaRepository;

    @Override
    public Optional<PartnerCredit> findByPartnerIdForUpdate(UUID partnerId) {
        return jpaRepository.findByPartnerIdForUpdate(partnerId).map(this::toDomain);
    }

    @Override
    public PartnerCredit save(PartnerCredit credit) {
        // busca a entidade existente para preservar o id e version (necessario para @Version)
        PartnerCreditJpaEntity entity = jpaRepository.findByPartnerId(credit.getPartnerId())
                .orElseGet(() -> {
                    PartnerCreditJpaEntity newEntity = new PartnerCreditJpaEntity();
                    newEntity.setId(UUID.randomUUID());
                    newEntity.setPartnerId(credit.getPartnerId());
                    return newEntity;
                });

        entity.setCreditLimit(credit.getCreditLimit());
        entity.setAvailableCredit(credit.getAvailableCredit());
        jpaRepository.save(entity);
        return credit;
    }

    private PartnerCredit toDomain(PartnerCreditJpaEntity entity) {
        return PartnerCredit.of(entity.getPartnerId(), entity.getCreditLimit(), entity.getAvailableCredit());
    }
}
