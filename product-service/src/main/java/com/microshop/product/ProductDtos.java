package com.microshop.product;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/** DTO (record) tach khoi entity: doi cau truc DB khong lam vo API contract. */
public final class ProductDtos {

    private ProductDtos() {
    }

    public record ProductResponse(UUID id, String sku, String name, String description, BigDecimal price,
                                  String category, boolean active) {
        static ProductResponse from(Product p) {
            return new ProductResponse(p.getId(), p.getSku(), p.getName(), p.getDescription(), p.getPrice(),
                    p.getCategory(), p.isActive());
        }
    }

    public record ProductRequest(
            @NotBlank @Size(max = 50) String sku,
            @NotBlank @Size(max = 200) String name,
            @Size(max = 1000) String description,
            @NotNull @DecimalMin("0") BigDecimal price,
            @NotBlank @Size(max = 50) String category) {
    }

    public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {
    }
}
