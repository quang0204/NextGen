-- ====================================================================
-- NextGen E-Commerce Comprehensive Seed Data (27 Tables)
-- Database: NextGen (MySQL)
-- Accounts for testing:
--   ADMIN:    trinhvanquang02042004@gmail.com / Admin@123
--   STAFF:    hoang.kythuat@phoneshop.vn      / Password@123
--             staff.thang@nextgen.vn          / Password@123
--   CUSTOMER: huyen.tran@phoneshop.vn (ID 105) / Password@123
--             huyen.tran@gmail.com (ID 4)      / Password@123
-- ====================================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- --------------------------------------------------------------------
-- 1. USERS
-- --------------------------------------------------------------------
DELETE FROM users;
INSERT INTO users (user_id, full_name, email, phone, password_hash, role, status, lock_reason, created_at, updated_at) VALUES
(1, 'Trịnh Văn Quang', 'trinhvanquang02042004@gmail.com', '0989801827', '$2a$10$KjY9JZVDMt10IXI9V5asAehIlnf64YYaZasuiXanzSD00Fj28ggme', 'ADMIN', 'ACTIVE', NULL, '2026-01-01 08:00:00', '2026-01-01 08:00:00'),
(2, 'Nguyễn Văn Thắng', 'staff.thang@nextgen.vn', '0912345678', '$2a$10$lKifkavs5h2GbVPkX65AHuBBJf8qFtuqHfv0FA/JYIUShTBkbySvm', 'STAFF', 'ACTIVE', NULL, '2026-02-01 09:00:00', '2026-02-01 09:00:00'),
(3, 'Lê Thị Mai', 'staff.mai@nextgen.vn', '0923456789', '$2a$10$lKifkavs5h2GbVPkX65AHuBBJf8qFtuqHfv0FA/JYIUShTBkbySvm', 'STAFF', 'ACTIVE', NULL, '2026-02-15 09:30:00', '2026-02-15 09:30:00'),
(4, 'Trần Thị Thu Huyền', 'huyen.tran@gmail.com', '0987654321', '$2a$10$lKifkavs5h2GbVPkX65AHuBBJf8qFtuqHfv0FA/JYIUShTBkbySvm', 'CUSTOMER', 'ACTIVE', NULL, '2026-03-01 10:00:00', '2026-03-01 10:00:00'),
(5, 'Hoàng Minh Khôi', 'khoi.hoang@gmail.com', '0971234567', '$2a$10$lKifkavs5h2GbVPkX65AHuBBJf8qFtuqHfv0FA/JYIUShTBkbySvm', 'CUSTOMER', 'ACTIVE', NULL, '2026-03-10 11:20:00', '2026-03-10 11:20:00'),
(6, 'Phạm Đức Anh', 'anh.pham@gmail.com', '0962345678', '$2a$10$lKifkavs5h2GbVPkX65AHuBBJf8qFtuqHfv0FA/JYIUShTBkbySvm', 'CUSTOMER', 'ACTIVE', NULL, '2026-03-15 14:15:00', '2026-03-15 14:15:00'),
(7, 'Đỗ Bảo Ngọc', 'ngoc.do@gmail.com', '0933456789', '$2a$10$lKifkavs5h2GbVPkX65AHuBBJf8qFtuqHfv0FA/JYIUShTBkbySvm', 'CUSTOMER', 'ACTIVE', NULL, '2026-04-01 08:45:00', '2026-04-01 08:45:00'),
(8, 'Vũ Tuấn Kiệt', 'kiet.vu@gmail.com', '0944567890', '$2a$10$lKifkavs5h2GbVPkX65AHuBBJf8qFtuqHfv0FA/JYIUShTBkbySvm', 'CUSTOMER', 'ACTIVE', NULL, '2026-04-10 16:30:00', '2026-04-10 16:30:00'),
(9, 'Bùi Phương Thảo', 'thao.bui@gmail.com', '0955678901', '$2a$10$lKifkavs5h2GbVPkX65AHuBBJf8qFtuqHfv0FA/JYIUShTBkbySvm', 'CUSTOMER', 'ACTIVE', NULL, '2026-05-01 10:10:00', '2026-05-01 10:10:00'),
(10, 'Đinh Quang Huy', 'huy.dinh@gmail.com', '0901234567', '$2a$10$lKifkavs5h2GbVPkX65AHuBBJf8qFtuqHfv0FA/JYIUShTBkbySvm', 'CUSTOMER', 'LOCKED', 'Vi phạm điều khoản cộng đồng: spam đánh giá ảo', '2026-05-05 13:00:00', '2026-05-06 14:00:00'),
(105, 'Trần Thu Huyền', 'huyen.tran@phoneshop.vn', '0987654321', '$2a$10$lKifkavs5h2GbVPkX65AHuBBJf8qFtuqHfv0FA/JYIUShTBkbySvm', 'CUSTOMER', 'ACTIVE', NULL, '2026-03-01 10:00:00', '2026-03-01 10:00:00'),
(201, 'Hoàng Kỹ Thuật', 'hoang.kythuat@phoneshop.vn', '0911223344', '$2a$10$lKifkavs5h2GbVPkX65AHuBBJf8qFtuqHfv0FA/JYIUShTBkbySvm', 'STAFF', 'ACTIVE', NULL, '2026-02-01 08:30:00', '2026-02-01 08:30:00');

