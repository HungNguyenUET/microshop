CREATE TABLE wallet (
    user_id     VARCHAR(64)    PRIMARY KEY,            -- claim "sub" trong JWT
    balance     NUMERIC(15, 0) NOT NULL CHECK (balance >= 0),
    version     BIGINT         NOT NULL DEFAULT 0,
    created_at  TIMESTAMPTZ    NOT NULL,
    updated_at  TIMESTAMPTZ    NOT NULL
);

-- Lich su bien dong so du (ca nap tien lan thanh toan).
CREATE TABLE wallet_transaction (
    id          UUID PRIMARY KEY,
    user_id     VARCHAR(64)    NOT NULL REFERENCES wallet (user_id),
    type        VARCHAR(20)    NOT NULL,               -- TOP_UP | PAYMENT
    amount      NUMERIC(15, 0) NOT NULL,
    status      VARCHAR(20)    NOT NULL,               -- SUCCESS | FAILED
    order_id    UUID,
    reason      VARCHAR(255),
    -- eventId cua PaymentRequested. UNIQUE -> Kafka gui lai cung 1 event cung KHONG tru tien 2 lan.
    event_id    UUID UNIQUE,
    created_at  TIMESTAMPTZ    NOT NULL
);
CREATE INDEX idx_wallet_tx_user ON wallet_transaction (user_id, created_at DESC);
-- 1 order chi duoc tru tien thanh cong 1 lan
CREATE UNIQUE INDEX uk_wallet_tx_order_paid ON wallet_transaction (order_id) WHERE type = 'PAYMENT' AND status = 'SUCCESS';
