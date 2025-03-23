package com.example.catalogservice.service;

import com.example.catalogservice.jpa.CatalogEntity;

import java.util.List;

public interface CatalogService {
    Iterable<CatalogEntity> getAllCatalogs();

    CatalogEntity getCatalogs(String productId);
}
