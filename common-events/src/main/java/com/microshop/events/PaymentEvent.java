package com.microshop.events;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Cac event do payment-service phat ra (topic {@value Topics#PAYMENT_EVENTS}). */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = PaymentEvent.PaymentSucceeded.class, name = "PaymentSucceeded"),
        @JsonSubTypes.Type(value = PaymentEvent.PaymentFailed.class, name = "PaymentFailed")
})
public sealed interface PaymentEvent {

    UUID eventId();

    UUID orderId();

    Instant occurredAt();

    record PaymentSucceeded(UUID eventId, UUID orderId, String userId, BigDecimal amount, BigDecimal balanceAfter,
                            Instant occurredAt) implements PaymentEvent {
    }

    record PaymentFailed(UUID eventId, UUID orderId, String userId, String reason,
                         Instant occurredAt) implements PaymentEvent {
    }
}
