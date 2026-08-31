package com.edivaldo.pedido.infrastructure.persistence.adapter;

import com.edivaldo.pedido.domain.model.CreditTransaction;
import com.edivaldo.pedido.domain.port.out.CreditTransactionRepository;
import com.edivaldo.pedido.infrastructure.persistence.entity.CreditTransactionJpaEntity;
import com.edivaldo.pedido.infrastructure.persistence.jpa.CreditTransactionJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class CreditTransactionRepositoryAdapter implements CreditTransactionRepository {

    private final CreditTransactionJpaRepository jpaRepository;

    @Override
    public void save(CreditTransaction transaction) {
        CreditTransactionJpaEntity entity = new CreditTransactionJpaEntity();
        entity.setId(transaction.getId());
        entity.setPartnerId(transaction.getPartnerId());
        entity.setOrderId(transaction.getOrderId());
        entity.setType(transaction.getType());
        entity.setAmount(transaction.getAmount());
        entity.setCreatedAt(transaction.getCreatedAt());
        jpaRepository.save(entity);
    }

    @Override
    public Page<CreditTransaction> findByPartnerId(UUID partnerId, Pageable pageable) {
        return jpaRepository.findByPartnerId(partnerId, pageable).map(this::toDomain);
    }

    private CreditTransaction toDomain(CreditTransactionJpaEntity e) {
        return CreditTransaction.reconstitute(
                e.getId(), e.getPartnerId(), e.getOrderId(),
                e.getType(), e.getAmount(), e.getCreatedAt());
    }
}
