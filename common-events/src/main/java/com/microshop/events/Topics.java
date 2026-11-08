package com.microshop.events;

/** Ten topic Kafka. Moi service chi publish len topic cua chinh minh. */
public final class Topics {

    /** order-service publish; payment-service va notification-service subscribe. */
    public static final String ORDER_EVENTS = "order-events";

    /** payment-service publish; order-service subscribe. */
    public static final String PAYMENT_EVENTS = "payment-events";

    private Topics() {
    }
}
