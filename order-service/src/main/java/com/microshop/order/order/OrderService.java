package com.microshop.order.order;

import com.microshop.events.OrderEvent;
import com.microshop.order.OrderExceptions;
import com.microshop.order.cart.CartItem;
import com.microshop.order.cart.CartService;
import com.microshop.order.client.ProductClient.ProductInfo;
import com.microshop.order.messaging.OrderEventPublisher;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orders;
    private final CartService cart;
    private final OrderEventPublisher events;
    /** Custom metric -> /actuator/prometheus: microshop_orders_placed_total (xem dashboard Grafana). */
    private final Counter ordersPlaced;

    public OrderService(OrderRepository orders, CartService cart, OrderEventPublisher events, MeterRegistry metrics) {
        this.orders = orders;
        this.cart = cart;
        this.events = events;
        this.ordersPlaced = Counter.builder("microshop.orders.placed").description("So don hang da dat").register(metrics);
    }

    /** Tao don tu gio hang. Gia lay MOI NHAT tu product-service (REST) roi "chup" lai vao order_item. */
    @Transactional
    public Order checkout(String userId, String username) {
        List<CartItem> items = cart.rawItems(userId);
        if (items.isEmpty()) {
            throw new OrderExceptions.InvalidRequest("Gio hang dang trong");
        }
        Map<UUID, ProductInfo> products = cart.loadProducts(items);

        Order order = new Order(userId, username);
        for (CartItem item : items) {
            ProductInfo p = products.get(item.getProductId());
            if (p == null || !p.active()) {
                throw new OrderExceptions.InvalidRequest("San pham " + item.getProductId() + " khong con ban");
            }
            order.addItem(p.id(), p.name(), p.price(), item.getQuantity());
        }
        orders.save(order);
        cart.clear(userId);
        ordersPlaced.increment();

        log.info("Order {} placed by {} total={}", order.getId(), username, order.getTotalAmount());
        events.publish(new OrderEvent.OrderPlaced(UUID.randomUUID(), order.getId(), userId, username,
                order.getTotalAmount(), order.getItems().size(), Instant.now()));
        return order;
    }

    /** Gui yeu cau thanh toan qua Kafka. Ket qua tra ve BAT DONG BO (client polling GET /api/orders/{id}). */
    @Transactional
    public Order requestPayment(UUID orderId, String userId) {
        Order order = orders.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new OrderExceptions.NotFound("Order " + orderId + " not found"));
        if (!order.moveTo(OrderStatus.PAYMENT_PROCESSING, null)) {
            throw new OrderExceptions.Conflict("Order dang o trang thai " + order.getStatus() + ", khong the thanh toan");
        }
        log.info("Order {} request payment {}", orderId, order.getTotalAmount());
        events.publish(new OrderEvent.PaymentRequested(UUID.randomUUID(), order.getId(), userId,
                order.getTotalAmount(), Instant.now()));
        return order;
    }

    @Transactional
    public void markPaid(UUID orderId, BigDecimal amount) {
        orders.findById(orderId).ifPresent(order -> {
            if (order.moveTo(OrderStatus.PAID, null)) {
                log.info("Order {} PAID", orderId);
                events.publish(new OrderEvent.OrderPaid(UUID.randomUUID(), orderId, order.getUserId(),
                        order.getUsername(), amount, Instant.now()));
            } else {
                log.warn("Ignore PaymentSucceeded for order {} in status {}", orderId, order.getStatus());
            }
        });
    }

    @Transactional
    public void markPaymentFailed(UUID orderId, String reason) {
        orders.findById(orderId).ifPresent(order -> {
            if (order.moveTo(OrderStatus.PAYMENT_FAILED, reason)) {
                log.info("Order {} PAYMENT_FAILED: {}", orderId, reason);
                events.publish(new OrderEvent.OrderPaymentFailed(UUID.randomUUID(), orderId, order.getUserId(),
                        order.getUsername(), reason, Instant.now()));
            } else {
                log.warn("Ignore PaymentFailed for order {} in status {}", orderId, order.getStatus());
            }
        });
    }
}
