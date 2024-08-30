package com.example.orderservice.service;

import com.example.orderservice.dto.OrderDto;
import com.example.orderservice.jpa.OrderEntity;

import java.util.HashMap;
import java.util.List;

public interface OrderService {
    OrderDto createOrder(OrderDto orderDetails);
    OrderDto getOrderByOrderId(String orderId);
    Iterable<OrderEntity> getOrdersByUserId(String userId);
    // 주문된 상품 ID별 총 개수와 총액을 반환하는 메서드
    List<HashMap<String, Object>> getOrderSummary();
}