-- --------------------------------------------------------------------
-- 2. USER_ADDRESSES
-- --------------------------------------------------------------------
DELETE FROM user_addresses;
INSERT INTO user_addresses (address_id, user_user_id, recipient_name, phone, address_line, is_default, created_at, updated_at) VALUES
(1, 4, 'Trần Thị Thu Huyền', '0987654321', 'Số 12 ngõ 45 phố Trần Thái Tông, Dịch Vọng Hậu, Cầu Giấy, Hà Nội', b'1', '2026-03-01 10:05:00', '2026-03-01 10:05:00'),
(2, 4, 'Trần Thị Thu Huyền', '0987654321', 'Tòa nhà Landmark 81, Phường 22, Bình Thạnh, TP. Hồ Chí Minh', b'0', '2026-04-02 11:00:00', '2026-04-02 11:00:00'),
(3, 5, 'Hoàng Minh Khôi', '0971234567', 'Số 88 đường Nguyễn Văn Linh, Phường Nam Dương, Hải Châu, Đà Nẵng', b'1', '2026-03-10 11:25:00', '2026-03-10 11:25:00'),
(4, 6, 'Phạm Đức Anh', '0962345678', 'Số 250 đường Lạch Tray, Quận Ngô Quyền, Hải Phòng', b'1', '2026-03-15 14:20:00', '2026-03-15 14:20:00'),
(5, 7, 'Đỗ Bảo Ngọc', '0933456789', 'Số 18 đường 30 Tháng 4, Phường An Phú, Ninh Kiều, Cần Thơ', b'1', '2026-04-01 08:50:00', '2026-04-01 08:50:00'),
(6, 8, 'Vũ Tuấn Kiệt', '0944567890', 'Số 105 phố Bà Triệu, Phường Hàng Bài, Hoàn Kiếm, Hà Nội', b'1', '2026-04-10 16:35:00', '2026-04-10 16:35:00'),
(7, 9, 'Bùi Phương Thảo', '0955678901', 'Căn hộ B12-08 Sunrise City, Quận 7, TP. Hồ Chí Minh', b'1', '2026-05-01 10:15:00', '2026-05-01 10:15:00'),
(8, 105, 'Trần Thu Huyền', '0987654321', 'Số 12 ngõ 45 phố Trần Thái Tông, Dịch Vọng Hậu, Cầu Giấy, Hà Nội', b'1', '2026-03-01 10:05:00', '2026-03-01 10:05:00');

