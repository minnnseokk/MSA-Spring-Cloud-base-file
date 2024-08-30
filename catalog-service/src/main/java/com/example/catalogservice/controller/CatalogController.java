package com.example.catalogservice.controller;

import com.example.catalogservice.jpa.CatalogEntity;
import com.example.catalogservice.service.CatalogService;
import com.example.catalogservice.vo.ResponseCatalog;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/catalog-service")
@Slf4j
public class CatalogController {
    Environment env;
    CatalogService catalogService;

    @Autowired
    private DiscoveryClient discoveryClient;

    @GetMapping("/{productId}/order-catalog")
    public ResponseEntity<List<ResponseCatalog>> getCatalogs(@PathVariable String productId){
        CatalogEntity orderedCatalog = catalogService.getCatalogs(productId);
        // ResponseCatalog 리스트 생성
        List<ResponseCatalog> result = new ArrayList<>();

        // 필요한 필드를 ResponseCatalog에 매핑하여 리스트에 추가
        if (orderedCatalog != null) {
            ResponseCatalog responseCatalog = new ResponseCatalog();
            responseCatalog.setProductId(orderedCatalog.getProductId());
            responseCatalog.setProductName(orderedCatalog.getProductName());
            responseCatalog.setStock(orderedCatalog.getStock());
            responseCatalog.setUnitPrice(orderedCatalog.getUnitPrice());

            result.add(responseCatalog);
        }
        return ResponseEntity.status(HttpStatus.OK).body(result);
    }

    @Autowired
    public CatalogController(Environment env, CatalogService catalogService) {
        this.env = env;
        this.catalogService = catalogService;
    }

    @GetMapping("/health-check")
    public String status() {
        List<ServiceInstance> serviceList = getApplications();
        for (ServiceInstance instance : serviceList) {
            System.out.println(String.format("instanceId:%s, serviceId:%s, host:%s, scheme:%s, uri:%s",
                    instance.getInstanceId(), instance.getServiceId(), instance.getHost(), instance.getScheme(), instance.getUri()));
        }
        return String.format("It's Working in Catalog Service on LOCAL PORT %s (SERVER PORT %s)",
                env.getProperty("local.server.port"),
                env.getProperty("server.port"));
    }

    @GetMapping("/catalogs")
    public ResponseEntity<List<ResponseCatalog>> getCatalogs() {
        log.info("Called catalog list");
        Iterable<CatalogEntity> catalogList = catalogService.getAllCatalogs();

        List<ResponseCatalog> result = new ArrayList<>();
        catalogList.forEach(v -> {
            result.add(new ModelMapper().map(v, ResponseCatalog.class));
        });

        log.info("ToTal catalog count -> {}", result.size());

        return ResponseEntity.status(HttpStatus.OK).body(result);
    }

    private List<ServiceInstance> getApplications() {

        List<String> services = this.discoveryClient.getServices();
        List<ServiceInstance> instances = new ArrayList<ServiceInstance>();
        services.forEach(serviceName -> {
            this.discoveryClient.getInstances(serviceName).forEach(instance ->{
                instances.add(instance);
            });
        });
        return instances;
    }
}
