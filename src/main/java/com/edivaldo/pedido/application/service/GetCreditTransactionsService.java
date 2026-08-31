package com.edivaldo.pedido.application.service;

import com.edivaldo.pedido.application.dto.CreditTransactionResponse;
import com.edivaldo.pedido.domain.port.in.GetCreditTransactionsUseCase;
import com.edivaldo.pedido.domain.port.out.CreditTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GetCreditTransactionsService implements GetCreditTransactionsUseCase {

    private final CreditTransactionRepository creditTransactionRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<CreditTransactionResponse> execute(UUID partnerUuid, int page, int size) {
        return creditTransactionRepository
                .findByPartnerId(partnerUuid, PageRequest.of(page, size, Sort.by("createdAt").descending()))
                .map(t -> new CreditTransactionResponse(
                        t.getId(),
                        t.getPartnerId(),
                        t.getOrderId(),
                        t.getType().name(),
                        t.getAmount(),
                        t.getCreatedAt()));
    }
}
