-- NextGen E-Commerce Sample Seed Data
-- Database: NextGen (MySQL)
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- 1. USERS
-- Password for all accounts:
-- ADMIN: Admin@123 (Hash: $2a$10$KjY9JZVDMt10IXI9V5asAehIlnf64YYaZasuiXanzSD00Fj28ggme)
-- STAFF & CUSTOMER: Password@123 (Hash: $2a$10$lKifkavs5h2GbVPkX65AHuBBJf8qFtuqHfv0FA/JYIUShTBkbySvm)

INSERT INTO users (user_id, full_name, email, phone, password_hash, role, status, lock_reason, created_at, updated_at) VALUES
(1, 'Trịnh Văn Quang', 'trinhvanquang02042004@gmail.com', '0989801827', '$2a$10$KjY9JZVDMt10IXI9V5asAehIlnf64YYaZasuiXanzSD00Fj28ggme', 'ADMIN', 'ACTIVE', NULL, '2026-01-01 08:00:00', '2026-01-01 08:00:00')
ON DUPLICATE KEY UPDATE 
    full_name = VALUES(full_name),
    password_hash = VALUES(password_hash),
    role = VALUES(role),
    status = VALUES(status);

INSERT INTO users (user_id, full_name, email, phone, password_hash, role, status, lock_reason, created_at, updated_at) VALUES
(2, 'Nguyễn Văn Thắng', 'staff.thang@nextgen.vn', '0912345678', '$2a$10$lKifkavs5h2GbVPkX65AHuBBJf8qFtuqHfv0FA/JYIUShTBkbySvm', 'STAFF', 'ACTIVE', NULL, '2026-02-01 09:00:00', '2026-02-01 09:00:00'),
(3, 'Lê Thị Mai', 'staff.mai@nextgen.vn', '0923456789', '$2a$10$lKifkavs5h2GbVPkX65AHuBBJf8qFtuqHfv0FA/JYIUShTBkbySvm', 'STAFF', 'ACTIVE', NULL, '2026-02-15 09:30:00', '2026-02-15 09:30:00'),
(4, 'Trần Thị Thu Huyền', 'huyen.tran@gmail.com', '0987654321', '$2a$10$lKifkavs5h2GbVPkX65AHuBBJf8qFtuqHfv0FA/JYIUShTBkbySvm', 'CUSTOMER', 'ACTIVE', NULL, '2026-03-01 10:00:00', '2026-03-01 10:00:00'),
(5, 'Hoàng Minh Khôi', 'khoi.hoang@gmail.com', '0971234567', '$2a$10$lKifkavs5h2GbVPkX65AHuBBJf8qFtuqHfv0FA/JYIUShTBkbySvm', 'CUSTOMER', 'ACTIVE', NULL, '2026-03-10 11:20:00', '2026-03-10 11:20:00'),
(6, 'Phạm Đức Anh', 'anh.pham@gmail.com', '0962345678', '$2a$10$lKifkavs5h2GbVPkX65AHuBBJf8qFtuqHfv0FA/JYIUShTBkbySvm', 'CUSTOMER', 'ACTIVE', NULL, '2026-03-15 14:15:00', '2026-03-15 14:15:00'),
(7, 'Đỗ Bảo Ngọc', 'ngoc.do@gmail.com', '0933456789', '$2a$10$lKifkavs5h2GbVPkX65AHuBBJf8qFtuqHfv0FA/JYIUShTBkbySvm', 'CUSTOMER', 'ACTIVE', NULL, '2026-04-01 08:45:00', '2026-04-01 08:45:00'),
(8, 'Vũ Tuấn Kiệt', 'kiet.vu@gmail.com', '0944567890', '$2a$10$lKifkavs5h2GbVPkX65AHuBBJf8qFtuqHfv0FA/JYIUShTBkbySvm', 'CUSTOMER', 'ACTIVE', NULL, '2026-04-10 16:30:00', '2026-04-10 16:30:00'),
(9, 'Bùi Phương Thảo', 'thao.bui@gmail.com', '0955678901', '$2a$10$lKifkavs5h2GbVPkX65AHuBBJf8qFtuqHfv0FA/JYIUShTBkbySvm', 'CUSTOMER', 'ACTIVE', NULL, '2026-05-01 10:10:00', '2026-05-01 10:10:00'),
(10, 'Đinh Quang Huy', 'huy.dinh@gmail.com', '0901234567', '$2a$10$lKifkavs5h2GbVPkX65AHuBBJf8qFtuqHfv0FA/JYIUShTBkbySvm', 'CUSTOMER', 'LOCKED', 'Vi phạm điều khoản cộng đồng: spam đánh giá ảo', '2026-05-05 13:00:00', '2026-05-06 14:00:00')
ON DUPLICATE KEY UPDATE 
    full_name = VALUES(full_name),
    password_hash = VALUES(password_hash),
    role = VALUES(role),
    status = VALUES(status),
    lock_reason = VALUES(lock_reason);

