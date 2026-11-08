package com.microshop.product;

import com.microshop.product.ProductDtos.PageResponse;
import com.microshop.product.ProductDtos.ProductRequest;
import com.microshop.product.ProductDtos.ProductResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.UUID;

/**
 * RESTful resource "products":
 * <pre>
 * GET    /api/products?q=&page=0&size=20   200            (public)
 * GET    /api/products/{id}                200 | 404      (public)
 * POST   /api/products                     201 + Location (ADMIN)
 * PUT    /api/products/{id}                200 | 404      (ADMIN)
 * DELETE /api/products/{id}                204 | 404      (ADMIN, xoa mem)
 * </pre>
 * Danh tu so nhieu, dung HTTP method cho hanh dong, dung status code cho ket qua.
 */
@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductRepository repository;

    public ProductController(ProductRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public PageResponse<ProductResponse> search(@RequestParam(defaultValue = "") String q,
                                                @RequestParam(defaultValue = "0") @Min(0) int page,
                                                @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        Page<Product> result = repository.findByActiveTrueAndNameContainingIgnoreCase(q,
                PageRequest.of(page, size, Sort.by("name")));
        return new PageResponse<>(result.map(ProductResponse::from).getContent(),
                page, size, result.getTotalElements(), result.getTotalPages());
    }

    @GetMapping("/{id}")
    public ProductResponse get(@PathVariable UUID id) {
        return ProductResponse.from(find(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public ResponseEntity<ProductResponse> create(@Valid @RequestBody ProductRequest req) {
        if (repository.existsBySku(req.sku())) {
            throw new ProductExceptions.DuplicateSku(req.sku());
        }
        Product saved = repository.save(new Product(req.sku(), req.name(), req.description(), req.price(), req.category()));
        var location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(saved.getId()).toUri();
        return ResponseEntity.created(location).body(ProductResponse.from(saved));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public ProductResponse update(@PathVariable UUID id, @Valid @RequestBody ProductRequest req) {
        Product product = find(id);
        product.update(req.name(), req.description(), req.price(), req.category());
        return ProductResponse.from(product);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        find(id).deactivate();
        return ResponseEntity.noContent().build();
    }

    private Product find(UUID id) {
        return repository.findById(id).orElseThrow(() -> new ProductExceptions.NotFound(id));
    }
}
