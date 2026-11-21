package com.microshop.payment.events;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Cac event do order-service phat ra (topic {@value Topics#ORDER_EVENTS}).
 * <p>
 * <b>Contract</b>: order-service SO HUU. Day la ban sao phia consumer (payment-service).
 * Cac service KHONG dung chung jar: moi service tu giu ban sao contract trong package cua minh,
 * chi can khop dinh dang JSON tren Kafka (ten field + gia tri "type"). Thay doi contract phai tuong thich nguoc
 * (chi them field moi; khong doi ten/xoa field, khong doi gia tri "type").
 * <p>
 * JSON co them field "type" de consumer biet day la event nao, vi du:
 * <pre>{"type":"PaymentRequested","eventId":"...","orderId":"...","userId":"...","amount":120000,...}</pre>
 * {@code sealed}: compiler biet het cac loai event -> switch khong can default.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = OrderEvent.OrderPlaced.class, name = "OrderPlaced"),
        @JsonSubTypes.Type(value = OrderEvent.PaymentRequested.class, name = "PaymentRequested"),
        @JsonSubTypes.Type(value = OrderEvent.OrderPaid.class, name = "OrderPaid"),
        @JsonSubTypes.Type(value = OrderEvent.OrderPaymentFailed.class, name = "OrderPaymentFailed")
})
public sealed interface OrderEvent {

    UUID eventId();

    UUID orderId();

    String userId();

    Instant occurredAt();

    /** Khach vua dat hang thanh cong (chua thanh toan). */
    record OrderPlaced(UUID eventId, UUID orderId, String userId, String username, BigDecimal totalAmount,
                       int itemCount, Instant occurredAt) implements OrderEvent {
    }

    /** Khach bam "Thanh toan" -> nho payment-service tru tien trong vi. */
    record PaymentRequested(UUID eventId, UUID orderId, String userId, BigDecimal amount,
                            Instant occurredAt) implements OrderEvent {
    }

    /** Ket qua cuoi: da thanh toan. */
    record OrderPaid(UUID eventId, UUID orderId, String userId, String username, BigDecimal amount,
                     Instant occurredAt) implements OrderEvent {
    }

    /** Ket qua cuoi: thanh toan that bai (vd: khong du tien). */
    record OrderPaymentFailed(UUID eventId, UUID orderId, String userId, String username, String reason,
                              Instant occurredAt) implements OrderEvent {
    }
}