-- 2. USER ADDRESSES
DELETE FROM user_addresses;
INSERT INTO user_addresses (address_id, user_user_id, recipient_name, phone, address_line, is_default, created_at, updated_at) VALUES
(1, 4, 'Trần Thị Thu Huyền', '0987654321', 'Số 12 ngõ 45 phố Trần Thái Tông, Dịch Vọng Hậu, Cầu Giấy, Hà Nội', b'1', '2026-03-01 10:05:00', '2026-03-01 10:05:00'),
(2, 4, 'Trần Thị Thu Huyền', '0987654321', 'Tòa nhà Landmark 81, Phường 22, Bình Thạnh, TP. Hồ Chí Minh', b'0', '2026-04-02 11:00:00', '2026-04-02 11:00:00'),
(3, 5, 'Hoàng Minh Khôi', '0971234567', 'Số 88 đường Nguyễn Văn Linh, Phường Nam Dương, Hải Châu, Đà Nẵng', b'1', '2026-03-10 11:25:00', '2026-03-10 11:25:00'),
(4, 6, 'Phạm Đức Anh', '0962345678', 'Số 250 đường Lạch Tray, Quận Ngô Quyền, Hải Phòng', b'1', '2026-03-15 14:20:00', '2026-03-15 14:20:00'),
(5, 7, 'Đỗ Bảo Ngọc', '0933456789', 'Số 18 đường 30 Tháng 4, Phường An Phú, Ninh Kiều, Cần Thơ', b'1', '2026-04-01 08:50:00', '2026-04-01 08:50:00'),
(6, 8, 'Vũ Tuấn Kiệt', '0944567890', 'Số 105 phố Bà Triệu, Phường Hàng Bài, Hoàn Kiếm, Hà Nội', b'1', '2026-04-10 16:35:00', '2026-04-10 16:35:00'),
(7, 9, 'Bùi Phương Thảo', '0955678901', 'Căn hộ B12-08 Sunrise City, Quận 7, TP. Hồ Chí Minh', b'1', '2026-05-01 10:15:00', '2026-05-01 10:15:00');

