package com.microshop.payment.events;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Contract test: co dinh dinh dang JSON tren Kafka. Khong co jar event dung chung,
 * nen day la "hang rao" dam bao ban sao contract cua payment-service van khop voi cac service khac.
 */
class EventContractTest {

    private final JsonMapper mapper = JsonMapper.builder().build();

    @Test
    void consumesPaymentRequestedPublishedByOrderService() {
        String json = """
                {"type":"PaymentRequested","eventId":"11111111-1111-1111-1111-111111111111",
                 "orderId":"22222222-2222-2222-2222-222222222222","userId":"u-1",
                 "amount":120000,"occurredAt":"2026-01-01T00:00:00Z"}
                """;

        OrderEvent event = mapper.readValue(json, OrderEvent.class);

        assertThat(event).isInstanceOf(OrderEvent.PaymentRequested.class);
        var request = (OrderEvent.PaymentRequested) event;
        assertThat(request.amount()).isEqualByComparingTo("120000");
        assertThat(request.userId()).isEqualTo("u-1");
    }

    @Test
    void producesPaymentEventWithTypeNameAndRoundTrips() {
        PaymentEvent event = new PaymentEvent.PaymentSucceeded(UUID.randomUUID(), UUID.randomUUID(), "u-1",
                new BigDecimal("120000"), new BigDecimal("30000"), Instant.now());

        String json = mapper.writeValueAsString(event);

        assertThat(json).contains("\"type\":\"PaymentSucceeded\"");
        assertThat(mapper.readValue(json, PaymentEvent.class)).isEqualTo(event);
    }
}
