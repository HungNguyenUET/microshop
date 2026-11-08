package com.microshop.order.client;

import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Goi product-service bang REST. Chi khai bao interface, Spring tu sinh code goi HTTP
 * (giong Feign nhung co san trong Spring Framework). Xem ProductClientConfig.
 */
@HttpExchange("/internal/products")
public interface ProductClient {

    @GetExchange
    List<ProductInfo> findByIds(@RequestParam List<UUID> ids);

    record ProductInfo(UUID id, String sku, String name, BigDecimal price, String category, boolean active) {
    }
}
