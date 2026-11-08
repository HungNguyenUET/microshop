-- Chay 1 lan khi volume Postgres con trong.
-- "Moi service 1 database": cung 1 Postgres instance cho nhe may, nhung moi service co
-- database + user rieng. User cua service A KHONG ket noi duoc database cua service B.
CREATE USER auth_svc         WITH PASSWORD 'auth_pwd';
CREATE USER product_svc      WITH PASSWORD 'product_pwd';
CREATE USER order_svc        WITH PASSWORD 'order_pwd';
CREATE USER payment_svc      WITH PASSWORD 'payment_pwd';
CREATE USER notification_svc WITH PASSWORD 'notification_pwd';

CREATE DATABASE auth_db         OWNER auth_svc;
CREATE DATABASE product_db      OWNER product_svc;
CREATE DATABASE order_db        OWNER order_svc;
CREATE DATABASE payment_db      OWNER payment_svc;
CREATE DATABASE notification_db OWNER notification_svc;

REVOKE CONNECT ON DATABASE auth_db         FROM PUBLIC;
REVOKE CONNECT ON DATABASE product_db      FROM PUBLIC;
REVOKE CONNECT ON DATABASE order_db        FROM PUBLIC;
REVOKE CONNECT ON DATABASE payment_db      FROM PUBLIC;
REVOKE CONNECT ON DATABASE notification_db FROM PUBLIC;
