package com.edivaldo.pedido.domain.port.out;

import com.edivaldo.pedido.domain.model.Partner;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PartnerRepository {

    Partner save(Partner partner);

    Optional<Partner> findById(Integer id);

    Optional<Partner> findByName(String name);

    Optional<Partner> findByPartnerUuid(UUID partnerUuid);

    List<Partner> findAll();

    boolean existsByName(String name);

    void deleteById(Integer id);
}
