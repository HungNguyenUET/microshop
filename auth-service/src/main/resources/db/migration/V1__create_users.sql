-- User cua he thong MicroShop. Dang nhap bang GitHub (provider=GITHUB) hoac tai khoan demo (provider=LOCAL).
CREATE TABLE app_user (
    id               UUID PRIMARY KEY,                 -- = claim "sub" trong JWT
    username         VARCHAR(100) NOT NULL UNIQUE,
    display_name     VARCHAR(200),
    email            VARCHAR(255),
    avatar_url       VARCHAR(500),
    provider         VARCHAR(20)  NOT NULL,            -- LOCAL | GITHUB
    provider_user_id VARCHAR(100),                     -- GitHub numeric id
    password_hash    VARCHAR(200),                     -- chi co voi LOCAL
    enabled          BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at       TIMESTAMPTZ  NOT NULL,
    last_login_at    TIMESTAMPTZ,
    CONSTRAINT uk_user_provider UNIQUE (provider, provider_user_id)
);

CREATE TABLE app_user_role (
    user_id  UUID        NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
    role     VARCHAR(30) NOT NULL,                     -- USER | ADMIN
    PRIMARY KEY (user_id, role)
);
