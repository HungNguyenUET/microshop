package com.microshop.order.events;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Contract test: co dinh dinh dang JSON tren Kafka. Khong co jar event dung chung,
 * nen day la "hang rao" dam bao ban sao contract cua order-service van khop voi cac service khac.
 */
class EventContractTest {

    private final JsonMapper mapper = JsonMapper.builder().build();

    @Test
    void producesOrderEventWithTypeNameAndRoundTrips() {
        OrderEvent event = new OrderEvent.PaymentRequested(UUID.randomUUID(), UUID.randomUUID(), "u-1",
                new BigDecimal("120000"), Instant.now());

        String json = mapper.writeValueAsString(event);

        assertThat(json).contains("\"type\":\"PaymentRequested\"");
        assertThat(mapper.readValue(json, OrderEvent.class)).isEqualTo(event);
    }

    @Test
    void consumesPaymentEventPublishedByPaymentService() {
        String json = """
                {"type":"PaymentFailed","eventId":"11111111-1111-1111-1111-111111111111",
                 "orderId":"22222222-2222-2222-2222-222222222222","userId":"u-1",
                 "reason":"Insufficient balance","occurredAt":"2026-01-01T00:00:00Z"}
                """;

        PaymentEvent event = mapper.readValue(json, PaymentEvent.class);

        String result = switch (event) {
            case PaymentEvent.PaymentSucceeded s -> "paid " + s.amount();
            case PaymentEvent.PaymentFailed f -> "failed: " + f.reason();
        };
        assertThat(result).isEqualTo("failed: Insufficient balance");
    }
}
