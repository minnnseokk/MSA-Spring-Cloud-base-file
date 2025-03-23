package com.example.orderservice.client;

import com.example.orderservice.vo.ResponseCatalog;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.cloud.openfeign.FeignClient;

import java.util.List;

@FeignClient(name="CATALOG-SERVICE")
public interface CatalogServiceClient {
    // 재고 수량 가져오기 위한 컨트롤러
    @GetMapping("/catalog-service/{productId}/order-catalog")
    List<ResponseCatalog> getCatalogs(@PathVariable String productId);
}