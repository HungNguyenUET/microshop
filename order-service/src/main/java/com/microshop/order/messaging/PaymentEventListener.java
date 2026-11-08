package com.microshop.order.messaging;

import com.microshop.events.PaymentEvent;
import com.microshop.events.Topics;
import com.microshop.order.order.OrderService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/** Nhan ket qua thanh toan tu payment-service. */
@Component
public class PaymentEventListener {

    private final OrderService orders;
    private final JsonMapper json;

    public PaymentEventListener(OrderService orders, JsonMapper json) {
        this.orders = orders;
        this.json = json;
    }

    @KafkaListener(topics = Topics.PAYMENT_EVENTS)
    public void onPaymentEvent(String message) {
        PaymentEvent event = json.readValue(message, PaymentEvent.class);
        // sealed interface -> switch phai xu ly du moi loai event, thieu la compile loi
        switch (event) {
            case PaymentEvent.PaymentSucceeded e -> orders.markPaid(e.orderId(), e.amount());
            case PaymentEvent.PaymentFailed e -> orders.markPaymentFailed(e.orderId(), e.reason());
        }
    }
}
