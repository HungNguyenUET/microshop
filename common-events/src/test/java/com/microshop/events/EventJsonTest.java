package com.microshop.events;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class EventJsonTest {

    private final JsonMapper mapper = JsonMapper.builder().build();

    @Test
    void serializesTypeAndRestoresConcreteRecord() {
        OrderEvent event = new OrderEvent.PaymentRequested(UUID.randomUUID(), UUID.randomUUID(), "u-1",
                new BigDecimal("120000"), Instant.now());

        String json = mapper.writeValueAsString(event);

        assertThat(json).contains("\"type\":\"PaymentRequested\"");
        assertThat(mapper.readValue(json, OrderEvent.class)).isEqualTo(event);
    }

    @Test
    void sealedSwitchIsExhaustive() {
        PaymentEvent event = new PaymentEvent.PaymentFailed(UUID.randomUUID(), UUID.randomUUID(), "u-1",
                "Insufficient balance", Instant.now());

        String result = switch (event) {
            case PaymentEvent.PaymentSucceeded s -> "paid " + s.amount();
            case PaymentEvent.PaymentFailed f -> "failed: " + f.reason();
        };

        assertThat(result).isEqualTo("failed: Insufficient balance");
    }
}
