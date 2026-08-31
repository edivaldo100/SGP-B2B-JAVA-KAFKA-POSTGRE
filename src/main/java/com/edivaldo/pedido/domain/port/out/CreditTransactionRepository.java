package com.edivaldo.pedido.domain.port.out;

import com.edivaldo.pedido.domain.model.CreditTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface CreditTransactionRepository {

    void save(CreditTransaction transaction);

    Page<CreditTransaction> findByPartnerId(UUID partnerId, Pageable pageable);
}
