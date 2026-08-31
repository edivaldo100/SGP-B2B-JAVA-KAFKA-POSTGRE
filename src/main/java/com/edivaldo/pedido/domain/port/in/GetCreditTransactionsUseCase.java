package com.edivaldo.pedido.domain.port.in;

import com.edivaldo.pedido.application.dto.CreditTransactionResponse;
import org.springframework.data.domain.Page;

import java.util.UUID;

public interface GetCreditTransactionsUseCase {

    Page<CreditTransactionResponse> execute(UUID partnerUuid, int page, int size);
}
