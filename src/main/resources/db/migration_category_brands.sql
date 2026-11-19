-- Migration script: Create category_brands junction table and seed data
USE NextGen;

CREATE TABLE IF NOT EXISTS category_brands (
    category_id BIGINT NOT NULL,
    brand_id BIGINT NOT NULL,
    PRIMARY KEY (category_id, brand_id),
    CONSTRAINT fk_cb_category FOREIGN KEY (category_id) REFERENCES categories (category_id) ON DELETE CASCADE,
    CONSTRAINT fk_cb_brand FOREIGN KEY (brand_id) REFERENCES brands (brand_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Seed category_brands mappings
INSERT IGNORE INTO category_brands (category_id, brand_id) VALUES
-- Apple (1): Phone, Tablet, Laptop, Accessory, Smartwatch
(1, 1), (2, 1), (3, 1), (4, 1), (15, 1),

-- Samsung (2): Phone, Tablet, Accessory, Smartwatch
(1, 2), (2, 2), (4, 2), (15, 2),

-- Xiaomi (3): Phone, Tablet, Accessory, Smartwatch
(1, 3), (2, 3), (4, 3), (15, 3),

-- Sony (4): Phone, Accessory
(1, 4), (4, 4),

-- Asus (5): Laptop
(3, 5),

-- Anker (6): Accessory
(4, 6),

-- Marshall (7): Accessory
(4, 7),

-- Dell (8): Laptop
(3, 8),

-- HP (9): Laptop
(3, 9),

-- Lenovo (10): Tablet, Laptop
(2, 10), (3, 10),

-- LG (11): Laptop
(3, 11),

-- Google (12): Phone, Smartwatch
(1, 12), (15, 12),

-- Garmin (13): Smartwatch
(15, 13),

-- JBL (14): Accessory
(4, 14),

-- Logitech (15): Accessory
(4, 15),

-- Baseus (16): Accessory
(4, 16),

-- Sennheiser (17): Accessory
(4, 17);
