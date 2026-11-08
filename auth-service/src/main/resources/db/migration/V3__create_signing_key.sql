-- Cap khoa RSA dung de KY JWT. Luu trong DB de restart auth-service khong lam vo hieu token dang dung.
-- Production: dung KMS / Vault va co co che xoay vong khoa (key rotation).
CREATE TABLE signing_key (
    kid          VARCHAR(100) PRIMARY KEY,
    public_key   TEXT         NOT NULL,   -- X.509 / Base64
    private_key  TEXT         NOT NULL,   -- PKCS#8 / Base64
    created_at   TIMESTAMPTZ  NOT NULL
);
