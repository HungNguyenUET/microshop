package com.microshop.product;

import com.microshop.product.ProductDtos.ProductResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * API NOI BO cho service khac (order-service) goi truc tiep, KHONG di qua gateway.
 * Chi chap nhan token client_credentials (role SERVICE).
 */
@RestController
@RequestMapping("/internal/products")
public class InternalProductController {

    private final ProductRepository repository;

    public InternalProductController(ProductRepository repository) {
        this.repository = repository;
    }

    /** Lay nhieu san pham trong 1 request (tranh goi N lan qua mang). */
    @GetMapping
    @PreAuthorize("hasRole('SERVICE')")
    public List<ProductResponse> findByIds(@RequestParam List<UUID> ids) {
        return repository.findAllById(ids).stream().map(ProductResponse::from).toList();
    }
}
