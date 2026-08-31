package com.edivaldo.pedido.application.service;

import com.edivaldo.pedido.application.dto.OrderResponse;
import com.edivaldo.pedido.domain.exception.OrderNotFoundException;
import com.edivaldo.pedido.domain.model.OrderStatus;
import com.edivaldo.pedido.domain.port.in.FindOrderUseCase;
import com.edivaldo.pedido.domain.port.out.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FindOrderService implements FindOrderUseCase {

    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;

    @Override
    @Transactional(readOnly = true)
    public OrderResponse findById(UUID id) {
        return orderRepository.findById(id)
                .map(orderMapper::toResponse)
                .orElseThrow(() -> new OrderNotFoundException(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> findByPartnerId(UUID partnerId) {
        return orderRepository.findByPartnerId(partnerId).stream()
                .map(orderMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> findByStatus(OrderStatus status) {
        return orderRepository.findByStatus(status).stream()
                .map(orderMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> findByPeriod(LocalDateTime start, LocalDateTime end) {
        return orderRepository.findByCreatedAtBetween(start, end).stream()
                .map(orderMapper::toResponse)
                .toList();
    }
}
