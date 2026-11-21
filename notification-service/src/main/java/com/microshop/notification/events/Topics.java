package com.microshop.notification.events;

/** Ten topic Kafka ma notification-service doc (chi subscribe, khong publish). */
public final class Topics {

    /** order-service publish; notification-service subscribe. */
    public static final String ORDER_EVENTS = "order-events";

    private Topics() {
    }
}
