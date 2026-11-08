package com.microshop.order.order;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * <pre>
 * POST /api/orders                  dat hang tu gio -> 201 Created + Location
 * GET  /api/orders                  don cua toi
 * GET  /api/orders/{id}             chi tiet (dung de polling trang thai thanh toan)
 * POST /api/orders/{id}/payment     thanh toan -> 202 Accepted (xu ly bat dong bo qua Kafka)
 * GET  /api/admin/orders            (ADMIN) tat ca don
 * </pre>
 */
@RestController
public class OrderController {

    public record OrderResponse(UUID id, String status, BigDecimal totalAmount, String failureReason,
                                List<Item> items, Instant createdAt, Instant updatedAt, String username) {
        public record Item(UUID productId, String productName, BigDecimal unitPrice, int quantity) {
        }

        static OrderResponse from(Order o) {
            return new OrderResponse(o.getId(), o.getStatus().name(), o.getTotalAmount(), o.getFailureReason(),
                    o.getItems().stream().map(i -> new Item(i.getProductId(), i.getProductName(), i.getUnitPrice(), i.getQuantity())).toList(),
                    o.getCreatedAt(), o.getUpdatedAt(), o.getUsername());
        }
    }

    private final OrderService service;
    private final OrderRepository orders;

    public OrderController(OrderService service, OrderRepository orders) {
        this.service = service;
        this.orders = orders;
    }

    @PostMapping("/api/orders")
    public ResponseEntity<OrderResponse> checkout(@AuthenticationPrincipal Jwt jwt) {
        Order order = service.checkout(jwt.getSubject(), jwt.getClaimAsString("preferred_username"));
        return ResponseEntity.created(URI.create("/api/orders/" + order.getId())).body(OrderResponse.from(order));
    }

    @GetMapping("/api/orders")
    public List<OrderResponse> mine(@AuthenticationPrincipal Jwt jwt) {
        return orders.findTop50ByUserIdOrderByCreatedAtDesc(jwt.getSubject()).stream().map(OrderResponse::from).toList();
    }

    @GetMapping("/api/orders/{id}")
    public OrderResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        // Chi tim trong don CUA MINH: don cua nguoi khac -> 404 (khong lo la don do ton tai)
        return orders.findByIdAndUserId(id, jwt.getSubject()).map(OrderResponse::from)
                .orElseThrow(() -> new com.microshop.order.OrderExceptions.NotFound("Order " + id + " not found"));
    }

    @PostMapping("/api/orders/{id}/payment")
    public ResponseEntity<OrderResponse> pay(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return ResponseEntity.accepted().body(OrderResponse.from(service.requestPayment(id, jwt.getSubject())));
    }

    @GetMapping("/api/admin/orders")
    public List<OrderResponse> all() {
        return orders.findTop100ByOrderByCreatedAtDesc().stream().map(OrderResponse::from).toList();
    }
}
