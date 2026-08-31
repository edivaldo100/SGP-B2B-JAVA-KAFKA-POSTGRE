package com.edivaldo.pedido.infrastructure.persistence.adapter;

import com.edivaldo.pedido.domain.model.Partner;
import com.edivaldo.pedido.domain.port.out.PartnerRepository;
import com.edivaldo.pedido.infrastructure.persistence.entity.PartnerJpaEntity;
import com.edivaldo.pedido.infrastructure.persistence.jpa.PartnerJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class PartnerRepositoryAdapter implements PartnerRepository {

    private final PartnerJpaRepository jpaRepository;

    @Override
    public Partner save(Partner partner) {
        PartnerJpaEntity entity = new PartnerJpaEntity(
                partner.getPartnerUuid(),
                partner.getName(),
                partner.getCreatedAt()
        );
        return toDomain(jpaRepository.save(entity));
    }

    @Override
    public Optional<Partner> findById(Integer id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<Partner> findByName(String name) {
        return jpaRepository.findByName(name).map(this::toDomain);
    }

    @Override
    public Optional<Partner> findByPartnerUuid(UUID partnerUuid) {
        return jpaRepository.findByPartnerUuid(partnerUuid).map(this::toDomain);
    }

    @Override
    public List<Partner> findAll() {
        return jpaRepository.findAll().stream().map(this::toDomain).toList();
    }

    @Override
    public boolean existsByName(String name) {
        return jpaRepository.existsByName(name);
    }

    @Override
    public void deleteById(Integer id) {
        jpaRepository.deleteById(id);
    }

    private Partner toDomain(PartnerJpaEntity e) {
        return Partner.reconstitute(e.getId(), e.getPartnerUuid(), e.getName(), e.getCreatedAt());
    }
}
