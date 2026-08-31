package com.edivaldo.pedido.application.service;

import com.edivaldo.pedido.application.dto.OrderResponse;
import com.edivaldo.pedido.domain.exception.OrderNotFoundException;
import com.edivaldo.pedido.domain.model.Order;
import com.edivaldo.pedido.domain.model.OrderStatus;
import com.edivaldo.pedido.domain.model.Partner;
import com.edivaldo.pedido.domain.port.in.FindOrderUseCase;
import com.edivaldo.pedido.domain.port.out.OrderRepository;
import com.edivaldo.pedido.domain.port.out.PartnerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FindOrderService implements FindOrderUseCase {

    private final OrderRepository orderRepository;
    private final PartnerRepository partnerRepository;
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
    public List<OrderResponse> findAll() {
        List<Order> orders = orderRepository.findByCreatedAtBetween(
                LocalDateTime.now().minusYears(10), LocalDateTime.now().plusDays(1));
        return mapWithPartners(orders);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> findByPartnerId(UUID partnerId) {
        return mapWithPartners(orderRepository.findByPartnerId(partnerId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> findByStatus(OrderStatus status) {
        return mapWithPartners(orderRepository.findByStatus(status));
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> findByPeriod(LocalDateTime start, LocalDateTime end) {
        return mapWithPartners(orderRepository.findByCreatedAtBetween(start, end));
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> search(Integer partnerSequentialId, String partnerName, OrderStatus status) {
        Optional<UUID> partnerUuid = resolvePartnerUuid(partnerSequentialId, partnerName);

        List<Order> orders;
        if (partnerUuid.isPresent() && status != null) {
            orders = orderRepository.findByPartnerIdAndStatus(partnerUuid.get(), status);
        } else if (partnerUuid.isPresent()) {
            orders = orderRepository.findByPartnerId(partnerUuid.get());
        } else if (status != null) {
            orders = orderRepository.findByStatus(status);
        } else {
            orders = orderRepository.findByCreatedAtBetween(
                    LocalDateTime.now().minusYears(10), LocalDateTime.now().plusDays(1));
        }

        return mapWithPartners(orders);
    }

    private List<OrderResponse> mapWithPartners(List<Order> orders) {
        Map<UUID, Partner> partnerMap = partnerRepository.findAll().stream()
                .collect(Collectors.toMap(Partner::getPartnerUuid, p -> p));
        return orders.stream()
                .map(o -> orderMapper.toResponse(o, partnerMap))
                .toList();
    }

    private Optional<UUID> resolvePartnerUuid(Integer partnerSequentialId, String partnerName) {
        if (partnerSequentialId != null) {
            return partnerRepository.findById(partnerSequentialId).map(Partner::getPartnerUuid);
        }
        if (partnerName != null && !partnerName.isBlank()) {
            return partnerRepository.findByName(partnerName).map(Partner::getPartnerUuid);
        }
        return Optional.empty();
    }
}
