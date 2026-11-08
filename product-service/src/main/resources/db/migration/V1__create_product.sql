CREATE TABLE product (
    id           UUID PRIMARY KEY,
    sku          VARCHAR(50)    NOT NULL UNIQUE,
    name         VARCHAR(200)   NOT NULL,
    description  VARCHAR(1000),
    price        NUMERIC(15, 0) NOT NULL CHECK (price >= 0),   -- VND, khong co phan le
    category     VARCHAR(50)    NOT NULL,
    active       BOOLEAN        NOT NULL DEFAULT TRUE,
    created_at   TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ    NOT NULL DEFAULT now()
);

INSERT INTO product (id, sku, name, description, price, category) VALUES
 ('a0000000-0000-0000-0000-000000000001', 'KB-01',  'Bàn phím cơ 87 phím',      'Switch brown, kết nối USB-C',              1290000, 'Phụ kiện'),
 ('a0000000-0000-0000-0000-000000000002', 'MS-01',  'Chuột không dây',          'Pin 6 tháng, 2.4GHz + Bluetooth',           450000, 'Phụ kiện'),
 ('a0000000-0000-0000-0000-000000000003', 'HP-01',  'Tai nghe chống ồn',        'ANC, pin 30 giờ',                          2990000, 'Âm thanh'),
 ('a0000000-0000-0000-0000-000000000004', 'MN-01',  'Màn hình 27 inch 4K',      'IPS, 60Hz, USB-C 65W',                     7490000, 'Màn hình'),
 ('a0000000-0000-0000-0000-000000000005', 'LP-01',  'Laptop lập trình 16GB',    'CPU 12 nhân, RAM 16GB, SSD 512GB',        25990000, 'Laptop'),
 ('a0000000-0000-0000-0000-000000000006', 'BK-01',  'Sách: Building Microservices', 'Sam Newman, bản 2',                      650000, 'Sách'),
 ('a0000000-0000-0000-0000-000000000007', 'BK-02',  'Sách: Designing Data-Intensive Applications', 'Martin Kleppmann',        790000, 'Sách'),
 ('a0000000-0000-0000-0000-000000000008', 'WC-01',  'Webcam Full HD',           '1080p 30fps, micro kép',                    890000, 'Phụ kiện');
