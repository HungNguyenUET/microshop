package com.microshop.payment;

import com.microshop.events.OrderEvent;
import com.microshop.events.PaymentEvent;
import com.microshop.events.Topics;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/**
 * Lang nghe topic order-events. Chi quan tam PaymentRequested, bo qua cac event khac.
 * Xu ly xong (transaction DB da commit trong WalletService.pay) moi gui ket qua len payment-events.
 */
@Component
public class OrderEventListener {

    private final WalletService wallet;
    private final KafkaTemplate<String, String> kafka;
    private final JsonMapper json;

    public OrderEventListener(WalletService wallet, KafkaTemplate<String, String> kafka, JsonMapper json) {
        this.wallet = wallet;
        this.kafka = kafka;
        this.json = json;
    }

    @KafkaListener(topics = Topics.ORDER_EVENTS)
    public void onOrderEvent(String message) {
        OrderEvent event = json.readValue(message, OrderEvent.class);
        switch (event) {
            case OrderEvent.PaymentRequested request -> wallet.pay(request).ifPresent(this::send);
            case OrderEvent.OrderPlaced _, OrderEvent.OrderPaid _, OrderEvent.OrderPaymentFailed _ -> {
                // khong lien quan toi payment
            }
        }
    }

    private void send(PaymentEvent result) {
        kafka.send(Topics.PAYMENT_EVENTS, result.orderId().toString(), json.writeValueAsString(result)).join();
    }
}
