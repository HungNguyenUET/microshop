CREATE TABLE notification (
    id          UUID PRIMARY KEY,
    user_id     VARCHAR(64)  NOT NULL,
    order_id    UUID,
    type        VARCHAR(50)  NOT NULL,     -- ORDER_PLACED | ORDER_PAID | PAYMENT_FAILED
    title       VARCHAR(200) NOT NULL,
    message     VARCHAR(500) NOT NULL,
    -- eventId cua Kafka: UNIQUE -> event giao lai cung khong tao thong bao trung
    event_id    UUID         NOT NULL UNIQUE,
    created_at  TIMESTAMPTZ  NOT NULL
);
CREATE INDEX idx_notification_user ON notification (user_id, created_at DESC);