-- --------------------------------------------------------------------
-- 3. BRANDS
-- --------------------------------------------------------------------
DELETE FROM brands;
INSERT INTO brands (brand_id, brand_name, slug, description, logo_url, is_active, created_at, updated_at) VALUES
(1, 'Apple', 'apple', 'Thương hiệu công nghệ hàng đầu thế giới với hệ sinh thái iPhone, iPad, Mac', 'https://cdn.nextgen.vn/brands/apple.png', b'1', '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
(2, 'Samsung', 'samsung', 'Tập đoàn điện tử công nghệ đa quốc gia từ Hàn Quốc', 'https://cdn.nextgen.vn/brands/samsung.png', b'1', '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
(3, 'Xiaomi', 'xiaomi', 'Hệ sinh thái thiết bị thông minh và flagship cấu hình cao giá tốt', 'https://cdn.nextgen.vn/brands/xiaomi.png', b'1', '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
(4, 'Sony', 'sony', 'Âm thanh chuyên nghiệp đỉnh cao và thiết bị nhiếp ảnh hàng đầu', 'https://cdn.nextgen.vn/brands/sony.png', b'1', '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
(5, 'Asus', 'asus', 'Nhà sản xuất bo mạch chủ, laptop gaming ROG và linh kiện PC cao cấp', 'https://cdn.nextgen.vn/brands/asus.png', b'1', '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
(6, 'Anker', 'anker', 'Thương hiệu phụ kiện sạc nhanh, pin dự phòng và audio số 1 toàn cầu', 'https://cdn.nextgen.vn/brands/anker.png', b'1', '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
(7, 'Marshall', 'marshall', 'Huyền thoại âm thanh vintage từ Anh Quốc với chất âm rock đặc trưng', 'https://cdn.nextgen.vn/brands/marshall.png', b'1', '2026-01-01 00:00:00', '2026-01-01 00:00:00');

-- --------------------------------------------------------------------
-- 4. CATEGORIES
-- --------------------------------------------------------------------
DELETE FROM categories;
INSERT INTO categories (category_id, parent_category_id, category_name, slug, description, icon_url, is_active, created_at, updated_at) VALUES
(1, NULL, 'Điện thoại di động', 'dien-thoai-di-dong', 'Các dòng smartphone chính hãng bảo hành điện tử', 'https://cdn.nextgen.vn/categories/phone.svg', b'1', '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
(2, NULL, 'Máy tính bảng', 'may-tinh-bang', 'Tablet học tập, làm việc sáng tạo chuyên nghiệp', 'https://cdn.nextgen.vn/categories/tablet.svg', b'1', '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
(3, NULL, 'Laptop & PC', 'laptop-pc', 'Laptop văn phòng mỏng nhẹ, laptop gaming đồ họa cao', 'https://cdn.nextgen.vn/categories/laptop.svg', b'1', '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
(4, NULL, 'Phụ kiện & Âm thanh', 'phu-kien-am-thanh', 'Tai nghe, loa bluetooth, củ cáp sạc nhanh', 'https://cdn.nextgen.vn/categories/accessory.svg', b'1', '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
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

-- --------------------------------------------------------------------
-- 5. PRODUCTS
-- --------------------------------------------------------------------
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
(9, 14, 6, 'Củ sạc Anker GaNPrime 65W', 'cu-sac-anker-ganprime-65w', 'Công nghệ sạc nhanh GaNPrime độc quyền, 2 cổng Type-C + 1 cổng USB-A tiện lợi', 'https://cdn.nextgen.vn/products/anker-ganprime-65w.png', 1250000.00, 'ACTIVE', '2026-03-01 09:00:00', '2026-03-01 09:00:00'),
(10, 5, 1, 'iPhone 15 Pro Max', 'iphone-15-pro-max', 'Khung Titan cao cấp thế hệ đầu tiên, camera tiềm vọng zoom quang học 5x', 'https://cdn.nextgen.vn/products/iphone-15-pro-max-natural.png', 29990000.00, 'ACTIVE', '2026-01-15 08:00:00', '2026-01-15 08:00:00');

-- --------------------------------------------------------------------
-- 6. PRODUCT_VARIANTS
-- --------------------------------------------------------------------
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
(11, 9, 'Củ sạc Anker GaNPrime 65W 3 cổng - Đen', 'ANKER-GAN-65W-BLK', 1250000.00, 1490000.00, 'Đen Nhám', 0, 0, 'https://cdn.nextgen.vn/variants/anker-65w-blk.png', 'ACTIVE', '2026-03-01 09:00:00', '2026-03-01 09:00:00'),
(101, 10, 'iPhone 15 Pro Max 256GB - Titan Tự Nhiên', 'IP15PM-256-TI', 29990000.00, 32990000.00, 'Titan Tự Nhiên', 8, 256, 'https://cdn.nextgen.vn/variants/ip15pm-ti.png', 'ACTIVE', '2026-01-15 08:30:00', '2026-01-15 08:30:00');

-- --------------------------------------------------------------------
-- 7. PRODUCT_IMAGES
-- --------------------------------------------------------------------
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
(10, 9, 'https://cdn.nextgen.vn/products/anker-ganprime-65w.png', b'1', 1, '2026-03-01 09:00:00'),
(11, 10, 'https://cdn.nextgen.vn/products/iphone-15-pro-max-natural.png', b'1', 1, '2026-01-15 08:00:00');

-- --------------------------------------------------------------------
-- 8. PRODUCT_SPECS
-- --------------------------------------------------------------------
DELETE FROM product_specs;
INSERT INTO product_specs (spec_id, product_product_id, spec_name, spec_value, created_at) VALUES
(1, 1, 'Màn hình', '6.9 inch Super Retina XDR OLED, 120Hz ProMotion', '2026-02-01 10:00:00'),
(2, 1, 'Vi xử lý', 'Apple A18 Pro (3nm thế hệ thứ 2)', '2026-02-01 10:00:00'),
(3, 1, 'Camera chính', '48MP Fusion + 48MP Ultra-wide + 12MP Tele 5x', '2026-02-01 10:00:00'),
(4, 3, 'Màn hình', '6.8 inch Dynamic AMOLED 2X, 120Hz, Gorilla Glass Armor', '2026-02-02 09:00:00'),
(5, 3, 'Vi xử lý', 'Snapdragon 8 Gen 3 for Galaxy', '2026-02-02 09:00:00'),
(6, 10, 'Màn hình', '6.7 inch Super Retina XDR OLED 120Hz', '2026-01-15 08:00:00'),
(7, 10, 'Vi xử lý', 'Apple A17 Pro (3nm)', '2026-01-15 08:00:00');

-- --------------------------------------------------------------------
-- 9. INVENTORIES
-- --------------------------------------------------------------------
DELETE FROM inventories;
INSERT INTO inventories (inventory_id, variant_variant_id, quantity, reserved_quantity, min_threshold, updated_at) VALUES
(1, 1, 45, 2, 5, '2026-11-01 10:00:00'),
(2, 2, 30, 0, 5, '2026-11-01 10:00:00'),
(3, 3, 20, 1, 5, '2026-11-01 10:00:00'),
(4, 4, 60, 0, 5, '2026-11-01 10:00:00'),
(5, 5, 35, 1, 5, '2026-11-01 10:00:00'),
(6, 6, 25, 0, 5, '2026-11-01 10:00:00'),
(7, 7, 40, 0, 5, '2026-11-01 10:00:00'),
(8, 8, 15, 0, 5, '2026-11-01 10:00:00'),
(9, 9, 50, 1, 5, '2026-11-01 10:00:00'),
(10, 10, 30, 1, 5, '2026-11-01 10:00:00'),
(11, 11, 100, 2, 5, '2026-11-01 10:00:00'),
(12, 101, 8, 4, 5, '2026-11-01 10:00:00');

-- --------------------------------------------------------------------
-- 10. PROMOTIONS
-- --------------------------------------------------------------------
DELETE FROM promotion_variants;
DELETE FROM promotions;
INSERT INTO promotions (promotion_id, promotion_name, description, discount_type, discount_value, is_active, start_date, end_date, created_by_user_id, created_at, updated_at) VALUES
(1, 'Tuần Lễ Flash Sale Flagship 2026', 'Chiến dịch giảm sốc các dòng điện thoại và phụ kiện cao cấp', 'PERCENTAGE', 10.00, b'1', '2026-11-01 00:00:00', '2026-11-30 23:59:59', 1, '2026-10-25 09:00:00', '2026-10-25 09:00:00');

-- --------------------------------------------------------------------
-- 11. PROMOTION_VARIANTS
-- --------------------------------------------------------------------
INSERT INTO promotion_variants (promo_variant_id, promotion_promotion_id, variant_variant_id, promotional_price, created_at) VALUES
(1, 1, 1, 33990000.00, '2026-10-25 09:30:00'),
(2, 1, 5, 29990000.00, '2026-10-25 09:30:00'),
(3, 1, 9, 7990000.00, '2026-10-25 09:30:00'),
(4, 1, 101, 28990000.00, '2026-10-25 09:30:00');

-- --------------------------------------------------------------------
-- 12. VOUCHERS
-- --------------------------------------------------------------------
DELETE FROM vouchers;
INSERT INTO vouchers (voucher_id, voucher_code, voucher_name, discount_type, discount_value, min_order_value, max_discount_amount, usage_limit, used_count, is_active, start_date, end_date, created_at, updated_at) VALUES
(1, 'NEXTGEN500', 'Voucher Giảm 500K Cho Flagship', 'FIXED_AMOUNT', 500000.00, 20000000.00, 500000.00, 100, 12, b'1', '2026-10-01 00:00:00', '2026-12-31 23:59:59', '2026-10-01 00:00:00', '2026-10-01 00:00:00'),
(2, 'FREESHIP30', 'Miễn Phí Vận Chuyển Toàn Quốc', 'FIXED_AMOUNT', 30000.00, 500000.00, 30000.00, 500, 85, b'1', '2026-10-01 00:00:00', '2026-12-31 23:59:59', '2026-10-01 00:00:00', '2026-10-01 00:00:00'),
(3, 'TECHNEW10', 'Giảm 10% Cho Phụ Kiện', 'PERCENTAGE', 10.00, 1000000.00, 200000.00, 200, 40, b'1', '2026-10-01 00:00:00', '2026-12-31 23:59:59', '2026-10-01 00:00:00', '2026-10-01 00:00:00');

-- --------------------------------------------------------------------
-- 13. CARTS
-- --------------------------------------------------------------------
DELETE FROM cart_items;
DELETE FROM carts;
INSERT INTO carts (cart_id, user_user_id, created_at, updated_at) VALUES
(1, 4, '2026-03-01 10:10:00', '2026-11-04 14:00:00'),
(2, 5, '2026-03-10 11:30:00', '2026-11-04 14:30:00'),
(3, 105, '2026-03-01 10:10:00', '2026-11-04 14:00:00');

-- --------------------------------------------------------------------
-- 14. CART_ITEMS
-- --------------------------------------------------------------------
INSERT INTO cart_items (cart_item_id, cart_cart_id, variant_variant_id, quantity, created_at, updated_at) VALUES
(1, 1, 1, 1, '2026-11-04 10:00:00', '2026-11-04 10:00:00'),
(2, 1, 9, 1, '2026-11-04 10:15:00', '2026-11-04 10:15:00'),
(3, 2, 11, 2, '2026-11-04 14:30:00', '2026-11-04 14:30:00'),
(4, 3, 1, 1, '2026-11-04 10:00:00', '2026-11-04 10:00:00');

-- --------------------------------------------------------------------
-- 15. ORDERS
-- --------------------------------------------------------------------
DELETE FROM order_items;
DELETE FROM shipments;
DELETE FROM payments;
DELETE FROM order_status_history;
DELETE FROM notifications;
DELETE FROM inventory_transactions;
DELETE FROM return_requests;
DELETE FROM reviews;
DELETE FROM support_tickets;
DELETE FROM wishlists;
DELETE FROM orders;

INSERT INTO orders (order_id, order_code, user_user_id, recipient_name, recipient_phone, shipping_address, total_amount, shipping_fee, discount_amount, final_amount, payment_method, payment_status, status, note, cancel_reason, voucher_voucher_id, completed_at, created_at, updated_at) VALUES
(1, 'DH-20261015-089', 4, 'Trần Thị Thu Huyền', '0987654321', 'Số 12 ngõ 45 phố Trần Thái Tông, Dịch Vọng Hậu, Cầu Giấy, Hà Nội', 34990000.00, 0.00, 500000.00, 34490000.00, 'BANK_TRANSFER', 'PAID', 'SHIPPING', 'Giao hàng giờ hành chính', NULL, 1, NULL, '2026-10-15 08:30:00', '2026-10-16 10:00:00'),
(2, 'DH-20261020-102', 5, 'Hoàng Minh Khôi', '0971234567', 'Số 88 đường Nguyễn Văn Linh, Phường Nam Dương, Hải Châu, Đà Nẵng', 31990000.00, 0.00, 0.00, 31990000.00, 'BANK_TRANSFER', 'PAID', 'COMPLETED', 'Gọi trước khi giao', NULL, NULL, '2026-10-23 15:30:00', '2026-10-20 09:15:00', '2026-10-23 15:30:00'),
(3, 'DH-20261028-115', 6, 'Phạm Đức Anh', '0962345678', 'Số 250 đường Lạch Tray, Quận Ngô Quyền, Hải Phòng', 3590000.00, 30000.00, 30000.00, 3590000.00, 'COD', 'PENDING', 'CONFIRMED', 'Kiểm tra hàng trước khi thanh toán COD', NULL, 2, NULL, '2026-10-28 14:00:00', '2026-10-28 15:00:00'),
(4, 'DH-20261101-120', 7, 'Đỗ Bảo Ngọc', '0933456789', 'Số 18 đường 30 Tháng 4, Phường An Phú, Ninh Kiều, Cần Thơ', 8490000.00, 0.00, 0.00, 8490000.00, 'BANK_TRANSFER', 'PAID', 'PROCESSING', 'Đóng gói cẩn thận giúp mình', NULL, NULL, NULL, '2026-11-01 11:20:00', '2026-11-01 11:30:00'),
(5, 'DH-20261102-125', 8, 'Vũ Tuấn Kiệt', '0944567890', 'Số 105 phố Bà Triệu, Phường Hàng Bài, Hoàn Kiếm, Hà Nội', 1250000.00, 30000.00, 0.00, 1280000.00, 'BANK_TRANSFER', 'FAILED', 'CANCELLED', 'Huỷ đơn', 'Khách hàng đổi ý, đặt nhầm màu sản phẩm', NULL, NULL, '2026-11-02 16:00:00', '2026-11-02 16:45:00'),
(6, 'DH-20261103-130', 9, 'Bùi Phương Thảo', '0955678901', 'Căn hộ B12-08 Sunrise City, Quận 7, TP. Hồ Chí Minh', 28500000.00, 0.00, 500000.00, 28000000.00, 'COD', 'PENDING', 'SHIPPING', 'Giao buổi chiều sau 14h', NULL, 1, NULL, '2026-11-03 10:00:00', '2026-11-04 09:00:00'),
-- Đơn hàng 210 chuẩn spec Nhóm 11 để test đổi trả:
(210, 'ORD-20261102-004', 105, 'Trần Thu Huyền', '0987654321', 'Số 12 ngõ 45 phố Trần Thái Tông, Dịch Vọng Hậu, Cầu Giấy, Hà Nội', 29990000.00, 0.00, 0.00, 29990000.00, 'COD', 'PAID', 'COMPLETED', 'Đơn hàng giao thành công, trong hạn 30 ngày', NULL, NULL, '2026-11-02 15:00:00', '2026-11-01 09:00:00', '2026-11-02 15:00:00'),
-- Đơn hàng 211 để test quá hạn 30 ngày (RETURN_PERIOD_EXPIRED):
(211, 'ORD-20260901-001', 105, 'Trần Thu Huyền', '0987654321', 'Số 12 ngõ 45 phố Trần Thái Tông, Dịch Vọng Hậu, Cầu Giấy, Hà Nội', 19990000.00, 0.00, 0.00, 19990000.00, 'BANK_TRANSFER', 'PAID', 'COMPLETED', 'Đơn hàng cũ đã hơn 60 ngày', NULL, NULL, '2026-09-01 10:00:00', '2026-08-28 10:00:00', '2026-09-01 10:00:00'),
-- Đơn hàng 212 để test trường hợp thanh toán BANK_TRANSFER hợp lệ:
(212, 'ORD-20261105-002', 4, 'Trần Thị Thu Huyền', '0987654321', 'Số 12 ngõ 45 phố Trần Thái Tông, Dịch Vọng Hậu, Cầu Giấy, Hà Nội', 34990000.00, 0.00, 0.00, 34990000.00, 'BANK_TRANSFER', 'PAID', 'COMPLETED', 'Đã nhận máy ngày 05/11', NULL, NULL, '2026-11-05 14:00:00', '2026-11-04 10:00:00', '2026-11-05 14:00:00');

-- --------------------------------------------------------------------
-- 16. ORDER_ITEMS
-- --------------------------------------------------------------------
INSERT INTO order_items (order_item_id, order_order_id, variant_variant_id, sku_snapshot, product_name_snapshot, color_snapshot, rom_snapshot, image_url, price, quantity, total_price, created_at) VALUES
(1, 1, 1, 'IP16PM-256-DESERT', 'iPhone 16 Pro Max', 'Titan Sa Mạc', '256GB', 'https://cdn.nextgen.vn/variants/ip16pm-desert.png', 34990000.00, 1, 34990000.00, '2026-10-15 08:30:00'),
(2, 2, 5, 'S24U-256-GRAY', 'Galaxy S24 Ultra', 'Xám Titan', '256GB', 'https://cdn.nextgen.vn/variants/s24u-gray.png', 31990000.00, 1, 31990000.00, '2026-10-20 09:15:00'),
(3, 3, 10, 'MARSHALL-EMB2-BRASS', 'Marshall Emberton II', 'Black and Brass', 'N/A', 'https://cdn.nextgen.vn/variants/marshall-emb2.png', 3590000.00, 1, 3590000.00, '2026-10-28 14:00:00'),
(4, 4, 9, 'SONY-XM5-BLACK', 'Sony WH-1000XM5', 'Đen Nhám', 'N/A', 'https://cdn.nextgen.vn/variants/sony-xm5-black.png', 8490000.00, 1, 8490000.00, '2026-11-01 11:20:00'),
(5, 5, 11, 'ANKER-GAN-65W-BLK', 'Củ sạc Anker GaNPrime 65W', 'Đen Nhám', 'N/A', 'https://cdn.nextgen.vn/variants/anker-65w-blk.png', 1250000.00, 1, 1250000.00, '2026-11-02 16:00:00'),
(6, 6, 7, 'IPADAIR-M2-128-GRAY', 'iPad Air M2 11 inch', 'Xám Không Gian', '128GB', 'https://cdn.nextgen.vn/variants/ipad-air-gray.png', 16990000.00, 1, 16990000.00, '2026-11-03 10:00:00'),
(7, 6, 11, 'ANKER-GAN-65W-BLK', 'Củ sạc Anker GaNPrime 65W', 'Đen Nhám', 'N/A', 'https://cdn.nextgen.vn/variants/anker-65w-blk.png', 1250000.00, 1, 1250000.00, '2026-11-03 10:00:00'),
(8, 210, 101, 'IP15PM-256-TI', 'iPhone 15 Pro Max', 'Titan Tự Nhiên', '256GB', 'https://cdn.nextgen.vn/variants/ip15pm-ti.png', 29990000.00, 1, 29990000.00, '2026-11-01 09:00:00'),
(9, 211, 4, 'IP15-128-PINK', 'iPhone 15', 'Hồng Pastel', '128GB', 'https://cdn.nextgen.vn/variants/ip15-pink.png', 19990000.00, 1, 19990000.00, '2026-08-28 10:00:00'),
(10, 212, 1, 'IP16PM-256-DESERT', 'iPhone 16 Pro Max', 'Titan Sa Mạc', '256GB', 'https://cdn.nextgen.vn/variants/ip16pm-desert.png', 34990000.00, 1, 34990000.00, '2026-11-04 10:00:00');

-- --------------------------------------------------------------------
-- 17. PAYMENTS
-- --------------------------------------------------------------------
INSERT INTO payments (payment_id, order_order_id, payment_method, payment_provider, status, amount, transaction_ref, paid_at, created_at, updated_at) VALUES
(1, 1, 'BANK_TRANSFER', 'VIETCOMBANK', 'SUCCESS', 34490000.00, 'VCB20261015089001', '2026-10-15 08:35:00', '2026-10-15 08:30:00', '2026-10-15 08:35:00'),
(2, 2, 'BANK_TRANSFER', 'VIETCOMBANK', 'SUCCESS', 31990000.00, 'VCB20261020102002', '2026-10-20 09:20:00', '2026-10-20 09:15:00', '2026-10-20 09:20:00'),
(3, 3, 'COD', 'CASH_ON_DELIVERY', 'PENDING', 3590000.00, NULL, NULL, '2026-10-28 14:00:00', '2026-10-28 14:00:00'),
(4, 4, 'BANK_TRANSFER', 'VIETCOMBANK', 'SUCCESS', 8490000.00, 'VCB20261101120003', '2026-11-01 11:25:00', '2026-11-01 11:20:00', '2026-11-01 11:25:00'),
(5, 5, 'BANK_TRANSFER', 'VIETCOMBANK', 'FAILED', 1280000.00, 'VCB20261102125004', NULL, '2026-11-02 16:00:00', '2026-11-02 16:15:00'),
(6, 6, 'COD', 'CASH_ON_DELIVERY', 'PENDING', 28000000.00, NULL, NULL, '2026-11-03 10:00:00', '2026-11-03 10:00:00'),
(7, 210, 'COD', 'CASH_ON_DELIVERY', 'SUCCESS', 29990000.00, 'COD20261102210', '2026-11-02 15:00:00', '2026-11-01 09:00:00', '2026-11-02 15:00:00'),
(8, 211, 'BANK_TRANSFER', 'VIETCOMBANK', 'SUCCESS', 19990000.00, 'VCB20260901211', '2026-09-01 10:05:00', '2026-08-28 10:00:00', '2026-09-01 10:05:00'),
(9, 212, 'BANK_TRANSFER', 'VIETCOMBANK', 'SUCCESS', 34990000.00, 'VCB20261105212', '2026-11-05 14:05:00', '2026-11-04 10:00:00', '2026-11-05 14:05:00');

-- --------------------------------------------------------------------
-- 18. SHIPMENTS
-- --------------------------------------------------------------------
INSERT INTO shipments (shipment_id, order_order_id, tracking_number, carrier_name, shipping_status, estimated_delivery, shipped_at, delivered_at, fail_reason, notes, version, updated_by_user_id, created_at, updated_at) VALUES
(1, 1, 'GHN-20261015089', 'Giao Hàng Nhanh Express', 'IN_TRANSIT', '2026-10-18 17:00:00', '2026-10-16 09:00:00', NULL, NULL, 'Kiện hàng nguyên seal, dán tem hàng giá trị cao', 0, 2, '2026-10-15 09:00:00', '2026-10-16 09:00:00'),
(2, 2, 'GHTK-9812401', 'Giao Hàng Tiết Kiệm', 'DELIVERED', '2026-10-23 18:00:00', '2026-10-21 08:30:00', '2026-10-23 15:30:00', NULL, 'Đã ký nhận: Hoàng Minh Khôi', 0, 2, '2026-10-20 10:00:00', '2026-10-23 15:30:00'),
(3, 3, 'VTP-4412903', 'Viettel Post', 'READY_TO_PICK', '2026-10-31 18:00:00', NULL, NULL, NULL, 'Đã đóng gói chờ bưu tá Viettel Post lấy hàng', 0, 3, '2026-10-28 15:30:00', '2026-10-28 15:30:00'),
(4, 4, 'GHN-20261101120', 'Giao Hàng Nhanh Express', 'READY_TO_PICK', '2026-11-05 17:00:00', NULL, NULL, NULL, 'Đã in phiếu vận đơn chuẩn bị bàn giao kho', 0, 2, '2026-11-01 11:45:00', '2026-11-01 11:45:00'),
(5, 6, 'VNP-7788990', 'VNPost Express', 'FAILED_DELIVERY', '2026-11-05 18:00:00', '2026-11-03 14:00:00', NULL, 'Khách hàng hẹn giao lại vào hôm sau do đi công tác', 'Lần giao 1 không thành công', 0, 3, '2026-11-03 10:30:00', '2026-11-04 15:00:00'),
(6, 210, 'GHN-20261101210', 'Giao Hàng Nhanh Express', 'DELIVERED', '2026-11-02 18:00:00', '2026-11-01 14:00:00', '2026-11-02 15:00:00', NULL, 'Giao thành công, khách kiểm tra máy nguyên vẹn', 0, 2, '2026-11-01 11:00:00', '2026-11-02 15:00:00');

-- --------------------------------------------------------------------
-- 19. ORDER_STATUS_HISTORY
-- --------------------------------------------------------------------
INSERT INTO order_status_history (history_id, order_order_id, changed_by_user_id, previous_status, new_status, note, created_at) VALUES
(1, 1, 4, NULL, 'PENDING_PAYMENT', 'Khách hàng tạo đơn hàng qua cổng trực tuyến', '2026-10-15 08:30:00'),
(2, 1, 1, 'PENDING_PAYMENT', 'CONFIRMED', 'Hệ thống tự động xác nhận sau khi nhận thanh toán thành công', '2026-10-15 08:35:00'),
(3, 1, 2, 'CONFIRMED', 'SHIPPING', 'Nhân viên đã bàn giao kiện hàng cho GHN Express', '2026-10-16 09:00:00'),
(4, 2, 5, NULL, 'PENDING_PAYMENT', 'Khách hàng đặt hàng', '2026-10-20 09:15:00'),
(5, 2, 1, 'PENDING_PAYMENT', 'CONFIRMED', 'Xác nhận thanh toán thành công', '2026-10-20 09:20:00'),
(6, 2, 2, 'CONFIRMED', 'SHIPPING', 'Bàn giao cho GHTK', '2026-10-21 08:30:00'),
(7, 2, 2, 'SHIPPING', 'COMPLETED', 'Khách hàng đã nhận hàng thành công', '2026-10-23 15:30:00'),
(8, 210, 105, NULL, 'CONFIRMED', 'Đặt đơn COD thành công', '2026-11-01 09:00:00'),
(9, 210, 2, 'CONFIRMED', 'SHIPPING', 'Bàn giao GHN', '2026-11-01 14:00:00'),
(10, 210, 2, 'SHIPPING', 'COMPLETED', 'Giao hàng thành công', '2026-11-02 15:00:00');

-- --------------------------------------------------------------------
-- 20. INVENTORY_TRANSACTIONS
-- --------------------------------------------------------------------
DELETE FROM inventory_transactions;
INSERT INTO inventory_transactions (txn_id, variant_variant_id, order_order_id, performed_by_user_id, transaction_type, form_code, quantity, qty_before, qty_after, reason, created_at) VALUES
(1, 1, NULL, 1, 'IMPORT', 'MB01-20261001-0001', 50, 0, 50, 'Nhập lô hàng ban đầu từ Apple Authorized Distributor', '2026-10-01 08:00:00'),
(2, 5, NULL, 1, 'IMPORT', 'MB01-20261001-0002', 40, 0, 40, 'Nhập lô hàng Samsung S24 Ultra chính hãng', '2026-10-01 08:30:00'),
(3, 101, NULL, 1, 'IMPORT', 'MB01-20261010-0003', 10, 0, 10, 'Nhập kho iPhone 15 Pro Max', '2026-10-10 09:00:00'),
(4, 101, 210, 2, 'EXPORT', 'MB03-20261101-0012', -1, 10, 9, 'Xuất kho phục vụ đơn hàng ORD-20261102-004', '2026-11-01 11:30:00');

-- --------------------------------------------------------------------
-- 21. RETURN_REQUESTS (Nhóm 11)
-- --------------------------------------------------------------------
DELETE FROM return_requests;
INSERT INTO return_requests (return_id, order_order_id, requested_by_user_id, inspected_by_user_id, reason, status, refund_amount, evidence_images, bank_name, bank_account_number, bank_account_holder, inspection_result, reject_reason, admin_note, inspected_at, approved_at, created_at, updated_at) VALUES
(35, 210, 105, NULL, 'Màn hình xuất hiện sọc xanh sau 5 ngày sử dụng, loa thoại bị rè khi đàm thoại', 'PENDING', NULL, '["https://phoneshop.vn/uploads/returns/evidence-soc-man-hinh-1.jpg","https://phoneshop.vn/uploads/returns/evidence-soc-man-hinh-2.jpg"]', 'Vietcombank', '0123456789', 'TRAN THU HUYEN', NULL, NULL, NULL, NULL, NULL, '2026-11-03 21:10:00', '2026-11-03 21:10:00'),
(36, 212, 4, 201, 'Máy tự động sập nguồn liên tục khi cắm sạc pin', 'APPROVED', 34990000.00, '["https://phoneshop.vn/uploads/returns/evidence-power-1.jpg"]', NULL, NULL, NULL, 'Xác nhận lỗi phần cứng nguồn từ nhà sản xuất, đủ điều kiện hoàn kho MB04', NULL, 'Đã hoàn tiền vào tài khoản liên kết', '2026-11-06 14:00:00', '2026-11-06 14:00:00', '2026-11-06 09:00:00', '2026-11-06 14:00:00'),
(37, 2, 5, 201, 'Vỏ ngoài bị trầy xước nhẹ sau 10 ngày nhận hàng', 'REJECTED', NULL, '["https://phoneshop.vn/uploads/returns/evidence-scratch.jpg"]', NULL, NULL, NULL, 'Kiểm tra máy bị trầy xước do va quẹt từ phía người dùng sau khi đã nhận và ký biên bản giao hàng', 'Từ chối đổi trả do trầy xước ngoại quan phát sinh trong quá trình khách hàng sử dụng', 'Đã liên hệ giải thích chính sách cho khách', '2026-10-25 10:00:00', NULL, '2026-10-24 16:00:00', '2026-10-25 10:00:00');

-- --------------------------------------------------------------------
-- 22. REVIEWS
-- --------------------------------------------------------------------
DELETE FROM reviews;
INSERT INTO reviews (review_id, order_item_order_item_id, rating, comment, is_verified_purchase, status, reply, replied_at, created_at, updated_at) VALUES
(1, 2, 5, 'Sản phẩm tuyệt vời! Galaxy S24 Ultra chụp ảnh cực nét, màn hình không bị lóa khi ra nắng.', b'1', 'VISIBLE', 'NextGen chân thành cảm ơn bạn đã tin tưởng và ủng hộ!', '2026-10-24 10:00:00', '2026-10-24 09:00:00', '2026-10-24 10:00:00'),
(2, 3, 5, 'Chất âm Marshall đặc trưng, bass chắc, nghe acoustic cực hay. Giao hàng nhanh!', b'1', 'VISIBLE', NULL, NULL, '2026-10-29 11:00:00', '2026-10-29 11:00:00'),
(3, 8, 4, 'Máy dùng rất mượt và đẹp, pin trâu, tuy nhiên bị lỗi panel sọc xanh nên đang gửi bảo hành.', b'1', 'VISIBLE', 'Dạ NextGen đã tiếp nhận yêu cầu đổi trả và hỗ trợ quý khách ngay ạ!', '2026-11-03 21:30:00', '2026-11-03 20:00:00', '2026-11-03 21:30:00');

-- --------------------------------------------------------------------
-- 23. SUPPORT_TICKETS
-- --------------------------------------------------------------------
DELETE FROM support_tickets;
INSERT INTO support_tickets (ticket_id, user_user_id, handled_by_user_id, subject, description, priority, status, resolution_note, resolved_at, created_at, updated_at) VALUES
(1, 4, 2, 'Hỏi về thời gian áp dụng bảo hành', 'Cho mình hỏi sản phẩm iPhone kích hoạt bảo hành điện tử tính từ ngày nhận hàng đúng không?', 'NORMAL', 'RESOLVED', 'Đã giải thích qua điện thoại: bảo hành chính hãng Apple VN/A tính từ ngày xuất hóa đơn GTGT.', '2026-10-16 11:00:00', '2026-10-16 10:00:00', '2026-10-16 11:00:00'),
(2, 105, 201, 'Tư vấn quy trình đổi trả đơn ORD-20261102-004', 'Máy mình bị sọc xanh màn hình, mình đã gửi yêu cầu đổi trả thì bao lâu kỹ thuật viên phản hồi?', 'HIGH', 'IN_PROGRESS', NULL, NULL, '2026-11-03 21:15:00', '2026-11-03 21:20:00'),
(3, 6, NULL, 'Muốn đổi địa chỉ nhận hàng', 'Đơn hàng DH-20261028-115 mình muốn đổi địa chỉ sang số 150 Lạch Tray có được không?', 'MEDIUM', 'OPEN', NULL, NULL, '2026-10-28 14:30:00', '2026-10-28 14:30:00');

-- --------------------------------------------------------------------
-- 24. WISHLISTS
-- --------------------------------------------------------------------
DELETE FROM wishlists;
INSERT INTO wishlists (wishlist_id, user_user_id, product_product_id, created_at) VALUES
(1, 4, 1, '2026-03-01 10:20:00'),
(2, 4, 6, '2026-03-05 14:00:00'),
(3, 5, 4, '2026-03-11 09:00:00'),
(4, 105, 1, '2026-03-01 10:20:00'),
(5, 105, 10, '2026-03-02 11:00:00');

-- --------------------------------------------------------------------
-- 25. FAQS
-- --------------------------------------------------------------------
DELETE FROM faqs;
INSERT INTO faqs (faq_id, question, answer, category, display_order, is_active, created_at, updated_at) VALUES
(1, 'Chính sách đổi trả trong vòng bao nhiêu ngày?', 'NextGen áp dụng chính sách đổi mới hoặc hoàn tiền trong vòng 30 ngày kể từ ngày đơn hàng giao thành công (COMPLETED) đối với lỗi phần cứng từ nhà sản xuất.', 'RETURN_POLICY', 1, b'1', '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
(2, 'Điều kiện để được duyệt biên bản hoàn kho MB04 là gì?', 'Sản phẩm phải còn nguyên tem niêm phong, không bị vào nước, rơi vỡ hay có sự can thiệp phần cứng trái phép, và đã được kỹ thuật viên thẩm định xác nhận lỗi phần cứng.', 'RETURN_POLICY', 2, b'1', '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
(3, 'Thanh toán COD khi nhận hàng có được hoàn tiền về tài khoản ngân hàng không?', 'Có, đối với đơn hàng thanh toán COD, khi gửi yêu cầu đổi trả bạn chỉ cần cung cấp số tài khoản và ngân hàng thụ hưởng để hệ thống hoàn tiền sau khi biên bản MB04 được duyệt.', 'PAYMENT', 3, b'1', '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
(4, 'Thời gian giao hàng toàn quốc mất bao lâu?', 'Thời gian giao hàng từ 1-2 ngày đối với khu vực nội thành Hà Nội, TP.HCM và 3-4 ngày đối với các tỉnh thành khác.', 'SHIPPING', 4, b'1', '2026-01-01 00:00:00', '2026-01-01 00:00:00');

-- --------------------------------------------------------------------
-- 26. NOTIFICATIONS
-- --------------------------------------------------------------------
DELETE FROM notifications;
INSERT INTO notifications (notification_id, user_user_id, order_order_id, title, content, is_read, created_at) VALUES
(1, 4, 1, 'Đơn hàng đang được giao', 'Đơn hàng DH-20261015-089 đã được bàn giao cho đơn vị vận chuyển GHN Express.', b'1', '2026-10-16 09:05:00'),
(2, 5, 2, 'Giao hàng thành công', 'Đơn hàng DH-20261020-102 đã được giao thành công. Cảm ơn bạn đã tin tưởng NextGen!', b'1', '2026-10-23 15:35:00'),
(3, 9, 6, 'Giao hàng không thành công', 'Đơn hàng DH-20261103-130 chưa giao thành công. Bưu tá sẽ liên hệ giao lại cho bạn vào ngày mai.', b'0', '2026-11-04 15:05:00'),
(4, 105, 210, 'Tiếp nhận yêu cầu đổi trả #35', 'Yêu cầu đổi trả cho đơn hàng ORD-20261102-004 đã được tiếp nhận. Kỹ thuật viên sẽ liên hệ với bạn trong vòng 24h.', b'0', '2026-11-03 21:10:00');

-- --------------------------------------------------------------------
-- 27. AUDIT_LOGS
-- --------------------------------------------------------------------
DELETE FROM audit_logs;
INSERT INTO audit_logs (log_id, user_user_id, action, entity_name, entity_id, old_value, new_value, created_at) VALUES
(1, 1, 'CREATE_BRAND', 'Brand', 1, NULL, 'Apple', '2026-01-01 00:00:00'),
(2, 1, 'CREATE_CATEGORY', 'Category', 1, NULL, 'Điện thoại di động', '2026-01-01 00:00:00'),
(3, 1, 'CREATE_INTERNAL_USER', 'User', 2, NULL, 'Nguyễn Văn Thắng (STAFF)', '2026-02-01 09:00:00'),
(4, 1, 'CREATE_INTERNAL_USER', 'User', 3, NULL, 'Lê Thị Mai (STAFF)', '2026-02-15 09:30:00'),
(5, 1, 'LOCK_USER', 'User', 10, 'ACTIVE', 'LOCKED', '2026-05-06 14:00:00'),
(6, 105, 'CREATE_RETURN_REQUEST', 'ReturnRequests', 35, NULL, 'Tạo yêu cầu đổi trả cho đơn hàng #ORD-20261102-004', '2026-11-03 21:10:00'),
(7, 201, 'RETURN_APPROVED', 'ReturnRequests', 36, 'status: PENDING', 'status: APPROVED, refundAmount: 34990000', '2026-11-06 14:00:00');

SET FOREIGN_KEY_CHECKS = 1;
-- ====================================================================
-- End of Seed Data (27 Tables Initialized)
-- ====================================================================
