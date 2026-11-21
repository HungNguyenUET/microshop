package com.microshop.notification.events;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Contract test: co dinh dinh dang JSON tren Kafka. Khong co jar event dung chung,
 * nen day la "hang rao" dam bao ban sao contract cua notification-service van khop voi order-service.
 */
class EventContractTest {

    private final JsonMapper mapper = JsonMapper.builder().build();

    @Test
    void consumesOrderPaidPublishedByOrderService() {
        String json = """
                {"type":"OrderPaid","eventId":"11111111-1111-1111-1111-111111111111",
                 "orderId":"22222222-2222-2222-2222-222222222222","userId":"u-1","username":"alice",
                 "amount":120000,"occurredAt":"2026-01-01T00:00:00Z"}
                """;

        OrderEvent event = mapper.readValue(json, OrderEvent.class);

        assertThat(event).isInstanceOf(OrderEvent.OrderPaid.class);
        var paid = (OrderEvent.OrderPaid) event;
        assertThat(paid.username()).isEqualTo("alice");
        assertThat(paid.amount()).isEqualByComparingTo("120000");
    }
}
