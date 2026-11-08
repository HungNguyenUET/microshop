-- Gio hang: chi luu productId + so luong. Ten, gia lay tu product-service khi hien thi (REST).
CREATE TABLE cart_item (
    id          UUID PRIMARY KEY,
    user_id     VARCHAR(64) NOT NULL,                 -- claim "sub" trong JWT
    product_id  UUID        NOT NULL,                 -- KHONG co FK: product nam o database khac!
    quantity    INT         NOT NULL CHECK (quantity > 0),
    added_at    TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_cart_user_product UNIQUE (user_id, product_id)
);

CREATE TABLE orders (
    id              UUID PRIMARY KEY,
    user_id         VARCHAR(64)    NOT NULL,
    username        VARCHAR(100)   NOT NULL,
    status          VARCHAR(30)    NOT NULL,          -- PENDING_PAYMENT | PAYMENT_PROCESSING | PAID | PAYMENT_FAILED
    total_amount    NUMERIC(15, 0) NOT NULL,
    failure_reason  VARCHAR(255),
    version         BIGINT         NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ    NOT NULL,
    updated_at      TIMESTAMPTZ    NOT NULL
);
CREATE INDEX idx_orders_user ON orders (user_id, created_at DESC);

-- Snapshot ten + gia LUC DAT HANG: product doi gia sau nay cung khong anh huong don cu.
CREATE TABLE order_item (
    id            UUID PRIMARY KEY,
    order_id      UUID           NOT NULL REFERENCES orders (id) ON DELETE CASCADE,
    product_id    UUID           NOT NULL,
    product_name  VARCHAR(200)   NOT NULL,
    unit_price    NUMERIC(15, 0) NOT NULL,
    quantity      INT            NOT NULL CHECK (quantity > 0)
);
CREATE INDEX idx_order_item_order ON order_item (order_id);