-- 3. BRANDS
DELETE FROM brands;
INSERT INTO brands (brand_id, brand_name, slug, description, logo_url, is_active, created_at, updated_at) VALUES
(1, 'Apple', 'apple', 'Thương hiệu công nghệ hàng đầu thế giới với hệ sinh thái iPhone, iPad, Mac', 'https://cdn.nextgen.vn/brands/apple.png', b'1', '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
(2, 'Samsung', 'samsung', 'Tập đoàn điện tử công nghệ đa quốc gia từ Hàn Quốc', 'https://cdn.nextgen.vn/brands/samsung.png', b'1', '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
(3, 'Xiaomi', 'xiaomi', 'Hệ sinh thái thiết bị thông minh và flagship cấu hình cao giá tốt', 'https://cdn.nextgen.vn/brands/xiaomi.png', b'1', '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
(4, 'Sony', 'sony', 'Âm thanh chuyên nghiệp đỉnh cao và thiết bị nhiếp ảnh hàng đầu', 'https://cdn.nextgen.vn/brands/sony.png', b'1', '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
(5, 'Asus', 'asus', 'Nhà sản xuất bo mạch chủ, laptop gaming ROG và linh kiện PC cao cấp', 'https://cdn.nextgen.vn/brands/asus.png', b'1', '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
(6, 'Anker', 'anker', 'Thương hiệu phụ kiện sạc nhanh, pin dự phòng và audio số 1 toàn cầu', 'https://cdn.nextgen.vn/brands/anker.png', b'1', '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
(7, 'Marshall', 'marshall', 'Huyền thoại âm thanh vintage từ Anh Quốc với chất âm rock đặc trưng', 'https://cdn.nextgen.vn/brands/marshall.png', b'1', '2026-01-01 00:00:00', '2026-01-01 00:00:00');

-- 4. CATEGORIES (Tree structure)
DELETE FROM categories;
-- Parent categories
INSERT INTO categories (category_id, parent_category_id, category_name, slug, description, icon_url, is_active, created_at, updated_at) VALUES
(1, NULL, 'Điện thoại di động', 'dien-thoai-di-dong', 'Các dòng smartphone chính hãng bảo hành điện tử', 'https://cdn.nextgen.vn/categories/phone.svg', b'1', '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
(2, NULL, 'Máy tính bảng', 'may-tinh-bang', 'Tablet học tập, làm việc sáng tạo chuyên nghiệp', 'https://cdn.nextgen.vn/categories/tablet.svg', b'1', '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
(3, NULL, 'Laptop & PC', 'laptop-pc', 'Laptop văn phòng mỏng nhẹ, laptop gaming đồ họa cao', 'https://cdn.nextgen.vn/categories/laptop.svg', b'1', '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
(4, NULL, 'Phụ kiện & Âm thanh', 'phu-kien-am-thanh', 'Tai nghe, loa bluetooth, củ cáp sạc nhanh', 'https://cdn.nextgen.vn/categories/accessory.svg', b'1', '2026-01-01 00:00:00', '2026-01-01 00:00:00');

-- Child categories
INSERT INTO categories (category_id, parent_category_id, category_name, slug, description, icon_url, is_active, created_at, updated_at) VALUES
(5, 1, 'iPhone (Apple)', 'iphone-apple', 'Điện thoại iPhone VN/A chính hãng phân phối tại Việt Nam', 'https://cdn.nextgen.vn/categories/iphone.svg', b'1', '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
(6, 1, 'Samsung Galaxy', 'samsung-galaxy', 'Dòng Galaxy S Ultra, Z Fold và Z Flip gập mở cao cấp', 'https://cdn.nextgen.vn/categories/samsung.svg', b'1', '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
(7, 1, 'Xiaomi', 'xiaomi', 'Smartphone Xiaomi 14 series, Redmi Note hiệu năng đỉnh', 'https://cdn.nextgen.vn/categories/xiaomi.svg', b'1', '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
(8, 2, 'iPad', 'ipad', 'iPad Pro M4, iPad Air M2 và iPad Gen 10 chính hãng', 'https://cdn.nextgen.vn/categories/ipad.svg', b'1', '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
(9, 2, 'Samsung Tab', 'samsung-tab', 'Galaxy Tab S9 Series màn hình Dynamic AMOLED 2X', 'https://cdn.nextgen.vn/categories/samsung-tab.svg', b'1', '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
(10, 3, 'MacBook', 'macbook', 'MacBook Air, MacBook Pro chip Apple Silicon', 'https://cdn.nextgen.vn/categories/macbook.svg', b'1', '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
(11, 3, 'Laptop Gaming Asus ROG', 'laptop-gaming-asus-rog', 'Laptop gaming màn hình tần số quét cao, tản nhiệt buồng hơi', 'https://cdn.nextgen.vn/categories/asus-rog.svg', b'1', '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
(12, 4, 'Tai nghe Chống ồn', 'tai-nghe-chong-on', 'Tai nghe True Wireless và Over-ear chống ồn chủ động ANC', 'https://cdn.nextgen.vn/categories/headphone.svg', b'1', '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
(13, 4, 'Loa Bluetooth Di động', 'loa-bluetooth-di-dong', 'Loa kháng nước công suất lớn âm trầm uy lực', 'https://cdn.nextgen.vn/categories/speaker.svg', b'1', '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
(14, 4, 'Củ sạc & Cáp GaN', 'cu-sac-cap-gan', 'Công nghệ sạc bán dẫn GaN công suất cao nhỏ gọn', 'https://cdn.nextgen.vn/categories/charger.svg', b'1', '2026-01-01 00:00:00', '2026-01-01 00:00:00');

-- 5. PRODUCTS
DELETE FROM products;
INSERT INTO products (product_id, category_category_id, brand_brand_id, product_name, slug, description, thumbnail_url, base_price, status, created_at, updated_at) VALUES
(1, 5, 1, 'iPhone 16 Pro Max', 'iphone-16-pro-max', 'Khung viền Titan sa mạc, nút điều khiển Camera Control, Chip A18 Pro mạnh mẽ nhất', 'https://cdn.nextgen.vn/products/iphone-16-pro-max-desert.png', 34990000.00, 'ACTIVE', '2026-02-01 10:00:00', '2026-02-01 10:00:00'),
(2, 5, 1, 'iPhone 15', 'iphone-15', 'Màn hình Dynamic Island, cổng sạc Type-C, camera 48MP nâng cấp sắc nét', 'https://cdn.nextgen.vn/products/iphone-15-pink.png', 19990000.00, 'ACTIVE', '2026-02-01 10:30:00', '2026-02-01 10:30:00'),
(3, 6, 2, 'Samsung Galaxy S24 Ultra', 'samsung-galaxy-s24-ultra', 'Khung viền Titan, màn hình phẳng chống chói, tích hợp bút S-Pen và quyền năng Galaxy AI', 'https://cdn.nextgen.vn/products/galaxy-s24-ultra-gray.png', 31990000.00, 'ACTIVE', '2026-02-02 09:00:00', '2026-02-02 09:00:00'),
(4, 7, 3, 'Xiaomi 14 Ultra', 'xiaomi-14-ultra', 'Cụm 4 ống kính quang học Leica Summilux, cảm biến 1-inch LYT-900 đỉnh cao nhiếp ảnh', 'https://cdn.nextgen.vn/products/xiaomi-14-ultra-white.png', 29990000.00, 'ACTIVE', '2026-02-05 14:00:00', '2026-02-05 14:00:00'),
(5, 8, 1, 'iPad Air M2 11 inch', 'ipad-air-m2-11-inch', 'Hiệu năng đột phá từ vi xử lý M2, hỗ trợ Apple Pencil Pro và Magic Keyboard', 'https://cdn.nextgen.vn/products/ipad-air-m2-spacegray.png', 16990000.00, 'ACTIVE', '2026-02-10 11:00:00', '2026-02-10 11:00:00'),
(6, 10, 1, 'MacBook Pro 14 M3 Pro', 'macbook-pro-14-m3-pro', 'Màn hình Liquid Retina XDR 120Hz, màu Space Black chống bám vân tay', 'https://cdn.nextgen.vn/products/macbook-pro-14-m3-black.png', 49990000.00, 'ACTIVE', '2026-02-15 15:00:00', '2026-02-15 15:00:00'),
(7, 12, 4, 'Tai nghe Sony WH-1000XM5', 'tai-nghe-sony-wh-1000xm5', 'Chống ồn đỉnh cao hàng đầu thế giới với bộ xử lý V1 và QN1, thời lượng pin 30 giờ', 'https://cdn.nextgen.vn/products/sony-wh-1000xm5-black.png', 8490000.00, 'ACTIVE', '2026-02-20 16:00:00', '2026-02-20 16:00:00'),
(8, 13, 7, 'Loa Marshall Emberton II', 'loa-marshall-emberton-ii', 'Công nghệ âm thanh đa hướng True Stereophonic, kháng nước chuẩn IP67, pin trên 30 giờ', 'https://cdn.nextgen.vn/products/marshall-emberton-ii.png', 3590000.00, 'ACTIVE', '2026-02-25 10:00:00', '2026-02-25 10:00:00'),
(9, 14, 6, 'Củ sạc Anker GaNPrime 65W', 'cu-sac-anker-ganprime-65w', 'Công nghệ sạc nhanh GaNPrime độc quyền, 2 cổng Type-C + 1 cổng USB-A tiện lợi', 'https://cdn.nextgen.vn/products/anker-ganprime-65w.png', 1250000.00, 'ACTIVE', '2026-03-01 09:00:00', '2026-03-01 09:00:00');

-- 6. PRODUCT VARIANTS
DELETE FROM product_variants;
INSERT INTO product_variants (variant_id, product_product_id, variant_name, sku, price, compare_at_price, color, ram_gb, rom_gb, image_url, status, created_at, updated_at) VALUES
(1, 1, 'iPhone 16 Pro Max 256GB - Titan Sa Mạc', 'IP16PM-256-DESERT', 34990000.00, 36990000.00, 'Titan Sa Mạc', 8, 256, 'https://cdn.nextgen.vn/variants/ip16pm-desert.png', 'ACTIVE', '2026-02-01 10:00:00', '2026-02-01 10:00:00'),
(2, 1, 'iPhone 16 Pro Max 256GB - Titan Tự Nhiên', 'IP16PM-256-NATURAL', 34990000.00, 36990000.00, 'Titan Tự Nhiên', 8, 256, 'https://cdn.nextgen.vn/variants/ip16pm-natural.png', 'ACTIVE', '2026-02-01 10:00:00', '2026-02-01 10:00:00'),
(3, 1, 'iPhone 16 Pro Max 512GB - Titan Đen', 'IP16PM-512-BLACK', 40990000.00, 42990000.00, 'Titan Đen', 8, 512, 'https://cdn.nextgen.vn/variants/ip16pm-black.png', 'ACTIVE', '2026-02-01 10:00:00', '2026-02-01 10:00:00'),
(4, 2, 'iPhone 15 128GB - Hồng Pastel', 'IP15-128-PINK', 19990000.00, 21990000.00, 'Hồng Pastel', 6, 128, 'https://cdn.nextgen.vn/variants/ip15-pink.png', 'ACTIVE', '2026-02-01 10:30:00', '2026-02-01 10:30:00'),
(5, 3, 'Galaxy S24 Ultra 256GB - Xám Titan', 'S24U-256-GRAY', 31990000.00, 33990000.00, 'Xám Titan', 12, 256, 'https://cdn.nextgen.vn/variants/s24u-gray.png', 'ACTIVE', '2026-02-02 09:00:00', '2026-02-02 09:00:00'),
(6, 4, 'Xiaomi 14 Ultra 512GB - Trắng Gốm', 'MI14U-512-WHITE', 29990000.00, 32990000.00, 'Trắng Gốm', 16, 512, 'https://cdn.nextgen.vn/variants/mi14u-white.png', 'ACTIVE', '2026-02-05 14:00:00', '2026-02-05 14:00:00'),
(7, 5, 'iPad Air M2 11 inch 128GB WiFi - Space Gray', 'IPADAIR-M2-128-GRAY', 16990000.00, 17990000.00, 'Xám Không Gian', 8, 128, 'https://cdn.nextgen.vn/variants/ipad-air-gray.png', 'ACTIVE', '2026-02-10 11:00:00', '2026-02-10 11:00:00'),
(8, 6, 'MacBook Pro 14 M3 Pro 18GB/512GB - Space Black', 'MBP14-M3P-512-BLACK', 49990000.00, 52990000.00, 'Đen Không Gian', 18, 512, 'https://cdn.nextgen.vn/variants/mbp14-black.png', 'ACTIVE', '2026-02-15 15:00:00', '2026-02-15 15:00:00'),
(9, 7, 'Sony WH-1000XM5 - Đen Nhám', 'SONY-XM5-BLACK', 8490000.00, 8990000.00, 'Đen Nhám', 0, 0, 'https://cdn.nextgen.vn/variants/sony-xm5-black.png', 'ACTIVE', '2026-02-20 16:00:00', '2026-02-20 16:00:00'),
(10, 8, 'Marshall Emberton II - Black and Brass', 'MARSHALL-EMB2-BRASS', 3590000.00, 3990000.00, 'Black and Brass', 0, 0, 'https://cdn.nextgen.vn/variants/marshall-emb2.png', 'ACTIVE', '2026-02-25 10:00:00', '2026-02-25 10:00:00'),
(11, 9, 'Củ sạc Anker GaNPrime 65W 3 cổng - Đen', 'ANKER-GAN-65W-BLK', 1250000.00, 1490000.00, 'Đen Nhám', 0, 0, 'https://cdn.nextgen.vn/variants/anker-65w-blk.png', 'ACTIVE', '2026-03-01 09:00:00', '2026-03-01 09:00:00');

-- 7. PRODUCT IMAGES
DELETE FROM product_images;
INSERT INTO product_images (image_id, product_product_id, image_url, is_primary, display_order, created_at) VALUES
(1, 1, 'https://cdn.nextgen.vn/products/iphone-16-pro-max-desert.png', b'1', 1, '2026-02-01 10:00:00'),
(2, 1, 'https://cdn.nextgen.vn/products/iphone-16-pro-max-back.png', b'0', 2, '2026-02-01 10:00:00'),
(3, 2, 'https://cdn.nextgen.vn/products/iphone-15-pink.png', b'1', 1, '2026-02-01 10:30:00'),
(4, 3, 'https://cdn.nextgen.vn/products/galaxy-s24-ultra-gray.png', b'1', 1, '2026-02-02 09:00:00'),
(5, 4, 'https://cdn.nextgen.vn/products/xiaomi-14-ultra-white.png', b'1', 1, '2026-02-05 14:00:00'),
(6, 5, 'https://cdn.nextgen.vn/products/ipad-air-m2-spacegray.png', b'1', 1, '2026-02-10 11:00:00'),
(7, 6, 'https://cdn.nextgen.vn/products/macbook-pro-14-m3-black.png', b'1', 1, '2026-02-15 15:00:00'),
(8, 7, 'https://cdn.nextgen.vn/products/sony-wh-1000xm5-black.png', b'1', 1, '2026-02-20 16:00:00'),
(9, 8, 'https://cdn.nextgen.vn/products/marshall-emberton-ii.png', b'1', 1, '2026-02-25 10:00:00'),
(10, 9, 'https://cdn.nextgen.vn/products/anker-ganprime-65w.png', b'1', 1, '2026-03-01 09:00:00');

-- 8. INVENTORIES (Stock tracking)
DELETE FROM inventories;
INSERT INTO inventories (inventory_id, variant_variant_id, quantity, reserved_quantity, updated_at) VALUES
(1, 1, 45, 2, '2026-11-01 10:00:00'),
(2, 2, 30, 0, '2026-11-01 10:00:00'),
(3, 3, 20, 1, '2026-11-01 10:00:00'),
(4, 4, 60, 0, '2026-11-01 10:00:00'),
(5, 5, 35, 1, '2026-11-01 10:00:00'),
(6, 6, 25, 0, '2026-11-01 10:00:00'),
(7, 7, 40, 0, '2026-11-01 10:00:00'),
(8, 8, 15, 0, '2026-11-01 10:00:00'),
(9, 9, 50, 1, '2026-11-01 10:00:00'),
(10, 10, 30, 1, '2026-11-01 10:00:00'),
(11, 11, 100, 2, '2026-11-01 10:00:00');

-- 9. PROMOTIONS & PROMOTION VARIANTS (Flash Sale)
DELETE FROM promotion_variants;
DELETE FROM promotions;
INSERT INTO promotions (promotion_id, promotion_name, description, discount_type, discount_value, is_active, start_date, end_date, created_by_user_id, created_at, updated_at) VALUES
(1, 'Tuần Lễ Flash Sale Flagship 2026', 'Chiến dịch giảm sốc các dòng điện thoại và phụ kiện cao cấp', 'PERCENTAGE', 10.00, b'1', '2026-11-01 00:00:00', '2026-11-30 23:59:59', 1, '2026-10-25 09:00:00', '2026-10-25 09:00:00');

INSERT INTO promotion_variants (promo_variant_id, promotion_promotion_id, variant_variant_id, promotional_price, created_at) VALUES
(1, 1, 1, 33990000.00, '2026-10-25 09:30:00'),
(2, 1, 5, 29990000.00, '2026-10-25 09:30:00'),
(3, 1, 9, 7990000.00, '2026-10-25 09:30:00');

-- 10. VOUCHERS
DELETE FROM vouchers;
INSERT INTO vouchers (voucher_id, voucher_code, voucher_name, discount_type, discount_value, min_order_value, max_discount_amount, usage_limit, used_count, is_active, start_date, end_date, created_at, updated_at) VALUES
(1, 'NEXTGEN500', 'Voucher Giảm 500K Cho Flagship', 'FIXED_AMOUNT', 500000.00, 20000000.00, 500000.00, 100, 12, b'1', '2026-10-01 00:00:00', '2026-12-31 23:59:59', '2026-10-01 00:00:00', '2026-10-01 00:00:00'),
(2, 'FREESHIP30', 'Miễn Phí Vận Chuyển Toàn Quốc', 'FIXED_AMOUNT', 30000.00, 500000.00, 30000.00, 500, 85, b'1', '2026-10-01 00:00:00', '2026-12-31 23:59:59', '2026-10-01 00:00:00', '2026-10-01 00:00:00'),
(3, 'TECHNEW10', 'Giảm 10% Cho Phụ Kiện', 'PERCENTAGE', 10.00, 1000000.00, 200000.00, 200, 40, b'1', '2026-10-01 00:00:00', '2026-12-31 23:59:59', '2026-10-01 00:00:00', '2026-10-01 00:00:00');

-- 11. CARTS & CART ITEMS
DELETE FROM cart_items;
DELETE FROM carts;
INSERT INTO carts (cart_id, user_user_id, created_at, updated_at) VALUES
(1, 4, '2026-03-01 10:10:00', '2026-11-04 14:00:00'),
(2, 5, '2026-03-10 11:30:00', '2026-11-04 14:30:00');

INSERT INTO cart_items (cart_item_id, cart_cart_id, variant_variant_id, quantity, created_at, updated_at) VALUES
(1, 1, 1, 1, '2026-11-04 10:00:00', '2026-11-04 10:00:00'),
(2, 1, 9, 1, '2026-11-04 10:15:00', '2026-11-04 10:15:00'),
(3, 2, 11, 2, '2026-11-04 14:30:00', '2026-11-04 14:30:00');

-- 12. ORDERS & ORDER ITEMS
DELETE FROM order_items;
DELETE FROM shipments;
DELETE FROM payments;
DELETE FROM order_status_history;
DELETE FROM notifications;
DELETE FROM orders;

INSERT INTO orders (order_id, order_code, user_user_id, recipient_name, recipient_phone, shipping_address, total_amount, shipping_fee, discount_amount, final_amount, status, payment_status, note, cancel_reason, voucher_voucher_id, created_at, updated_at, completed_at) VALUES
(1, 'DH-20261015-089', 4, 'Trần Thị Thu Huyền', '0987654321', 'Số 12 ngõ 45 phố Trần Thái Tông, Dịch Vọng Hậu, Cầu Giấy, Hà Nội', 34990000.00, 0.00, 500000.00, 34490000.00, 'SHIPPING', 'PAID', 'Giao hàng giờ hành chính', NULL, 1, '2026-10-15 08:30:00', '2026-10-16 10:00:00', NULL),
(2, 'DH-20261020-102', 5, 'Hoàng Minh Khôi', '0971234567', 'Số 88 đường Nguyễn Văn Linh, Phường Nam Dương, Hải Châu, Đà Nẵng', 31990000.00, 0.00, 0.00, 31990000.00, 'DELIVERED', 'PAID', 'Gọi trước khi giao', NULL, NULL, '2026-10-20 09:15:00', '2026-10-23 15:30:00', '2026-10-23 15:30:00'),
(3, 'DH-20261028-115', 6, 'Phạm Đức Anh', '0962345678', 'Số 250 đường Lạch Tray, Quận Ngô Quyền, Hải Phòng', 3590000.00, 30000.00, 30000.00, 3590000.00, 'CONFIRMED', 'PENDING', 'Kiểm tra hàng trước khi thanh toán COD', NULL, 2, '2026-10-28 14:00:00', '2026-10-28 15:00:00', NULL),
(4, 'DH-20261101-120', 7, 'Đỗ Bảo Ngọc', '0933456789', 'Số 18 đường 30 Tháng 4, Phường An Phú, Ninh Kiều, Cần Thơ', 8490000.00, 0.00, 0.00, 8490000.00, 'PROCESSING', 'PAID', 'Đóng gói cẩn thận giúp mình', NULL, NULL, '2026-11-01 11:20:00', '2026-11-01 11:30:00', NULL),
(5, 'DH-20261102-125', 8, 'Vũ Tuấn Kiệt', '0944567890', 'Số 105 phố Bà Triệu, Phường Hàng Bài, Hoàn Kiếm, Hà Nội', 1250000.00, 30000.00, 0.00, 1280000.00, 'CANCELLED', 'FAILED', 'Huỷ đơn', 'Khách hàng đổi ý, đặt nhầm màu sản phẩm', NULL, '2026-11-02 16:00:00', '2026-11-02 16:45:00', NULL),
(6, 'DH-20261103-130', 9, 'Bùi Phương Thảo', '0955678901', 'Căn hộ B12-08 Sunrise City, Quận 7, TP. Hồ Chí Minh', 28500000.00, 0.00, 500000.00, 28000000.00, 'SHIPPING', 'PENDING', 'Giao buổi chiều sau 14h', NULL, 1, '2026-11-03 10:00:00', '2026-11-04 09:00:00', NULL);

-- Order Items
INSERT INTO order_items (order_item_id, order_order_id, variant_variant_id, quantity, price, total_price, created_at) VALUES
(1, 1, 1, 1, 34990000.00, 34990000.00, '2026-10-15 08:30:00'),
(2, 2, 5, 1, 31990000.00, 31990000.00, '2026-10-20 09:15:00'),
(3, 3, 10, 1, 3590000.00, 3590000.00, '2026-10-28 14:00:00'),
(4, 4, 9, 1, 8490000.00, 8490000.00, '2026-11-01 11:20:00'),
(5, 5, 11, 1, 1250000.00, 1250000.00, '2026-11-02 16:00:00'),
(6, 6, 7, 1, 16990000.00, 16990000.00, '2026-11-03 10:00:00'),
(7, 6, 11, 1, 1250000.00, 1250000.00, '2026-11-03 10:00:00');

-- 13. PAYMENTS
INSERT INTO payments (payment_id, order_order_id, payment_method, payment_provider, status, amount, transaction_ref, paid_at, created_at, updated_at) VALUES
(1, 1, 'VNPAY', 'VNPAY_QR', 'COMPLETED', 34490000.00, 'VNP20261015089001', '2026-10-15 08:35:00', '2026-10-15 08:30:00', '2026-10-15 08:35:00'),
(2, 2, 'MOMO', 'MOMO_WALLET', 'COMPLETED', 31990000.00, 'MOM20261020102002', '2026-10-20 09:20:00', '2026-10-20 09:15:00', '2026-10-20 09:20:00'),
(3, 3, 'COD', 'CASH_ON_DELIVERY', 'PENDING', 3590000.00, NULL, NULL, '2026-10-28 14:00:00', '2026-10-28 14:00:00'),
(4, 4, 'VNPAY', 'VNPAY_ATM', 'COMPLETED', 8490000.00, 'VNP20261101120003', '2026-11-01 11:25:00', '2026-11-01 11:20:00', '2026-11-01 11:25:00'),
(5, 5, 'VNPAY', 'VNPAY_QR', 'FAILED', 1280000.00, 'VNP20261102125004', NULL, '2026-11-02 16:00:00', '2026-11-02 16:15:00'),
(6, 6, 'COD', 'CASH_ON_DELIVERY', 'PENDING', 28000000.00, NULL, NULL, '2026-11-03 10:00:00', '2026-11-03 10:00:00');

-- 14. SHIPMENTS
INSERT INTO shipments (shipment_id, order_order_id, tracking_number, carrier_name, shipping_status, estimated_delivery, shipped_at, delivered_at, fail_reason, notes, version, updated_by_user_id, created_at, updated_at) VALUES
(1, 1, 'GHN-20261015089', 'Giao Hàng Nhanh Express', 'IN_TRANSIT', '2026-10-18 17:00:00', '2026-10-16 09:00:00', NULL, NULL, 'Kiện hàng nguyên seal, dán tem hàng giá trị cao', 0, 2, '2026-10-15 09:00:00', '2026-10-16 09:00:00'),
(2, 2, 'GHTK-9812401', 'Giao Hàng Tiết Kiệm', 'DELIVERED', '2026-10-23 18:00:00', '2026-10-21 08:30:00', '2026-10-23 15:30:00', NULL, 'Đã ký nhận: Hoàng Minh Khôi', 0, 2, '2026-10-20 10:00:00', '2026-10-23 15:30:00'),
(3, 3, 'VTP-4412903', 'Viettel Post', 'READY_TO_PICK', '2026-10-31 18:00:00', NULL, NULL, NULL, 'Đã đóng gói chờ bưu tá Viettel Post lấy hàng', 0, 3, '2026-10-28 15:30:00', '2026-10-28 15:30:00'),
(4, 4, 'GHN-20261101120', 'Giao Hàng Nhanh Express', 'READY_TO_PICK', '2026-11-05 17:00:00', NULL, NULL, NULL, 'Đã in phiếu vận đơn chuẩn bị bàn giao kho', 0, 2, '2026-11-01 11:45:00', '2026-11-01 11:45:00'),
(5, 6, 'VNP-7788990', 'VNPost Express', 'FAILED_DELIVERY', '2026-11-05 18:00:00', '2026-11-03 14:00:00', NULL, 'Khách hàng hẹn giao lại vào hôm sau do đi công tác', 'Lần giao 1 không thành công', 0, 3, '2026-11-03 10:30:00', '2026-11-04 15:00:00');

-- 15. ORDER STATUS HISTORY
INSERT INTO order_status_history (history_id, order_order_id, changed_by_user_id, previous_status, new_status, note, created_at) VALUES
(1, 1, 4, NULL, 'PENDING_PAYMENT', 'Khách hàng tạo đơn hàng qua cổng trực tuyến', '2026-10-15 08:30:00'),
(2, 1, 1, 'PENDING_PAYMENT', 'CONFIRMED', 'Hệ thống tự động xác nhận sau khi nhận thanh toán VNPAY thành công', '2026-10-15 08:35:00'),
(3, 1, 2, 'CONFIRMED', 'SHIPPING', 'Nhân viên đã bàn giao kiện hàng cho GHN Express', '2026-10-16 09:00:00'),
(4, 2, 5, NULL, 'PENDING_PAYMENT', 'Khách hàng đặt hàng', '2026-10-20 09:15:00'),
(5, 2, 1, 'PENDING_PAYMENT', 'CONFIRMED', 'Xác nhận thanh toán MOMO thành công', '2026-10-20 09:20:00'),
(6, 2, 2, 'CONFIRMED', 'SHIPPING', 'Bàn giao cho GHTK', '2026-10-21 08:30:00'),
(7, 2, 2, 'SHIPPING', 'DELIVERED', 'Khách hàng đã nhận hàng thành công', '2026-10-23 15:30:00');

-- 16. AUDIT LOGS
DELETE FROM audit_logs;
INSERT INTO audit_logs (log_id, user_user_id, action, entity_name, entity_id, old_value, new_value, created_at) VALUES
(1, 1, 'CREATE_BRAND', 'Brand', 1, NULL, 'Apple', '2026-01-01 00:00:00'),
(2, 1, 'CREATE_CATEGORY', 'Category', 1, NULL, 'Điện thoại di động', '2026-01-01 00:00:00'),
(3, 1, 'CREATE_INTERNAL_USER', 'User', 2, NULL, 'Nguyễn Văn Thắng (STAFF)', '2026-02-01 09:00:00'),
(4, 1, 'CREATE_INTERNAL_USER', 'User', 3, NULL, 'Lê Thị Mai (STAFF)', '2026-02-15 09:30:00'),
(5, 1, 'LOCK_USER', 'User', 10, 'ACTIVE', 'LOCKED', '2026-05-06 14:00:00');

-- 17. NOTIFICATIONS
INSERT INTO notifications (notification_id, user_user_id, order_order_id, title, content, is_read, created_at) VALUES
(1, 4, 1, 'Đơn hàng đang được giao', 'Đơn hàng DH-20261015-089 đã được bàn giao cho đơn vị vận chuyển GHN Express.', b'1', '2026-10-16 09:05:00'),
(2, 5, 2, 'Giao hàng thành công', 'Đơn hàng DH-20261020-102 đã được giao thành công. Cảm ơn bạn đã tin tưởng NextGen!', b'1', '2026-10-23 15:35:00'),
(3, 9, 6, 'Giao hàng không thành công', 'Đơn hàng DH-20261103-130 chưa giao thành công. Bưu tá sẽ liên hệ giao lại cho bạn vào ngày mai.', b'0', '2026-11-04 15:05:00');

SET FOREIGN_KEY_CHECKS = 1;
