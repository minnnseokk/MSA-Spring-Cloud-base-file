package com.example.orderservice.service;

import com.example.orderservice.client.CatalogServiceClient;
import com.example.orderservice.dto.OrderDto;
import com.example.orderservice.jpa.OrderEntity;
import com.example.orderservice.jpa.OrderRepository;
import com.example.orderservice.messagequeue.OrderProducer;
import com.example.orderservice.vo.ResponseCatalog;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class OrderServiceImpl implements OrderService {
    OrderRepository orderRepository;

    CatalogServiceClient catalogServiceClient;
    @Autowired
    private OrderProducer orderProducer;

    @Autowired
    public OrderServiceImpl(OrderRepository orderRepository,
                            CatalogServiceClient catalogServiceClient
                            ) {
        this.catalogServiceClient = catalogServiceClient;
        this.orderRepository = orderRepository;
    }

    // 주문내역의 총합을 요약하는 메서드
    @Override
    public List<HashMap<String, Object>> getOrderSummary() {
        List<OrderEntity> orders = (List<OrderEntity>) orderRepository.findAll();
        // 상품 ID로 총 개수와 총액이 담길 map
        HashMap<String, Integer> totalQty = new HashMap<>();
        HashMap<String, Double> totalPrice = new HashMap<>();

        // 총 개수, 총액 연산
        for (OrderEntity order : orders) {
            String productId = order.getProductId(); // 상품 ID
            int qty = order.getQty(); // 주문 수량
            double price = order.getTotalPrice(); // 총 가격

            totalQty.put(productId, totalQty.getOrDefault(productId, 0) + qty);
            totalPrice.put(productId, totalPrice.getOrDefault(productId, 0.0) + price);
        }
        // 상품 id별 저장해주는 리스트 생성 ( 맵이 담긴 리스트다 )
        List<HashMap<String, Object>> summaryList = new ArrayList<>();
        for (String productId : totalQty.keySet()) {
            HashMap<String, Object> productSummary = new HashMap<>();
            productSummary.put("productId", productId);
            productSummary.put("totalQty", totalQty.get(productId));
            productSummary.put("totalPrice", totalPrice.get(productId));
            summaryList.add(productSummary);
        }
        return summaryList;
    }

    // ip 주소를 가져오기 위한 요청
    @Autowired
    private HttpServletRequest request; // HttpServletRequest 주입

    @Override
    public OrderDto createOrder(OrderDto orderDto) {

        // 카탈로그 서비스에서 재고 수량 가져오기
        List<ResponseCatalog> catalogs = catalogServiceClient.getCatalogs(orderDto.getProductId());
        System.out.println(catalogs.getFirst().getStock() + "재고 남음");

        // 재고 수량 확인 (여기서는 첫 번째 결과만 확인)
        if (catalogs.isEmpty() || catalogs.getFirst().getStock() < orderDto.getQty()) {
            log.info("카탈로그 재고 가져온 후 조건문이 정상작동함");
            throw new RuntimeException("재고가 부족합니다. 현재 재고 수량: " +
                    (catalogs.isEmpty() ? 0 : catalogs.getFirst().getStock()));
        } else {
            log.info("조건문이 작동하지 않음");
            orderDto.setOrderId(UUID.randomUUID().toString());
            orderDto.setTotalPrice(orderDto.getQty() * orderDto.getUnitPrice());

            // IP 주소 및 포트 번호를 떼어서 출처 저장
            String ipAddress = request.getRemoteAddr();
            String port = String.valueOf(request.getServerPort());
            orderDto.setOrderIp(ipAddress + ":" + port); // orderIp에 저장
            
            
            ModelMapper mapper = new ModelMapper();
            mapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
            OrderEntity orderEntity = mapper.map(orderDto, OrderEntity.class);

            orderRepository.save(orderEntity);
            // Kafka로 주문 정보 전송
            orderProducer.send("example-order-topic", orderDto); // 주문 정보를 Kafka에 전송

            OrderDto returnValue = mapper.map(orderEntity, OrderDto.class);

            return returnValue;
        }
    }

    @Override
    public OrderDto getOrderByOrderId(String orderId) {
        OrderEntity orderEntity = orderRepository.findByOrderId(orderId);
        OrderDto orderDto = new ModelMapper().map(orderEntity, OrderDto.class);

        return orderDto;
    }

    @Override
    public Iterable<OrderEntity> getOrdersByUserId(String userId) {
        return orderRepository.findByUserId(userId);
    }
}
