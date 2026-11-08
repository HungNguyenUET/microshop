package com.microshop.order.messaging;

import com.microshop.events.OrderEvent;
import com.microshop.events.Topics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import tools.jackson.databind.json.JsonMapper;

/**
 * Gui event len Kafka SAU KHI transaction DB commit thanh cong.
 * <ul>
 *   <li>DB rollback -> event khong bao gio duoc gui (khong co "don ma" tren Kafka).</li>
 *   <li>Han che: neu app chet ngay sau commit, truoc khi gui -> mat event.
 *       Giai phap chuan: Transactional Outbox pattern (ngoai pham vi demo nay).</li>
 * </ul>
 */
@Component
public class OrderEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(OrderEventPublisher.class);

    private final ApplicationEventPublisher springEvents;
    private final KafkaTemplate<String, String> kafka;
    private final JsonMapper json;

    public OrderEventPublisher(ApplicationEventPublisher springEvents, KafkaTemplate<String, String> kafka, JsonMapper json) {
        this.springEvents = springEvents;
        this.kafka = kafka;
        this.json = json;
    }

    /** Goi trong transaction nghiep vu: chi "ghi nho" event, chua gui. */
    public void publish(OrderEvent event) {
        springEvents.publishEvent(event);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    void sendAfterCommit(OrderEvent event) {
        // Key = orderId: moi event cua cung 1 order vao cung partition -> giu dung thu tu
        kafka.send(Topics.ORDER_EVENTS, event.orderId().toString(), json.writeValueAsString(event))
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish {} for order {}", event.getClass().getSimpleName(), event.orderId(), ex);
                    } else {
                        log.info("Published {} for order {} -> {}-{}@{}", event.getClass().getSimpleName(), event.orderId(),
                                result.getRecordMetadata().topic(), result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset());
                    }
                });
    }
}
