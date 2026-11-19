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
(1, 'Apple', 'apple', 'Thương hiệu công nghệ hàng đầu thế giới với hệ sinh thái iPhone, iPad, Mac', 'http://localhost:9000/brands/apple.png', b'1', '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
(2, 'Samsung', 'samsung', 'Tập đoàn điện tử công nghệ đa quốc gia từ Hàn Quốc', 'http://localhost:9000/brands/samsung.png', b'1', '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
(3, 'Xiaomi', 'xiaomi', 'Hệ sinh thái thiết bị thông minh và flagship cấu hình cao giá tốt', 'http://localhost:9000/brands/xiaomi.png', b'1', '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
(4, 'Sony', 'sony', 'Âm thanh chuyên nghiệp đỉnh cao và thiết bị nhiếp ảnh hàng đầu', 'http://localhost:9000/brands/sony.png', b'1', '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
(5, 'Asus', 'asus', 'Nhà sản xuất bo mạch chủ, laptop gaming ROG và linh kiện PC cao cấp', 'http://localhost:9000/brands/asus.png', b'1', '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
(6, 'Anker', 'anker', 'Thương hiệu phụ kiện sạc nhanh, pin dự phòng và audio số 1 toàn cầu', 'http://localhost:9000/brands/anker.png', b'1', '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
(7, 'Marshall', 'marshall', 'Huyền thoại âm thanh vintage từ Anh Quốc với chất âm rock đặc trưng', 'http://localhost:9000/brands/marshall.png', b'1', '2026-01-01 00:00:00', '2026-01-01 00:00:00');

-- --------------------------------------------------------------------
-- 4. CATEGORIES
-- --------------------------------------------------------------------
DELETE FROM categories;
INSERT INTO categories (category_id, parent_category_id, category_name, slug, description, variant_schema, is_active, created_at, updated_at) VALUES
(1, NULL, 'Điện thoại di động', 'dien-thoai-di-dong', 'Các dòng smartphone chính hãng bảo hành điện tử', '[{"key":"color","label":"Màu sắc","storage":"COLUMN","type":"text","required":true},{"key":"ramGb","label":"RAM","storage":"COLUMN","type":"select","options":[4,6,8,12,16],"unit":"GB","required":true},{"key":"romGb","label":"Bộ nhớ trong","storage":"COLUMN","type":"select","options":[64,128,256,512,1024],"unit":"GB","required":true},{"key":"edition","label":"Phiên bản","storage":"ATTRIBUTE","type":"select","options":["VN/A","Chính hãng","eSIM"],"required":false}]', b'1', '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
(2, NULL, 'Máy tính bảng', 'may-tinh-bang', 'Tablet học tập, làm việc sáng tạo chuyên nghiệp', '[{"key":"color","label":"Màu sắc","storage":"COLUMN","type":"text","required":true},{"key":"ramGb","label":"RAM","storage":"COLUMN","type":"select","options":[4,6,8,12,16],"unit":"GB","required":true},{"key":"romGb","label":"Bộ nhớ trong","storage":"COLUMN","type":"select","options":[64,128,256,512,1024],"unit":"GB","required":true},{"key":"connectionType","label":"Kết nối","storage":"COLUMN","type":"select","options":["Wifi Only","5G + Wifi","LTE"],"required":true},{"key":"edition","label":"Phiên bản","storage":"ATTRIBUTE","type":"select","options":["Chính hãng","eSIM"],"required":false}]', b'1', '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
(3, NULL, 'Laptop & PC', 'laptop-pc', 'Laptop văn phòng mỏng nhẹ, laptop gaming đồ họa cao', '[{"key":"color","label":"Màu sắc","storage":"COLUMN","type":"text","required":true},{"key":"cpu","label":"Vi xử lý (CPU)","storage":"ATTRIBUTE","type":"text","required":true},{"key":"ramGb","label":"RAM","storage":"COLUMN","type":"select","options":[8,16,32,64],"unit":"GB","required":true},{"key":"romGb","label":"Ổ cứng SSD","storage":"COLUMN","type":"select","options":[256,512,1024,2048],"unit":"GB","required":true},{"key":"gpu","label":"Card đồ họa (GPU)","storage":"ATTRIBUTE","type":"text","required":false},{"key":"screen","label":"Màn hình","storage":"ATTRIBUTE","type":"text","required":false}]', b'1', '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
(4, NULL, 'Phụ kiện & Âm thanh', 'phu-kien-am-thanh', 'Tai nghe, loa bluetooth, củ cáp sạc nhanh', '[{"key":"color","label":"Màu sắc","storage":"COLUMN","type":"text","required":true},{"key":"connectionType","label":"Chuẩn kết nối","storage":"COLUMN","type":"select","options":["USB-C","Lightning","USB-A","Bluetooth 5.3","3.5mm","MagSafe"],"required":true},{"key":"wattage","label":"Công suất","storage":"ATTRIBUTE","type":"text","required":false},{"key":"cableLength","label":"Độ dài dây","storage":"ATTRIBUTE","type":"text","required":false},{"key":"compatibleModel","label":"Thiết bị tương thích","storage":"ATTRIBUTE","type":"text","required":false}]', b'1', '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
(5, 1, 'iPhone (Apple)', 'iphone-apple', 'Điện thoại iPhone VN/A chính hãng phân phối tại Việt Nam', NULL, b'1', '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
(6, 1, 'Samsung Galaxy', 'samsung-galaxy', 'Dòng Galaxy S Ultra, Z Fold và Z Flip gập mở cao cấp', NULL, b'1', '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
(7, 1, 'Xiaomi', 'xiaomi', 'Smartphone Xiaomi 14 series, Redmi Note hiệu năng đỉnh', NULL, b'1', '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
(8, 2, 'iPad', 'ipad', 'iPad Pro M4, iPad Air M2 và iPad Gen 10 chính hãng', NULL, b'1', '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
(9, 2, 'Samsung Tab', 'samsung-tab', 'Galaxy Tab S9 Series màn hình Dynamic AMOLED 2X', NULL, b'1', '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
(10, 3, 'MacBook', 'macbook', 'MacBook Air, MacBook Pro chip Apple Silicon', NULL, b'1', '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
(11, 3, 'Laptop Gaming Asus ROG', 'laptop-gaming-asus-rog', 'Laptop gaming màn hình tần số quét cao, tản nhiệt buồng hơi', NULL, b'1', '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
(12, 4, 'Tai nghe Chống ồn', 'tai-nghe-chong-on', 'Tai nghe True Wireless và Over-ear chống ồn chủ động ANC', NULL, b'1', '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
(13, 4, 'Loa Bluetooth Di động', 'loa-bluetooth-di-dong', 'Loa kháng nước công suất lớn âm trầm uy lực', NULL, b'1', '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
(14, 4, 'Củ sạc & Cáp GaN', 'cu-sac-cap-gan', 'Công nghệ sạc bán dẫn GaN công suất cao nhỏ gọn', NULL, b'1', '2026-01-01 00:00:00', '2026-01-01 00:00:00');

-- --------------------------------------------------------------------
-- 5. PRODUCTS
-- --------------------------------------------------------------------
DELETE FROM products;
INSERT INTO products (product_id, category_category_id, brand_brand_id, product_name, slug, description, thumbnail_url, base_price, status, created_at, updated_at) VALUES
(1, 5, 1, 'iPhone 16 Pro Max', 'iphone-16-pro-max', 'Khung viền Titan sa mạc, nút điều khiển Camera Control, Chip A18 Pro mạnh mẽ nhất', 'http://localhost:9000/products/iphone-16-pro-max-desert.png', 34990000.00, 'ACTIVE', '2026-02-01 10:00:00', '2026-02-01 10:00:00'),
(2, 5, 1, 'iPhone 15', 'iphone-15', 'Màn hình Dynamic Island, cổng sạc Type-C, camera 48MP nâng cấp sắc nét', 'http://localhost:9000/products/iphone-15-pink.png', 19990000.00, 'ACTIVE', '2026-02-01 10:30:00', '2026-02-01 10:30:00'),
(3, 6, 2, 'Samsung Galaxy S24 Ultra', 'samsung-galaxy-s24-ultra', 'Khung viền Titan, màn hình phẳng chống chói, tích hợp bút S-Pen và quyền năng Galaxy AI', 'http://localhost:9000/products/galaxy-s24-ultra-gray.png', 31990000.00, 'ACTIVE', '2026-02-02 09:00:00', '2026-02-02 09:00:00'),
(4, 7, 3, 'Xiaomi 14 Ultra', 'xiaomi-14-ultra', 'Cụm 4 ống kính quang học Leica Summilux, cảm biến 1-inch LYT-900 đỉnh cao nhiếp ảnh', 'http://localhost:9000/products/xiaomi-14-ultra-white.png', 29990000.00, 'ACTIVE', '2026-02-05 14:00:00', '2026-02-05 14:00:00'),
(5, 8, 1, 'iPad Air M2 11 inch', 'ipad-air-m2-11-inch', 'Hiệu năng đột phá từ vi xử lý M2, hỗ trợ Apple Pencil Pro và Magic Keyboard', 'http://localhost:9000/products/ipad-air-m2-spacegray.png', 16990000.00, 'ACTIVE', '2026-02-10 11:00:00', '2026-02-10 11:00:00'),
(6, 10, 1, 'MacBook Pro 14 M3 Pro', 'macbook-pro-14-m3-pro', 'Màn hình Liquid Retina XDR 120Hz, màu Space Black chống bám vân tay', 'http://localhost:9000/products/macbook-pro-14-m3-black.png', 49990000.00, 'ACTIVE', '2026-02-15 15:00:00', '2026-02-15 15:00:00'),
(7, 12, 4, 'Tai nghe Sony WH-1000XM5', 'tai-nghe-sony-wh-1000xm5', 'Chống ồn đỉnh cao hàng đầu thế giới với bộ xử lý V1 và QN1, thời lượng pin 30 giờ', 'http://localhost:9000/products/sony-wh-1000xm5-black.png', 8490000.00, 'ACTIVE', '2026-02-20 16:00:00', '2026-02-20 16:00:00'),
(8, 13, 7, 'Loa Marshall Emberton II', 'loa-marshall-emberton-ii', 'Công nghệ âm thanh đa hướng True Stereophonic, kháng nước chuẩn IP67, pin trên 30 giờ', 'http://localhost:9000/products/marshall-emberton-ii.png', 3590000.00, 'ACTIVE', '2026-02-25 10:00:00', '2026-02-25 10:00:00'),
(9, 14, 6, 'Củ sạc Anker GaNPrime 65W', 'cu-sac-anker-ganprime-65w', 'Công nghệ sạc nhanh GaNPrime độc quyền, 2 cổng Type-C + 1 cổng USB-A tiện lợi', 'http://localhost:9000/products/anker-ganprime-65w.png', 1250000.00, 'ACTIVE', '2026-03-01 09:00:00', '2026-03-01 09:00:00'),
(10, 5, 1, 'iPhone 15 Pro Max', 'iphone-15-pro-max', 'Khung Titan cao cấp thế hệ đầu tiên, camera tiềm vọng zoom quang học 5x', 'http://localhost:9000/products/iphone-15-pro-max-natural.png', 29990000.00, 'ACTIVE', '2026-01-15 08:00:00', '2026-01-15 08:00:00');

-- --------------------------------------------------------------------
-- 6. PRODUCT_VARIANTS
-- --------------------------------------------------------------------
DELETE FROM product_variants;
INSERT INTO product_variants (variant_id, product_product_id, variant_name, sku, price, compare_at_price, color, ram_gb, rom_gb, connection_type, attributes, image_url, status, created_at, updated_at) VALUES
(1, 1, 'iPhone 16 Pro Max 256GB - Titan Sa Mạc', 'IP16PM-256-DESERT', 34990000.00, 36990000.00, 'Titan Sa Mạc', 8, 256, NULL, '{"edition":"VN/A"}', 'http://localhost:9000/products/variants/ip16pm-desert.png', 'ACTIVE', '2026-02-01 10:00:00', '2026-02-01 10:00:00'),
(2, 1, 'iPhone 16 Pro Max 256GB - Titan Tự Nhiên', 'IP16PM-256-NATURAL', 34990000.00, 36990000.00, 'Titan Tự Nhiên', 8, 256, NULL, '{"edition":"VN/A"}', 'http://localhost:9000/products/variants/ip16pm-natural.png', 'ACTIVE', '2026-02-01 10:00:00', '2026-02-01 10:00:00'),
(3, 1, 'iPhone 16 Pro Max 512GB - Titan Đen', 'IP16PM-512-BLACK', 40990000.00, 42990000.00, 'Titan Đen', 8, 512, NULL, '{"edition":"VN/A"}', 'http://localhost:9000/products/variants/ip16pm-black.png', 'ACTIVE', '2026-02-01 10:00:00', '2026-02-01 10:00:00'),
(4, 2, 'iPhone 15 128GB - Hồng Pastel', 'IP15-128-PINK', 19990000.00, 21990000.00, 'Hồng Pastel', 6, 128, NULL, '{"edition":"VN/A"}', 'http://localhost:9000/products/variants/ip15-pink.png', 'ACTIVE', '2026-02-01 10:30:00', '2026-02-01 10:30:00'),
(5, 3, 'Galaxy S24 Ultra 256GB - Xám Titan', 'S24U-256-GRAY', 31990000.00, 33990000.00, 'Xám Titan', 12, 256, NULL, '{"edition":"Chính hãng"}', 'http://localhost:9000/products/variants/s24u-gray.png', 'ACTIVE', '2026-02-02 09:00:00', '2026-02-02 09:00:00'),
(6, 4, 'Xiaomi 14 Ultra 512GB - Trắng Gốm', 'MI14U-512-WHITE', 29990000.00, 32990000.00, 'Trắng Gốm', 16, 512, NULL, '{"edition":"Chính hãng"}', 'http://localhost:9000/products/variants/mi14u-white.png', 'ACTIVE', '2026-02-05 14:00:00', '2026-02-05 14:00:00'),
(7, 5, 'iPad Air M2 11 inch 128GB WiFi - Space Gray', 'IPADAIR-M2-128-GRAY', 16990000.00, 17990000.00, 'Xám Không Gian', 8, 128, 'Wifi Only', '{"edition":"Chính hãng"}', 'http://localhost:9000/products/variants/ipad-air-gray.png', 'ACTIVE', '2026-02-10 11:00:00', '2026-02-10 11:00:00'),
(8, 6, 'Apple M3 Pro / 18GB / 512GB - Space Black', 'MBP14-M3P-512-BLACK', 49990000.00, 52990000.00, 'Space Black', 18, 512, NULL, '{"cpu":"Apple M3 Pro 11-core","gpu":"14-core GPU","screen":"14.2 inch Liquid Retina XDR"}', 'http://localhost:9000/products/variants/mbp14-black.png', 'ACTIVE', '2026-02-15 15:00:00', '2026-02-15 15:00:00'),
(9, 7, 'Sony WH-1000XM5 - Đen Nhám', 'SONY-XM5-BLACK', 8490000.00, 8990000.00, 'Đen Nhám', NULL, NULL, 'Bluetooth 5.3', '{"compatibleModel":"Android / iOS / Windows"}', 'http://localhost:9000/products/variants/sony-xm5-black.png', 'ACTIVE', '2026-02-20 16:00:00', '2026-02-20 16:00:00'),
(10, 8, 'Marshall Emberton II - Black and Brass', 'MARSHALL-EMB2-BRASS', 3590000.00, 3990000.00, 'Black and Brass', NULL, NULL, 'Bluetooth 5.3', '{"compatibleModel":"Mọi thiết bị di động"}', 'http://localhost:9000/products/variants/marshall-emb2.png', 'ACTIVE', '2026-02-25 10:00:00', '2026-02-25 10:00:00'),
(11, 9, 'USB-C / 65W - Đen Nhám', 'ANKER-GAN-65W-BLK', 1250000.00, 1490000.00, 'Đen Nhám', NULL, NULL, 'USB-C', '{"wattage":"65W","compatibleModel":"Laptop / iPhone / iPad"}', 'http://localhost:9000/products/variants/anker-65w-blk.png', 'ACTIVE', '2026-03-01 09:00:00', '2026-03-01 09:00:00'),
(101, 10, 'iPhone 15 Pro Max 256GB - Titan Tự Nhiên', 'IP15PM-256-TI', 29990000.00, 32990000.00, 'Titan Tự Nhiên', 8, 256, NULL, '{"edition":"VN/A"}', 'http://localhost:9000/products/variants/ip15pm-ti.png', 'ACTIVE', '2026-01-15 08:30:00', '2026-01-15 08:30:00');

-- --------------------------------------------------------------------
-- 7. PRODUCT_IMAGES
-- --------------------------------------------------------------------
DELETE FROM product_images;
INSERT INTO product_images (image_id, product_product_id, image_url, is_primary, display_order, created_at) VALUES
(1, 1, 'http://localhost:9000/products/iphone-16-pro-max-desert.png', b'1', 1, '2026-02-01 10:00:00'),
(2, 1, 'http://localhost:9000/products/iphone-16-pro-max-back.png', b'0', 2, '2026-02-01 10:00:00'),
(3, 2, 'http://localhost:9000/products/iphone-15-pink.png', b'1', 1, '2026-02-01 10:30:00'),
(4, 3, 'http://localhost:9000/products/galaxy-s24-ultra-gray.png', b'1', 1, '2026-02-02 09:00:00'),
(5, 4, 'http://localhost:9000/products/xiaomi-14-ultra-white.png', b'1', 1, '2026-02-05 14:00:00'),
(6, 5, 'http://localhost:9000/products/ipad-air-m2-spacegray.png', b'1', 1, '2026-02-10 11:00:00'),
(7, 6, 'http://localhost:9000/products/macbook-pro-14-m3-black.png', b'1', 1, '2026-02-15 15:00:00'),
(8, 7, 'http://localhost:9000/products/sony-wh-1000xm5-black.png', b'1', 1, '2026-02-20 16:00:00'),
(9, 8, 'http://localhost:9000/products/marshall-emberton-ii.png', b'1', 1, '2026-02-25 10:00:00'),
(10, 9, 'http://localhost:9000/products/anker-ganprime-65w.png', b'1', 1, '2026-03-01 09:00:00'),
(11, 10, 'http://localhost:9000/products/iphone-15-pro-max-natural.png', b'1', 1, '2026-01-15 08:00:00');

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

INSERT INTO orders (order_id, order_code, user_user_id, recipient_name, recipient_phone, shipping_address, total_amount, shipping_fee, discount_amount, final_amount, payment_method, payment_status, status, note, cancel_reason, voucher_voucher_id, completed_at, version, created_at, updated_at) VALUES
(1, 'DH-20261015-089', 4, 'Trần Thị Thu Huyền', '0987654321', 'Số 12 ngõ 45 phố Trần Thái Tông, Dịch Vọng Hậu, Cầu Giấy, Hà Nội', 34990000.00, 0.00, 500000.00, 34490000.00, 'BANK_TRANSFER', 'PAID', 'SHIPPING', 'Giao hàng giờ hành chính', NULL, 1, NULL, 0, '2026-10-15 08:30:00', '2026-10-16 10:00:00'),
(2, 'DH-20261020-102', 5, 'Hoàng Minh Khôi', '0971234567', 'Số 88 đường Nguyễn Văn Linh, Phường Nam Dương, Hải Châu, Đà Nẵng', 31990000.00, 0.00, 0.00, 31990000.00, 'BANK_TRANSFER', 'PAID', 'COMPLETED', 'Gọi trước khi giao', NULL, NULL, '2026-10-23 15:30:00', 0, '2026-10-20 09:15:00', '2026-10-23 15:30:00'),
(3, 'DH-20261028-115', 6, 'Phạm Đức Anh', '0962345678', 'Số 250 đường Lạch Tray, Quận Ngô Quyền, Hải Phòng', 3590000.00, 30000.00, 30000.00, 3590000.00, 'COD', 'PENDING', 'CONFIRMED', 'Kiểm tra hàng trước khi thanh toán COD', NULL, 2, NULL, 0, '2026-10-28 14:00:00', '2026-10-28 15:00:00'),
(4, 'DH-20261101-120', 7, 'Đỗ Bảo Ngọc', '0933456789', 'Số 18 đường 30 Tháng 4, Phường An Phú, Ninh Kiều, Cần Thơ', 8490000.00, 0.00, 0.00, 8490000.00, 'BANK_TRANSFER', 'PAID', 'PROCESSING', 'Đóng gói cẩn thận giúp mình', NULL, NULL, NULL, 0, '2026-11-01 11:20:00', '2026-11-01 11:30:00'),
(5, 'DH-20261102-125', 8, 'Vũ Tuấn Kiệt', '0944567890', 'Số 105 phố Bà Triệu, Phường Hàng Bài, Hoàn Kiếm, Hà Nội', 1250000.00, 30000.00, 0.00, 1280000.00, 'BANK_TRANSFER', 'FAILED', 'CANCELLED', 'Huỷ đơn', 'Khách hàng đổi ý, đặt nhầm màu sản phẩm', NULL, NULL, 0, '2026-11-02 16:00:00', '2026-11-02 16:45:00'),
(6, 'DH-20261103-130', 9, 'Bùi Phương Thảo', '0955678901', 'Căn hộ B12-08 Sunrise City, Quận 7, TP. Hồ Chí Minh', 28500000.00, 0.00, 500000.00, 28000000.00, 'COD', 'PENDING', 'SHIPPING', 'Giao buổi chiều sau 14h', NULL, 1, NULL, 0, '2026-11-03 10:00:00', '2026-11-04 09:00:00'),
-- Đơn hàng 210 chuẩn spec Nhóm 11 để test đổi trả:
(210, 'ORD-20261102-004', 105, 'Trần Thu Huyền', '0987654321', 'Số 12 ngõ 45 phố Trần Thái Tông, Dịch Vọng Hậu, Cầu Giấy, Hà Nội', 29990000.00, 0.00, 0.00, 29990000.00, 'COD', 'PAID', 'COMPLETED', 'Đơn hàng giao thành công, trong hạn 30 ngày', NULL, NULL, '2026-11-02 15:00:00', 0, '2026-11-01 09:00:00', '2026-11-02 15:00:00'),
-- Đơn hàng 211 để test quá hạn 30 ngày (RETURN_PERIOD_EXPIRED):
(211, 'ORD-20260901-001', 105, 'Trần Thu Huyền', '0987654321', 'Số 12 ngõ 45 phố Trần Thái Tông, Dịch Vọng Hậu, Cầu Giấy, Hà Nội', 19990000.00, 0.00, 0.00, 19990000.00, 'BANK_TRANSFER', 'PAID', 'COMPLETED', 'Đơn hàng cũ đã hơn 60 ngày', NULL, NULL, '2026-09-01 10:00:00', 0, '2026-08-28 10:00:00', '2026-09-01 10:00:00'),
-- Đơn hàng 212 để test trường hợp thanh toán BANK_TRANSFER hợp lệ:
(212, 'ORD-20261105-002', 4, 'Trần Thị Thu Huyền', '0987654321', 'Số 12 ngõ 45 phố Trần Thái Tông, Dịch Vọng Hậu, Cầu Giấy, Hà Nội', 34990000.00, 0.00, 0.00, 34990000.00, 'BANK_TRANSFER', 'PAID', 'COMPLETED', 'Đã nhận máy ngày 05/11', NULL, NULL, '2026-11-05 14:00:00', 0, '2026-11-04 10:00:00', '2026-11-05 14:00:00');

-- --------------------------------------------------------------------
-- 16. ORDER_ITEMS
-- --------------------------------------------------------------------
INSERT INTO order_items (order_item_id, order_order_id, variant_variant_id, sku_snapshot, product_name_snapshot, color_snapshot, rom_snapshot, image_url, price, quantity, total_price, created_at) VALUES
(1, 1, 1, 'IP16PM-256-DESERT', 'iPhone 16 Pro Max', 'Titan Sa Mạc', '256GB', 'http://localhost:9000/products/variants/ip16pm-desert.png', 34990000.00, 1, 34990000.00, '2026-10-15 08:30:00'),
(2, 2, 5, 'S24U-256-GRAY', 'Galaxy S24 Ultra', 'Xám Titan', '256GB', 'http://localhost:9000/products/variants/s24u-gray.png', 31990000.00, 1, 31990000.00, '2026-10-20 09:15:00'),
(3, 3, 10, 'MARSHALL-EMB2-BRASS', 'Marshall Emberton II', 'Black and Brass', 'N/A', 'http://localhost:9000/products/variants/marshall-emb2.png', 3590000.00, 1, 3590000.00, '2026-10-28 14:00:00'),
(4, 4, 9, 'SONY-XM5-BLACK', 'Sony WH-1000XM5', 'Đen Nhám', 'N/A', 'http://localhost:9000/products/variants/sony-xm5-black.png', 8490000.00, 1, 8490000.00, '2026-11-01 11:20:00'),
(5, 5, 11, 'ANKER-GAN-65W-BLK', 'Củ sạc Anker GaNPrime 65W', 'Đen Nhám', 'N/A', 'http://localhost:9000/products/variants/anker-65w-blk.png', 1250000.00, 1, 1250000.00, '2026-11-02 16:00:00'),
(6, 6, 7, 'IPADAIR-M2-128-GRAY', 'iPad Air M2 11 inch', 'Xám Không Gian', '128GB', 'http://localhost:9000/products/variants/ipad-air-gray.png', 16990000.00, 1, 16990000.00, '2026-11-03 10:00:00'),
(7, 6, 11, 'ANKER-GAN-65W-BLK', 'Củ sạc Anker GaNPrime 65W', 'Đen Nhám', 'N/A', 'http://localhost:9000/products/variants/anker-65w-blk.png', 1250000.00, 1, 1250000.00, '2026-11-03 10:00:00'),
(8, 210, 101, 'IP15PM-256-TI', 'iPhone 15 Pro Max', 'Titan Tự Nhiên', '256GB', 'http://localhost:9000/products/variants/ip15pm-ti.png', 29990000.00, 1, 29990000.00, '2026-11-01 09:00:00'),
(9, 211, 4, 'IP15-128-PINK', 'iPhone 15', 'Hồng Pastel', '128GB', 'http://localhost:9000/products/variants/ip15-pink.png', 19990000.00, 1, 19990000.00, '2026-08-28 10:00:00'),
(10, 212, 1, 'IP16PM-256-DESERT', 'iPhone 16 Pro Max', 'Titan Sa Mạc', '256GB', 'http://localhost:9000/products/variants/ip16pm-desert.png', 34990000.00, 1, 34990000.00, '2026-11-04 10:00:00');

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
(35, 210, 105, NULL, 'Màn hình xuất hiện sọc xanh sau 5 ngày sử dụng, loa thoại bị rè khi đàm thoại', 'PENDING', NULL, '["http://localhost:9000/reviews/evidence-soc-man-hinh-1.jpg","http://localhost:9000/reviews/evidence-soc-man-hinh-2.jpg"]', 'Vietcombank', '0123456789', 'TRAN THU HUYEN', NULL, NULL, NULL, NULL, NULL, '2026-11-03 21:10:00', '2026-11-03 21:10:00'),
(36, 212, 4, 201, 'Máy tự động sập nguồn liên tục khi cắm sạc pin', 'APPROVED', 34990000.00, '["http://localhost:9000/reviews/evidence-power-1.jpg"]', NULL, NULL, NULL, 'Xác nhận lỗi phần cứng nguồn từ nhà sản xuất, đủ điều kiện hoàn kho MB04', NULL, 'Đã hoàn tiền vào tài khoản liên kết', '2026-11-06 14:00:00', '2026-11-06 14:00:00', '2026-11-06 09:00:00', '2026-11-06 14:00:00'),
(37, 2, 5, 201, 'Vỏ ngoài bị trầy xước nhẹ sau 10 ngày nhận hàng', 'REJECTED', NULL, '["http://localhost:9000/reviews/evidence-scratch.jpg"]', NULL, NULL, NULL, 'Kiểm tra máy bị trầy xước do va quẹt từ phía người dùng sau khi đã nhận và ký biên bản giao hàng', 'Từ chối đổi trả do trầy xước ngoại quan phát sinh trong quá trình khách hàng sử dụng', 'Đã liên hệ giải thích chính sách cho khách', '2026-10-25 10:00:00', NULL, '2026-10-24 16:00:00', '2026-10-25 10:00:00');

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


-- --------------------------------------------------------------------
-- 28. REVIEW_IMAGES
-- --------------------------------------------------------------------
DELETE FROM review_images;
INSERT INTO review_images (image_id, review_id, image_url, bucket, object_key, content_type, file_size, display_order, created_at) VALUES
(1, 1, 'http://localhost:9000/reviews/review-s24u-1.jpg', 'reviews', 'review-s24u-1.jpg', 'image/jpeg', 636913, 1, '2026-10-24 09:05:00'),
(2, 2, 'http://localhost:9000/reviews/review-marshall-1.jpg', 'reviews', 'review-marshall-1.jpg', 'image/jpeg', 542194, 1, '2026-10-29 11:05:00'),
(3, 3, 'http://localhost:9000/reviews/review-iphone-1.jpg', 'reviews', 'review-iphone-1.jpg', 'image/jpeg', 218223, 1, '2026-11-03 20:05:00');

-- ====================================================================
-- BATCH 2 EXPANDED SEED DATA
-- ====================================================================

INSERT INTO users (user_id, full_name, email, phone, password_hash, role, status, lock_reason, created_at, updated_at) VALUES
(11, 'Lê Hoàng Nam', 'nam.le@gmail.com', '0903112233', '$2a$10$lKifkavs5h2GbVPkX65AHuBBJf8qFtuqHfv0FA/JYIUShTBkbySvm', 'CUSTOMER', 'ACTIVE', NULL, '2026-05-10 09:00:00', '2026-05-10 09:00:00'),
(12, 'Nguyễn Mai Anh', 'maianh.nguyen@gmail.com', '0904223344', '$2a$10$lKifkavs5h2GbVPkX65AHuBBJf8qFtuqHfv0FA/JYIUShTBkbySvm', 'CUSTOMER', 'ACTIVE', NULL, '2026-05-15 10:30:00', '2026-05-15 10:30:00'),
(13, 'Trương Quốc Bảo', 'bao.truong@gmail.com', '0905334455', '$2a$10$lKifkavs5h2GbVPkX65AHuBBJf8qFtuqHfv0FA/JYIUShTBkbySvm', 'CUSTOMER', 'ACTIVE', NULL, '2026-05-20 14:15:00', '2026-05-20 14:15:00'),
(14, 'Võ Thị Bích Trâm', 'tram.vo@gmail.com', '0906445566', '$2a$10$lKifkavs5h2GbVPkX65AHuBBJf8qFtuqHfv0FA/JYIUShTBkbySvm', 'CUSTOMER', 'ACTIVE', NULL, '2026-06-01 08:20:00', '2026-06-01 08:20:00'),
(15, 'Đặng Hải Đăng', 'dang.haidang@gmail.com', '0907556677', '$2a$10$lKifkavs5h2GbVPkX65AHuBBJf8qFtuqHfv0FA/JYIUShTBkbySvm', 'CUSTOMER', 'ACTIVE', NULL, '2026-06-05 11:45:00', '2026-06-05 11:45:00'),
(16, 'Phan Thùy Dung', 'dung.phan@gmail.com', '0908667788', '$2a$10$lKifkavs5h2GbVPkX65AHuBBJf8qFtuqHfv0FA/JYIUShTBkbySvm', 'CUSTOMER', 'ACTIVE', NULL, '2026-06-10 16:10:00', '2026-06-10 16:10:00'),
(17, 'Dương Gia Huy', 'huy.duong@gmail.com', '0909778899', '$2a$10$lKifkavs5h2GbVPkX65AHuBBJf8qFtuqHfv0FA/JYIUShTBkbySvm', 'CUSTOMER', 'ACTIVE', NULL, '2026-06-15 09:40:00', '2026-06-15 09:40:00'),
(18, 'Ngô Cẩm Tiên', 'tien.ngo@gmail.com', '0918112233', '$2a$10$lKifkavs5h2GbVPkX65AHuBBJf8qFtuqHfv0FA/JYIUShTBkbySvm', 'CUSTOMER', 'ACTIVE', NULL, '2026-07-01 13:00:00', '2026-07-01 13:00:00'),
(19, 'Hồ Vĩnh Khoa', 'khoa.ho@gmail.com', '0919223344', '$2a$10$lKifkavs5h2GbVPkX65AHuBBJf8qFtuqHfv0FA/JYIUShTBkbySvm', 'CUSTOMER', 'ACTIVE', NULL, '2026-07-05 15:30:00', '2026-07-05 15:30:00'),
(20, 'Đoàn Nhật Minh', 'minh.doan@gmail.com', '0917334455', '$2a$10$lKifkavs5h2GbVPkX65AHuBBJf8qFtuqHfv0FA/JYIUShTBkbySvm', 'CUSTOMER', 'ACTIVE', NULL, '2026-07-10 10:20:00', '2026-07-10 10:20:00'),
(21, 'Tạ Mỹ Linh', 'linh.ta@gmail.com', '0916445566', '$2a$10$lKifkavs5h2GbVPkX65AHuBBJf8qFtuqHfv0FA/JYIUShTBkbySvm', 'CUSTOMER', 'ACTIVE', NULL, '2026-07-15 11:15:00', '2026-07-15 11:15:00'),
(22, 'Phùng Thanh Độ', 'do.phung@gmail.com', '0915556677', '$2a$10$lKifkavs5h2GbVPkX65AHuBBJf8qFtuqHfv0FA/JYIUShTBkbySvm', 'CUSTOMER', 'ACTIVE', NULL, '2026-08-01 14:00:00', '2026-08-01 14:00:00'),
(23, 'Mai Văn Tài', 'tai.mai@nextgen.vn', '0914667788', '$2a$10$lKifkavs5h2GbVPkX65AHuBBJf8qFtuqHfv0FA/JYIUShTBkbySvm', 'STAFF', 'ACTIVE', NULL, '2026-08-05 08:30:00', '2026-08-05 08:30:00'),
(24, 'Cao Thị Diệu', 'dieu.cao@nextgen.vn', '0913778899', '$2a$10$lKifkavs5h2GbVPkX65AHuBBJf8qFtuqHfv0FA/JYIUShTBkbySvm', 'STAFF', 'ACTIVE', NULL, '2026-08-10 08:45:00', '2026-08-10 08:45:00'),
(25, 'Trịnh Công Sơn', 'son.trinh@gmail.com', '0912889900', '$2a$10$lKifkavs5h2GbVPkX65AHuBBJf8qFtuqHfv0FA/JYIUShTBkbySvm', 'CUSTOMER', 'ACTIVE', NULL, '2026-08-15 16:40:00', '2026-08-15 16:40:00');

INSERT INTO user_addresses (address_id, user_user_id, recipient_name, phone, address_line, is_default, created_at, updated_at) VALUES
(9, 11, 'Lê Hoàng Nam', '0903112233', 'Số 54 Liễu Giai, Phường Cống Vị, Ba Đình, Hà Nội', b'1', '2026-05-10 09:05:00', '2026-05-10 09:05:00'),
(10, 12, 'Nguyễn Mai Anh', '0904223344', 'Số 102 Lê Lợi, Phường Bến Nghé, Quận 1, TP. Hồ Chí Minh', b'1', '2026-05-15 10:35:00', '2026-05-15 10:35:00'),
(11, 13, 'Trương Quốc Bảo', '0905334455', 'Số 15 Quang Trung, Phường Thạch Thang, Hải Châu, Đà Nẵng', b'1', '2026-05-20 14:20:00', '2026-05-20 14:20:00'),
(12, 14, 'Võ Thị Bích Trâm', '0906445566', 'Tòa nhà Bitexco, 2 Hải Triều, Bến Nghé, Quận 1, TP. Hồ Chí Minh', b'1', '2026-06-01 08:25:00', '2026-06-01 08:25:00'),
(13, 15, 'Đặng Hải Đăng', '0907556677', 'Số 22 Hai Bà Trưng, Phường Vĩnh Ninh, TP. Huế', b'1', '2026-06-05 11:50:00', '2026-06-05 11:50:00'),
(14, 16, 'Phan Thùy Dung', '0908667788', 'Số 89 Nguyễn Thị Minh Khai, Phường 4, TP. Vũng Tàu', b'1', '2026-06-10 16:15:00', '2026-06-10 16:15:00'),
(15, 17, 'Dương Gia Huy', '0909778899', 'Số 76 Trần Hưng Đạo, Phường 1, TP. Tuy Hòa, Phú Yên', b'1', '2026-06-15 09:45:00', '2026-06-15 09:45:00'),
(16, 18, 'Ngô Cẩm Tiên', '0918112233', 'Số 41 Lê Duẩn, Phường Hải Châu 1, Hải Châu, Đà Nẵng', b'1', '2026-07-01 13:05:00', '2026-07-01 13:05:00'),
(17, 19, 'Hồ Vĩnh Khoa', '0919223344', 'Số 120 Điện Biên Phủ, Phường Đa Kao, Quận 1, TP. Hồ Chí Minh', b'1', '2026-07-05 15:35:00', '2026-07-05 15:35:00'),
(18, 20, 'Đoàn Nhật Minh', '0917334455', 'Số 33 Đại lộ Hòa Bình, Ninh Kiều, TP. Cần Thơ', b'1', '2026-07-10 10:25:00', '2026-07-10 10:25:00'),
(19, 21, 'Tạ Mỹ Linh', '0916445566', 'Số 90 Phan Bội Châu, Phường Tân Lập, Buôn Ma Thuột, Đắk Lắk', b'1', '2026-07-15 11:20:00', '2026-07-15 11:20:00'),
(20, 22, 'Phùng Thanh Độ', '0915556677', 'Khu đô thị Ecopark, Xã Xuân Quan, Văn Giang, Hưng Yên', b'1', '2026-08-01 14:05:00', '2026-08-01 14:05:00'),
(21, 25, 'Trịnh Công Sơn', '0912889900', 'Số 17 Pasteur, Phường 4, TP. Đà Lạt, Lâm Đồng', b'1', '2026-08-15 16:45:00', '2026-08-15 16:45:00'),
(22, 11, 'Lê Hoàng Nam', '0903112233', 'Tầng 12 Keangnam Landmark 72, Nam Từ Liêm, Hà Nội', b'0', '2026-08-20 09:00:00', '2026-08-20 09:00:00'),
(23, 12, 'Nguyễn Mai Anh', '0904223344', 'Căn hộ chung cư Vinhomes Central Park, Bình Thạnh, TP. Hồ Chí Minh', b'0', '2026-08-22 10:00:00', '2026-08-22 10:00:00');

INSERT INTO brands (brand_id, brand_name, slug, description, logo_url, is_active, created_at, updated_at) VALUES
(8, 'Dell', 'dell', 'Hãng sản xuất máy tính xách tay cao cấp XPS, Precision và Latitude hàng đầu thế giới', 'http://localhost:9000/brands/dell.svg', b'1', '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
(9, 'HP', 'hp', 'Thương hiệu máy tính văn phòng, workstation và gaming Omen đẳng cấp từ Mỹ', 'http://localhost:9000/brands/hp.svg', b'1', '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
(10, 'Lenovo', 'lenovo', 'Dòng máy tính doanh nhân ThinkPad huyền thoại và dòng Legion gaming hiệu năng cực cao', 'http://localhost:9000/brands/lenovo.svg', b'1', '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
(11, 'LG', 'lg', 'Tập đoàn công nghệ hiển thị OLED, màn hình gaming UltraGear và laptop mỏng nhẹ LG Gram', 'http://localhost:9000/brands/lg.svg', b'1', '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
(12, 'Google', 'google', 'Dòng smartphone Pixel thuần Android trải nghiệm AI đỉnh cao và nhiếp ảnh điện toán', 'http://localhost:9000/brands/google.svg', b'1', '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
(13, 'Garmin', 'garmin', 'Đồng hồ thông minh thể thao chuyên nghiệp với định vị GPS đa băng tần chính xác nhất', 'http://localhost:9000/brands/garmin.svg', b'1', '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
(14, 'JBL', 'jbl', 'Âm thanh di động bùng nổ JBL Pro Sound chuẩn kháng nước kháng bụi vượt trội', 'http://localhost:9000/brands/jbl.svg', b'1', '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
(15, 'Logitech', 'logitech', 'Thiết bị ngoại vi công thái học chuột MX Master và bàn phím cơ văn phòng hàng đầu', 'http://localhost:9000/brands/logitech.svg', b'1', '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
(16, 'Baseus', 'baseus', 'Phụ kiện công nghệ sạc nhanh GaN, pin sạc dự phòng công suất khủng và hub chuyển đổi', 'http://localhost:9000/brands/baseus.svg', b'1', '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
(17, 'Sennheiser', 'sennheiser', 'Thương hiệu âm thanh audiophile thượng hạng từ Đức với chất âm chi tiết trong trẻo', 'http://localhost:9000/brands/sennheiser.svg', b'1', '2026-01-01 00:00:00', '2026-01-01 00:00:00');

INSERT INTO categories (category_id, parent_category_id, category_name, slug, description, variant_schema, is_active, created_at, updated_at) VALUES
(15, NULL, 'Đồng hồ thông minh', 'dong-ho-thong-minh', 'Smartwatch theo dõi sức khỏe, thể thao chuyên nghiệp và nhận thông báo', '[{"key":"color","label":"Màu sắc","storage":"COLUMN","type":"text","required":true},{"key":"edition","label":"Phiên bản","storage":"ATTRIBUTE","type":"select","options":["GPS","GPS + Cellular","Titanium"],"required":true}]', b'1', '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
(16, 15, 'Đồng hồ thể thao Garmin', 'dong-ho-the-thao-garmin', 'Đồng hồ GPS chạy bộ, ba môn phối hợp và leo núi chuyên dụng', NULL, b'1', '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
(17, 3, 'Chuột & Bàn phím máy tính', 'chuot-ban-phim-may-tinh', 'Chuột công thái học không dây và bàn phím cơ gõ êm mượt', NULL, b'1', '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
(18, 4, 'Pin sạc dự phòng', 'pin-sac-du-phong', 'Pin sạc dự phòng dung lượng cao hỗ trợ sạc nhanh laptop và điện thoại', NULL, b'1', '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
(19, 3, 'Màn hình máy tính', 'man-hinh-may-tinh', 'Màn hình đồ họa chuẩn màu, màn hình gaming tần số quét 240Hz sắc nét', NULL, b'1', '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
(20, 4, 'Âm thanh Hi-Fi & Phòng thu', 'am-thanh-hi-fi-phong-thu', 'Tai nghe audiophile và DAC/AMP giải mã âm thanh chất lượng cao', NULL, b'1', '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
(21, 4, 'Phụ kiện Apple chính hãng', 'phu-kien-apple-chinh-hang', 'Cáp sạc MagSafe, ốp lưng Silicon Apple, củ sạc 20W VN/A', NULL, b'1', '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
(22, 4, 'Phụ kiện Samsung', 'phu-kien-samsung', 'Bút S-Pen, bao da gập Smart View và dock sạc đôi không dây', NULL, b'1', '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
(23, 4, 'Ốp lưng & Bao da chống sốc', 'op-lung-bao-da-chong-soc', 'Ốp lưng UAG, Spigen đạt chuẩn chống rơi va đập quân sự', NULL, b'1', '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
(24, 4, 'Cáp sạc Type-C đa năng', 'cap-sac-type-c-da-nang', 'Cáp bọc dù siêu bền hỗ trợ truyền tải 240W Power Delivery và dữ liệu tốc độ cao', NULL, b'1', '2026-01-01 00:00:00', '2026-01-01 00:00:00');

INSERT INTO products (product_id, category_category_id, brand_brand_id, product_name, slug, description, thumbnail_url, base_price, status, created_at, updated_at) VALUES
(11, 3, 8, 'Dell XPS 13 9340 (2024)', 'dell-xps-13-9340', 'Vi xử lý Intel Core Ultra 7 tích hợp NPU AI, màn hình OLED InfinityEdge 3K viền siêu mỏng', 'http://localhost:9000/products/dell-xps-13.png', 42990000.00, 'ACTIVE', '2026-03-01 10:00:00', '2026-03-01 10:00:00'),
(12, 3, 10, 'Lenovo ThinkPad X1 Carbon Gen 12', 'lenovo-thinkpad-x1-carbon-gen-12', 'Vỏ sợi carbon siêu nhẹ chuẩn độ bền quân đội Mỹ MIL-STD, bàn phím gõ êm ái hàng đầu thế giới', 'http://localhost:9000/products/thinkpad-x1.png', 45990000.00, 'ACTIVE', '2026-03-05 11:00:00', '2026-03-05 11:00:00'),
(13, 11, 5, 'Asus ROG Zephyrus G14 (2024)', 'asus-rog-zephyrus-g14', 'Màn hình ROG Nebula OLED 3K 120Hz, GPU RTX 4060, khung nhôm nguyên khối sang trọng', 'http://localhost:9000/products/asus-rog-g14.png', 48990000.00, 'ACTIVE', '2026-03-10 14:00:00', '2026-03-10 14:00:00'),
(14, 1, 12, 'Google Pixel 8 Pro', 'google-pixel-8-pro', 'Chip Tensor G3 mạnh mẽ, trợ lý ảo Gemini Nano trên máy và camera zoom tiềm vọng 5x đỉnh cao', 'http://localhost:9000/products/pixel-8-pro.png', 23990000.00, 'ACTIVE', '2026-03-15 09:00:00', '2026-03-15 09:00:00'),
(15, 6, 2, 'Samsung Galaxy Z Fold 5 5G', 'samsung-galaxy-z-fold-5', 'Bản lề Flex gập không khe hở, màn hình lớn Dynamic AMOLED 2X 7.6 inch đa nhiệm tuyệt đỉnh', 'http://localhost:9000/products/galaxy-z-fold-5.png', 32990000.00, 'ACTIVE', '2026-03-20 15:30:00', '2026-03-20 15:30:00'),
(16, 15, 1, 'Apple Watch Series 9 GPS 45mm', 'apple-watch-series-9-gps-45mm', 'Chip S9 SiP tiên tiến, thao tác chạm hai lần Double Tap độc đáo, màn hình sáng 2000 nits', 'http://localhost:9000/products/apple-watch-s9.png', 10490000.00, 'ACTIVE', '2026-04-01 10:00:00', '2026-04-01 10:00:00'),
(17, 15, 1, 'Apple Watch Ultra 2 GPS + Cellular', 'apple-watch-ultra-2', 'Vỏ Titan 49mm chuẩn hàng hải lặn biển 40m, GPS tần số kép L1+L5 chính xác tuyệt đối, pin 72 giờ', 'http://localhost:9000/products/apple-watch-ultra-2.png', 21490000.00, 'ACTIVE', '2026-04-05 11:30:00', '2026-04-05 11:30:00'),
(18, 16, 13, 'Garmin Forerunner 265 GPS AMOLED', 'garmin-forerunner-265', 'Màn hình cảm ứng AMOLED rực rỡ, tính năng gợi ý bài tập hàng ngày và đo chỉ số phục hồi HRV', 'http://localhost:9000/products/garmin-forerunner-265.png', 11690000.00, 'ACTIVE', '2026-04-10 16:00:00', '2026-04-10 16:00:00'),
(19, 13, 14, 'Loa Bluetooth JBL Charge 5', 'loa-bluetooth-jbl-charge-5', 'Chất âm JBL Pro Sound uy lực, kháng nước chuẩn IP67, tích hợp pin dự phòng sạc điện thoại', 'http://localhost:9000/products/jbl-charge-5.png', 3890000.00, 'ACTIVE', '2026-04-15 08:30:00', '2026-04-15 08:30:00'),
(20, 17, 15, 'Chuột không dây Logitech MX Master 3S', 'chuot-logitech-mx-master-3s', 'Cảm biến 8000 DPI lướt mượt trên kính, con cuộn siêu nhanh MagSpeed, click chuột tĩnh âm 90%', 'http://localhost:9000/products/logitech-mx-master-3s.png', 2490000.00, 'ACTIVE', '2026-04-20 14:00:00', '2026-04-20 14:00:00'),
(21, 17, 15, 'Bàn phím cơ Logitech MX Mechanical', 'ban-phim-logitech-mx-mechanical', 'Bàn phím cơ low-profile êm ái, switch Tactile Quiet êm dịu, kết nối đa thiết bị Easy-Switch', 'http://localhost:9000/products/logitech-mx-mechanical.png', 3690000.00, 'ACTIVE', '2026-04-25 10:00:00', '2026-04-25 10:00:00'),
(22, 12, 4, 'Tai nghe Sony WF-1000XM5 True Wireless', 'tai-nghe-sony-wf-1000xm5', 'Chống ồn thông minh hàng đầu với chip V2 và QN2e, codec âm thanh độ phân giải cao LDAC', 'http://localhost:9000/products/sony-wf-1000xm5.png', 6490000.00, 'ACTIVE', '2026-05-01 09:30:00', '2026-05-01 09:30:00'),
(23, 12, 17, 'Tai nghe Sennheiser Momentum 4 Wireless', 'tai-nghe-sennheiser-momentum-4', 'Chất âm audiophile trong trẻo tuyệt đối, thời lượng pin kinh ngạc lên đến 60 giờ liên tục', 'http://localhost:9000/products/sennheiser-momentum-4.png', 7990000.00, 'ACTIVE', '2026-05-05 13:45:00', '2026-05-05 13:45:00'),
(24, 18, 16, 'Pin dự phòng Baseus Blade 100W 20000mAh', 'pin-du-phong-baseus-blade-100w', 'Thiết kế siêu mỏng 18mm, công suất 100W Power Delivery sạc nhanh đồng thời 2 laptop và điện thoại', 'http://localhost:9000/products/baseus-blade-100w.png', 1390000.00, 'ACTIVE', '2026-05-10 15:00:00', '2026-05-10 15:00:00'),
(25, 19, 11, 'Màn hình LG UltraGear 27GR95QE OLED 240Hz', 'man-hinh-lg-ultragear-27gr95qe', 'Tấm nền OLED 27 inch 2K QHD, tốc độ phản hồi siêu nhanh 0.03ms, tương thích G-Sync và FreeSync', 'http://localhost:9000/products/lg-ultragear-monitor.png', 19990000.00, 'ACTIVE', '2026-05-15 16:30:00', '2026-05-15 16:30:00');

INSERT INTO product_variants (variant_id, product_product_id, variant_name, sku, price, compare_at_price, color, ram_gb, rom_gb, connection_type, attributes, image_url, status, created_at, updated_at) VALUES
(12, 11, 'Dell XPS 13 Core Ultra 7 / 16GB / 512GB - Bạc Platinum', 'DELL-XPS13-512-SLV', 42990000.00, 45990000.00, 'Bạc Platinum', 16, 512, NULL, '{"cpu":"Intel Core Ultra 7 155H","screen":"13.4 inch FHD+ 120Hz"}', 'http://localhost:9000/products/variants/dell-xps-13.png', 'ACTIVE', '2026-03-01 10:00:00', '2026-03-01 10:00:00'),
(13, 11, 'Dell XPS 13 Core Ultra 7 / 32GB / 1TB - Bạc Platinum', 'DELL-XPS13-1TB-SLV', 48990000.00, 52990000.00, 'Bạc Platinum', 32, 1024, NULL, '{"cpu":"Intel Core Ultra 7 155H","screen":"13.4 inch 3K OLED Touch"}', 'http://localhost:9000/products/variants/dell-xps-13.png', 'ACTIVE', '2026-03-01 10:00:00', '2026-03-01 10:00:00'),
(14, 12, 'ThinkPad X1 Carbon Gen 12 / 16GB / 512GB - Đen', 'THINKPAD-X1-512-BLK', 45990000.00, 48990000.00, 'Đen Nhung', 16, 512, NULL, '{"cpu":"Intel Core Ultra 5 125H","weight":"1.09 kg"}', 'http://localhost:9000/products/variants/thinkpad-x1.png', 'ACTIVE', '2026-03-05 11:00:00', '2026-03-05 11:00:00'),
(15, 12, 'ThinkPad X1 Carbon Gen 12 / 32GB / 1TB - Đen', 'THINKPAD-X1-1TB-BLK', 54990000.00, 58990000.00, 'Đen Nhung', 32, 1024, NULL, '{"cpu":"Intel Core Ultra 7 155H","weight":"1.09 kg"}', 'http://localhost:9000/products/variants/thinkpad-x1.png', 'ACTIVE', '2026-03-05 11:00:00', '2026-03-05 11:00:00'),
(16, 13, 'ROG Zephyrus G14 RTX 4060 / 16GB / 1TB - Trắng Moonlight', 'ROG-G14-4060-WHT', 48990000.00, 51990000.00, 'Trắng Moonlight', 16, 1024, NULL, '{"gpu":"NVIDIA RTX 4060 8GB","cpu":"AMD Ryzen 9 8945HS"}', 'http://localhost:9000/products/variants/asus-rog-g14.png', 'ACTIVE', '2026-03-10 14:00:00', '2026-03-10 14:00:00'),
(17, 14, 'Google Pixel 8 Pro 128GB - Xanh Vịnh Bay', 'PIXEL8P-128-BLU', 23990000.00, 25990000.00, 'Xanh Vịnh Bay', 12, 128, NULL, '{"edition":"Quốc tế"}', 'http://localhost:9000/products/variants/pixel-8-pro.png', 'ACTIVE', '2026-03-15 09:00:00', '2026-03-15 09:00:00'),
(18, 14, 'Google Pixel 8 Pro 256GB - Đen Obsidian', 'PIXEL8P-256-BLK', 26490000.00, 28990000.00, 'Đen Obsidian', 12, 256, NULL, '{"edition":"Quốc tế"}', 'http://localhost:9000/products/variants/pixel-8-pro.png', 'ACTIVE', '2026-03-15 09:00:00', '2026-03-15 09:00:00'),
(19, 15, 'Galaxy Z Fold 5 256GB - Xanh Icy', 'ZFOLD5-256-BLU', 32990000.00, 36990000.00, 'Xanh Icy', 12, 256, NULL, '{"edition":"Chính hãng SSVN"}', 'http://localhost:9000/products/variants/galaxy-z-fold-5.png', 'ACTIVE', '2026-03-20 15:30:00', '2026-03-20 15:30:00'),
(20, 15, 'Galaxy Z Fold 5 512GB - Đen Phantom', 'ZFOLD5-512-BLK', 36990000.00, 40990000.00, 'Đen Phantom', 12, 512, NULL, '{"edition":"Chính hãng SSVN"}', 'http://localhost:9000/products/variants/galaxy-z-fold-5.png', 'ACTIVE', '2026-03-20 15:30:00', '2026-03-20 15:30:00'),
(21, 16, 'Apple Watch Series 9 GPS 45mm - Nhôm Đen Midnight', 'AW-S9-45-MID', 10490000.00, 11290000.00, 'Đen Midnight', NULL, NULL, 'Bluetooth 5.3 + Wifi', '{"caseSize":"45mm","bandType":"Dây Sport Band"}', 'http://localhost:9000/products/variants/apple-watch-s9.png', 'ACTIVE', '2026-04-01 10:00:00', '2026-04-01 10:00:00'),
(22, 16, 'Apple Watch Series 9 GPS 45mm - Nhôm Ánh Sao Starlight', 'AW-S9-45-STR', 10490000.00, 11290000.00, 'Ánh Sao Starlight', NULL, NULL, 'Bluetooth 5.3 + Wifi', '{"caseSize":"45mm","bandType":"Dây Sport Band"}', 'http://localhost:9000/products/variants/apple-watch-s9.png', 'ACTIVE', '2026-04-01 10:00:00', '2026-04-01 10:00:00'),
(23, 17, 'Apple Watch Ultra 2 GPS + Cellular 49mm - Dây Trail Loop', 'AW-ULTRA2-49-TRAIL', 21490000.00, 22990000.00, 'Titan Tự Nhiên', NULL, NULL, 'eSIM LTE + GPS', '{"caseSize":"49mm","bandType":"Dây Trail Loop"}', 'http://localhost:9000/products/variants/apple-watch-ultra-2.png', 'ACTIVE', '2026-04-05 11:30:00', '2026-04-05 11:30:00'),
(24, 18, 'Garmin Forerunner 265 - Đen Viền Vàng', 'GARMIN-FR265-BLK', 11690000.00, 12490000.00, 'Đen Viền Vàng', NULL, NULL, 'Bluetooth + GPS đa băng', '{"screen":"1.3 inch AMOLED"}', 'http://localhost:9000/products/variants/garmin-forerunner-265.png', 'ACTIVE', '2026-04-10 16:00:00', '2026-04-10 16:00:00'),
(25, 19, 'JBL Charge 5 - Đen Quân Đội', 'JBL-CHARGE5-BLK', 3890000.00, 4290000.00, 'Đen', NULL, NULL, 'Bluetooth 5.1', '{"batteryCapacity":"7500mAh"}', 'http://localhost:9000/products/variants/jbl-charge-5.png', 'ACTIVE', '2026-04-15 08:30:00', '2026-04-15 08:30:00'),
(26, 20, 'Logitech MX Master 3S - Xám Graphite', 'LOGI-MXM3S-GRY', 2490000.00, 2790000.00, 'Xám Graphite', NULL, NULL, 'Bluetooth / Logi Bolt', '{"dpi":"8000 DPI"}', 'http://localhost:9000/products/variants/logitech-mx-master-3s.png', 'ACTIVE', '2026-04-20 14:00:00', '2026-04-20 14:00:00'),
(27, 21, 'Logitech MX Mechanical - Switch Tactile Quiet', 'LOGI-MXMECH-TAC', 3690000.00, 4090000.00, 'Xám Không Gian', NULL, NULL, 'Bluetooth / Logi Bolt', '{"switchType":"Tactile Quiet"}', 'http://localhost:9000/products/variants/logitech-mx-mechanical.png', 'ACTIVE', '2026-04-25 10:00:00', '2026-04-25 10:00:00'),
(28, 22, 'Sony WF-1000XM5 - Đen Nhám', 'SONY-WF-XM5-BLK', 6490000.00, 6990000.00, 'Đen Nhám', NULL, NULL, 'Bluetooth 5.3', '{"batteryLife":"24 giờ kèm hộp"}', 'http://localhost:9000/products/variants/sony-wf-1000xm5.png', 'ACTIVE', '2026-05-01 09:30:00', '2026-05-01 09:30:00'),
(29, 23, 'Sennheiser Momentum 4 - Đen Kim Loại', 'SENN-MOM4-BLK', 7990000.00, 8990000.00, 'Đen Kim Loại', NULL, NULL, 'Bluetooth 5.2 / Cáp 3.5', '{"batteryLife":"60 giờ"}', 'http://localhost:9000/products/variants/sennheiser-momentum-4.png', 'ACTIVE', '2026-05-05 13:45:00', '2026-05-05 13:45:00'),
(30, 24, 'Baseus Blade 100W 20000mAh - Đen Nhám', 'BASEUS-BLADE-100W', 1390000.00, 1690000.00, 'Đen Nhám', NULL, NULL, 'USB-C x2 + USB-A x2', '{"outputWattage":"100W"}', 'http://localhost:9000/products/variants/baseus-blade-100w.png', 'ACTIVE', '2026-05-10 15:00:00', '2026-05-10 15:00:00'),
(31, 25, 'LG UltraGear 27GR95QE OLED 240Hz 0.03ms', 'LG-27GR95QE-OLED', 19990000.00, 22990000.00, 'Đen Gaming', NULL, NULL, 'HDMI 2.1 x2 / DP 1.4', '{"refreshRate":"240Hz","resolution":"2560x1440"}', 'http://localhost:9000/products/variants/lg-ultragear-monitor.png', 'ACTIVE', '2026-05-15 16:30:00', '2026-05-15 16:30:00');

INSERT INTO product_images (image_id, product_product_id, image_url, is_primary, display_order, created_at) VALUES
(12, 11, 'http://localhost:9000/products/dell-xps-13.png', b'1', 1, '2026-03-01 10:00:00'),
(13, 12, 'http://localhost:9000/products/thinkpad-x1.png', b'1', 1, '2026-03-05 11:00:00'),
(14, 13, 'http://localhost:9000/products/asus-rog-g14.png', b'1', 1, '2026-03-10 14:00:00'),
(15, 14, 'http://localhost:9000/products/pixel-8-pro.png', b'1', 1, '2026-03-15 09:00:00'),
(16, 15, 'http://localhost:9000/products/galaxy-z-fold-5.png', b'1', 1, '2026-03-20 15:30:00'),
(17, 16, 'http://localhost:9000/products/apple-watch-s9.png', b'1', 1, '2026-04-01 10:00:00'),
(18, 17, 'http://localhost:9000/products/apple-watch-ultra-2.png', b'1', 1, '2026-04-05 11:30:00'),
(19, 18, 'http://localhost:9000/products/garmin-forerunner-265.png', b'1', 1, '2026-04-10 16:00:00'),
(20, 19, 'http://localhost:9000/products/jbl-charge-5.png', b'1', 1, '2026-04-15 08:30:00'),
(21, 20, 'http://localhost:9000/products/logitech-mx-master-3s.png', b'1', 1, '2026-04-20 14:00:00'),
(22, 21, 'http://localhost:9000/products/logitech-mx-mechanical.png', b'1', 1, '2026-04-25 10:00:00'),
(23, 22, 'http://localhost:9000/products/sony-wf-1000xm5.png', b'1', 1, '2026-05-01 09:30:00'),
(24, 23, 'http://localhost:9000/products/sennheiser-momentum-4.png', b'1', 1, '2026-05-05 13:45:00'),
(25, 24, 'http://localhost:9000/products/baseus-blade-100w.png', b'1', 1, '2026-05-10 15:00:00'),
(26, 25, 'http://localhost:9000/products/lg-ultragear-monitor.png', b'1', 1, '2026-05-15 16:30:00'),
(27, 11, 'http://localhost:9000/products/variants/dell-xps-13.png', b'0', 2, '2026-03-01 10:00:00'),
(28, 14, 'http://localhost:9000/products/variants/pixel-8-pro.png', b'0', 2, '2026-03-15 09:00:00'),
(29, 15, 'http://localhost:9000/products/variants/galaxy-z-fold-5.png', b'0', 2, '2026-03-20 15:30:00'),
(30, 16, 'http://localhost:9000/products/variants/apple-watch-s9.png', b'0', 2, '2026-04-01 10:00:00'),
(31, 20, 'http://localhost:9000/products/variants/logitech-mx-master-3s.png', b'0', 2, '2026-04-20 14:00:00');

INSERT INTO product_specs (spec_id, product_product_id, spec_name, spec_value, created_at) VALUES
(8, 11, 'Vi xử lý', 'Intel Core Ultra 7 155H 16 nhân 22 luồng', '2026-03-01 10:00:00'),
(9, 11, 'Màn hình', '13.4 inch FHD+ 120Hz Eyesafe', '2026-03-01 10:00:00'),
(10, 12, 'Khung vỏ', 'Sợi Carbon và Hợp kim Magie siêu bền', '2026-03-05 11:00:00'),
(11, 12, 'Trọng lượng', '1.09 kg', '2026-03-05 11:00:00'),
(12, 13, 'Card đồ họa', 'NVIDIA GeForce RTX 4060 8GB GDDR6', '2026-03-10 14:00:00'),
(13, 14, 'Cảm biến chính', '50 MP Octa PD, f/1.68, OIS quang học', '2026-03-15 09:00:00'),
(14, 15, 'Màn hình chính', '7.6 inch Dynamic AMOLED 2X 120Hz', '2026-03-20 15:30:00'),
(15, 15, 'Kháng nước', 'Chuẩn kháng nước cao cấp IPX8', '2026-03-20 15:30:00'),
(16, 16, 'Độ sáng màn hình', '2000 nits cực sáng ngoài trời', '2026-04-01 10:00:00'),
(17, 17, 'Chuẩn độ sâu', 'Đo độ sâu lặn biển chứng nhận EN13319 40m', '2026-04-05 11:30:00'),
(18, 18, 'Cảm biến nhịp tim', 'Elevate thế hệ mới nhất đo liên tục 24/7', '2026-04-10 16:00:00'),
(19, 19, 'Công suất loa', '40W RMS (Woofer 30W + Tweeter 10W)', '2026-04-15 08:30:00'),
(20, 20, 'Cảm biến', 'Darkfield High Precision 8000 DPI', '2026-04-20 14:00:00'),
(21, 21, 'Tuổi thọ pin', 'Lên đến 15 ngày có đèn nền, 10 tháng tắt đèn', '2026-04-25 10:00:00'),
(22, 22, 'Bộ giải mã', 'LDAC, AAC, SBC, LC3', '2026-05-01 09:30:00'),
(23, 23, 'Kích thước củ loa', '42mm Dynamic Transducer audiophile', '2026-05-05 13:45:00'),
(24, 24, 'Giao thức sạc', 'PD 3.0, QC 4.0+, SCP, FCP, AFC', '2026-05-10 15:00:00'),
(25, 25, 'Tần số quét', '240Hz, phản hồi cực nhanh 0.03ms GtG', '2026-05-15 16:30:00'),
(26, 14, 'Cập nhật phần mềm', 'Cam kết hỗ trợ 7 năm hệ điều hành và bảo mật', '2026-03-15 09:00:00'),
(27, 17, 'Thời lượng pin', 'Lên đến 72 giờ ở chế độ tiết kiệm năng lượng', '2026-04-05 11:30:00');

INSERT INTO inventories (inventory_id, variant_variant_id, quantity, reserved_quantity, min_threshold, updated_at) VALUES
(13, 12, 15, 0, 3, '2026-03-01 10:00:00'),
(14, 13, 10, 0, 2, '2026-03-01 10:00:00'),
(15, 14, 20, 0, 5, '2026-03-05 11:00:00'),
(16, 15, 12, 0, 2, '2026-03-05 11:00:00'),
(17, 16, 18, 0, 4, '2026-03-10 14:00:00'),
(18, 17, 25, 0, 5, '2026-03-15 09:00:00'),
(19, 18, 22, 0, 5, '2026-03-15 09:00:00'),
(20, 19, 14, 0, 3, '2026-03-20 15:30:00'),
(21, 20, 10, 0, 2, '2026-03-20 15:30:00'),
(22, 21, 35, 0, 8, '2026-04-01 10:00:00'),
(23, 22, 30, 0, 8, '2026-04-01 10:00:00'),
(24, 23, 15, 0, 3, '2026-04-05 11:30:00'),
(25, 24, 28, 0, 5, '2026-04-10 16:00:00'),
(26, 25, 50, 0, 10, '2026-04-15 08:30:00'),
(27, 26, 60, 0, 15, '2026-04-20 14:00:00'),
(28, 27, 40, 0, 10, '2026-04-25 10:00:00'),
(29, 28, 30, 0, 5, '2026-05-01 09:30:00'),
(30, 29, 25, 0, 5, '2026-05-05 13:45:00'),
(31, 30, 80, 0, 20, '2026-05-10 15:00:00'),
(32, 31, 16, 0, 3, '2026-05-15 16:30:00');

INSERT INTO promotions (promotion_id, promotion_name, description, discount_type, discount_value, is_active, start_date, end_date, created_by_user_id, created_at, updated_at) VALUES
(2, 'Đại Tiệc Âm Thanh & Phụ Kiện', 'Giảm sốc 15% cho các dòng loa di động JBL, tai nghe Sony và chuột Logitech', 'PERCENTAGE', 15.00, b'1', '2026-10-01 00:00:00', '2026-12-31 23:59:59', 1, '2026-10-01 00:00:00', '2026-10-01 00:00:00'),
(3, 'Tuần Lễ Laptop & Smartwatch', 'Trợ giá trực tiếp cho sinh viên và giới văn phòng mua laptop XPS, ThinkPad, Apple Watch', 'FIXED_AMOUNT', 2000000.00, b'1', '2026-10-15 00:00:00', '2026-11-30 23:59:59', 1, '2026-10-15 00:00:00', '2026-10-15 00:00:00'),
(4, 'Black Friday Siêu Deal Công Nghệ', 'Sự kiện siêu giảm giá công nghệ toàn diện dịp mua sắm cuối năm', 'PERCENTAGE', 10.00, b'1', '2026-11-20 00:00:00', '2026-11-30 23:59:59', 1, '2026-11-01 00:00:00', '2026-11-01 00:00:00');

INSERT INTO promotion_variants (promo_variant_id, promotion_promotion_id, variant_variant_id, promotional_price, created_at) VALUES
(5, 2, 25, 3300000.00, '2026-10-01 00:00:00'),
(6, 2, 26, 2190000.00, '2026-10-01 00:00:00'),
(7, 2, 27, 3290000.00, '2026-10-01 00:00:00'),
(8, 2, 28, 5690000.00, '2026-10-01 00:00:00'),
(9, 2, 29, 6990000.00, '2026-10-01 00:00:00'),
(10, 2, 30, 1190000.00, '2026-10-01 00:00:00'),
(11, 3, 12, 40990000.00, '2026-10-15 00:00:00'),
(12, 3, 14, 43990000.00, '2026-10-15 00:00:00'),
(13, 3, 16, 46990000.00, '2026-10-15 00:00:00'),
(14, 3, 21, 9490000.00, '2026-10-15 00:00:00'),
(15, 3, 23, 19490000.00, '2026-10-15 00:00:00'),
(16, 3, 24, 10490000.00, '2026-10-15 00:00:00'),
(17, 4, 17, 21990000.00, '2026-11-01 00:00:00'),
(18, 4, 19, 29990000.00, '2026-11-01 00:00:00'),
(19, 4, 31, 17990000.00, '2026-11-01 00:00:00');

INSERT INTO vouchers (voucher_id, voucher_code, voucher_name, discount_type, discount_value, min_order_value, max_discount_amount, usage_limit, used_count, is_active, start_date, end_date, created_at, updated_at) VALUES
(4, 'SMARTWATCH10', 'Giảm 10% khi sắm Apple Watch hoặc Garmin', 'PERCENTAGE', 10.00, 8000000.00, 1000000.00, 200, 15, b'1', '2026-10-01 00:00:00', '2026-12-31 23:59:59', '2026-10-01 00:00:00', '2026-10-01 00:00:00'),
(5, 'LAPTOPVIP', 'Giảm ngay 1.5 Triệu cho Laptop cao cấp', 'FIXED_AMOUNT', 1500000.00, 30000000.00, 1500000.00, 100, 8, b'1', '2026-10-01 00:00:00', '2026-12-31 23:59:59', '2026-10-01 00:00:00', '2026-10-01 00:00:00'),
(6, 'AUDIO500K', 'Giảm 500K cho đơn tai nghe và loa từ 3 Triệu', 'FIXED_AMOUNT', 500000.00, 3000000.00, 500000.00, 300, 42, b'1', '2026-10-01 00:00:00', '2026-12-31 23:59:59', '2026-10-01 00:00:00', '2026-10-01 00:00:00'),
(7, 'LOGITECH100', 'Voucher giảm 100K cho phụ kiện chuột bàn phím', 'FIXED_AMOUNT', 100000.00, 1500000.00, 100000.00, 500, 78, b'1', '2026-10-01 00:00:00', '2026-12-31 23:59:59', '2026-10-01 00:00:00', '2026-10-01 00:00:00'),
(8, 'PIXELPRO1TR', 'Khuyến mãi 1 Triệu khi rước Google Pixel 8 Pro', 'FIXED_AMOUNT', 1000000.00, 20000000.00, 1000000.00, 150, 12, b'1', '2026-10-01 00:00:00', '2026-12-31 23:59:59', '2026-10-01 00:00:00', '2026-10-01 00:00:00'),
(9, 'FREESHIPPRO', 'Miễn phí giao hàng hỏa tốc toàn quốc', 'FIXED_AMOUNT', 50000.00, 500000.00, 50000.00, 1000, 230, b'1', '2026-10-01 00:00:00', '2026-12-31 23:59:59', '2026-10-01 00:00:00', '2026-10-01 00:00:00'),
(10, 'NEXTGENWELCOME', 'Chào đón thành viên mới giảm 5% tối đa 300K', 'PERCENTAGE', 5.00, 1000000.00, 300000.00, 5000, 620, b'1', '2026-01-01 00:00:00', '2026-12-31 23:59:59', '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
(11, 'GAMINGZONE', 'Ưu đãi màn hình OLED & Laptop ROG Gaming 8%', 'PERCENTAGE', 8.00, 15000000.00, 2000000.00, 150, 9, b'1', '2026-10-15 00:00:00', '2026-12-15 23:59:59', '2026-10-15 00:00:00', '2026-10-15 00:00:00'),
(12, 'BASEUSPOWER', 'Giảm 150K khi mua pin sạc dự phòng Baseus', 'FIXED_AMOUNT', 150000.00, 1000000.00, 150000.00, 400, 53, b'1', '2026-10-01 00:00:00', '2026-12-31 23:59:59', '2026-10-01 00:00:00', '2026-10-01 00:00:00'),
(13, 'FLASHDEAL2026', 'Mã độc quyền giờ vàng giảm 12% tối đa 1.2 Triệu', 'PERCENTAGE', 12.00, 5000000.00, 1200000.00, 100, 4, b'1', '2026-11-01 00:00:00', '2026-11-30 23:59:59', '2026-11-01 00:00:00', '2026-11-01 00:00:00');

INSERT INTO carts (cart_id, user_user_id, created_at, updated_at) VALUES
(4, 11, '2026-10-01 10:00:00', '2026-10-01 10:00:00'),
(5, 12, '2026-10-02 11:00:00', '2026-10-02 11:00:00'),
(6, 13, '2026-10-05 14:00:00', '2026-10-05 14:00:00'),
(7, 14, '2026-10-10 09:30:00', '2026-10-10 09:30:00'),
(8, 15, '2026-10-12 16:00:00', '2026-10-12 16:00:00'),
(9, 16, '2026-10-15 13:20:00', '2026-10-15 13:20:00'),
(10, 17, '2026-10-18 15:40:00', '2026-10-18 15:40:00'),
(11, 18, '2026-10-20 10:15:00', '2026-10-20 10:15:00'),
(12, 19, '2026-10-22 11:45:00', '2026-10-22 11:45:00'),
(13, 20, '2026-10-25 09:10:00', '2026-10-25 09:10:00');

INSERT INTO cart_items (cart_item_id, cart_cart_id, variant_variant_id, quantity, created_at, updated_at) VALUES
(5, 4, 12, 1, '2026-10-01 10:05:00', '2026-10-01 10:05:00'),
(6, 4, 26, 1, '2026-10-01 10:06:00', '2026-10-01 10:06:00'),
(7, 5, 21, 1, '2026-10-02 11:05:00', '2026-10-02 11:05:00'),
(8, 6, 17, 1, '2026-10-05 14:05:00', '2026-10-05 14:05:00'),
(9, 6, 30, 2, '2026-10-05 14:06:00', '2026-10-05 14:06:00'),
(10, 7, 25, 1, '2026-10-10 09:35:00', '2026-10-10 09:35:00'),
(11, 8, 14, 1, '2026-10-12 16:05:00', '2026-10-12 16:05:00'),
(12, 9, 28, 1, '2026-10-15 13:25:00', '2026-10-15 13:25:00'),
(13, 10, 19, 1, '2026-10-18 15:45:00', '2026-10-18 15:45:00'),
(14, 10, 27, 1, '2026-10-18 15:46:00', '2026-10-18 15:46:00'),
(15, 11, 23, 1, '2026-10-20 10:20:00', '2026-10-20 10:20:00'),
(16, 12, 24, 1, '2026-10-22 11:50:00', '2026-10-22 11:50:00'),
(17, 12, 29, 1, '2026-10-22 11:51:00', '2026-10-22 11:51:00'),
(18, 13, 31, 1, '2026-10-25 09:15:00', '2026-10-25 09:15:00'),
(19, 13, 26, 1, '2026-10-25 09:16:00', '2026-10-25 09:16:00');

INSERT INTO orders (order_id, order_code, user_user_id, recipient_name, recipient_phone, shipping_address, total_amount, shipping_fee, discount_amount, final_amount, payment_method, payment_status, status, note, cancel_reason, voucher_voucher_id, completed_at, version, created_at, updated_at) VALUES
(7, 'DH-20261005-201', 11, 'Lê Hoàng Nam', '0903112233', 'Số 54 Liễu Giai, Cống Vị, Ba Đình, Hà Nội', 42990000.00, 0.00, 1500000.00, 41490000.00, 'BANK_TRANSFER', 'PAID', 'COMPLETED', 'Giao tận tay', NULL, 5, '2026-10-08 16:00:00', 0, '2026-10-05 10:00:00', '2026-10-08 16:00:00'),
(8, 'DH-20261008-202', 12, 'Nguyễn Mai Anh', '0904223344', 'Số 102 Lê Lợi, Bến Nghé, Quận 1, TP. Hồ Chí Minh', 10490000.00, 0.00, 1000000.00, 9490000.00, 'COD', 'PAID', 'COMPLETED', 'Kiểm tra hộp nguyên seal', NULL, 4, '2026-10-11 11:30:00', 0, '2026-10-08 14:15:00', '2026-10-11 11:30:00'),
(9, 'DH-20261012-203', 13, 'Trương Quốc Bảo', '0905334455', 'Số 15 Quang Trung, Hải Châu, Đà Nẵng', 23990000.00, 0.00, 1000000.00, 22990000.00, 'BANK_TRANSFER', 'PAID', 'COMPLETED', 'Đóng gói bọc xốp chống sốc', NULL, 8, '2026-10-15 15:00:00', 0, '2026-10-12 09:30:00', '2026-10-15 15:00:00'),
(10, 'DH-20261016-204', 14, 'Võ Thị Bích Trâm', '0906445566', 'Tòa nhà Bitexco, 2 Hải Triều, Quận 1, TP. Hồ Chí Minh', 3890000.00, 30000.00, 500000.00, 3420000.00, 'COD', 'PAID', 'COMPLETED', 'Giao giờ hành chính', NULL, 6, '2026-10-18 14:30:00', 0, '2026-10-16 11:00:00', '2026-10-18 14:30:00'),
(11, 'DH-20261020-205', 15, 'Đặng Hải Đăng', '0907556677', 'Số 22 Hai Bà Trưng, TP. Huế', 45990000.00, 0.00, 1500000.00, 44490000.00, 'BANK_TRANSFER', 'PAID', 'SHIPPING', 'Chuyển phát nhanh', NULL, 5, NULL, 0, '2026-10-20 15:20:00', '2026-10-21 09:00:00'),
(12, 'DH-20261023-206', 16, 'Phan Thùy Dung', '0908667788', 'Số 89 Nguyễn Thị Minh Khai, TP. Vũng Tàu', 6490000.00, 0.00, 500000.00, 5990000.00, 'BANK_TRANSFER', 'PAID', 'SHIPPING', 'Gọi trước khi giao 30p', NULL, 6, NULL, 0, '2026-10-23 08:45:00', '2026-10-24 10:00:00'),
(13, 'DH-20261026-207', 17, 'Dương Gia Huy', '0909778899', 'Số 76 Trần Hưng Đạo, Tuy Hòa, Phú Yên', 32990000.00, 0.00, 1200000.00, 31790000.00, 'BANK_TRANSFER', 'PAID', 'CONFIRMED', 'Hàng giá trị cao', NULL, 13, NULL, 0, '2026-10-26 16:00:00', '2026-10-26 16:30:00'),
(14, 'DH-20261028-208', 18, 'Ngô Cẩm Tiên', '0918112233', 'Số 41 Lê Duẩn, Hải Châu, Đà Nẵng', 21490000.00, 0.00, 1000000.00, 20490000.00, 'COD', 'PENDING', 'PROCESSING', 'Giao buổi sáng', NULL, 4, NULL, 0, '2026-10-28 10:10:00', '2026-10-28 10:30:00'),
(15, 'DH-20261030-209', 19, 'Hồ Vĩnh Khoa', '0919223344', 'Số 120 Điện Biên Phủ, Quận 1, TP. Hồ Chí Minh', 7990000.00, 0.00, 500000.00, 7490000.00, 'BANK_TRANSFER', 'PAID', 'CONFIRMED', 'Kiểm tra kỹ phụ kiện cáp sạc', NULL, 6, NULL, 0, '2026-10-30 13:40:00', '2026-10-30 14:00:00'),
(16, 'DH-20261102-213', 20, 'Đoàn Nhật Minh', '0917334455', 'Số 33 Đại lộ Hòa Bình, Ninh Kiều, Cần Thơ', 19990000.00, 0.00, 1599200.00, 18390800.00, 'BANK_TRANSFER', 'PAID', 'SHIPPING', 'Hàng cồng kềnh cẩn thận vỡ màn hình', NULL, 11, NULL, 0, '2026-11-02 09:15:00', '2026-11-03 08:30:00'),
(17, 'DH-20261104-214', 21, 'Tạ Mỹ Linh', '0916445566', 'Số 90 Phan Bội Châu, Buôn Ma Thuột, Đắk Lắk', 2490000.00, 30000.00, 100000.00, 2420000.00, 'COD', 'PENDING', 'PENDING_PAYMENT', 'Giao tận nơi', NULL, 7, NULL, 0, '2026-11-04 14:20:00', '2026-11-04 14:20:00'),
(18, 'DH-20261105-215', 22, 'Phùng Thanh Độ', '0915556677', 'Khu đô thị Ecopark, Văn Giang, Hưng Yên', 3690000.00, 0.00, 100000.00, 3590000.00, 'BANK_TRANSFER', 'PAID', 'CONFIRMED', 'Giao cho lễ tân tòa nhà', NULL, 7, NULL, 0, '2026-11-05 11:00:00', '2026-11-05 11:15:00'),
(19, 'DH-20261106-216', 25, 'Trịnh Công Sơn', '0912889900', 'Số 17 Pasteur, Phường 4, TP. Đà Lạt', 1390000.00, 30000.00, 150000.00, 1270000.00, 'COD', 'PENDING', 'PROCESSING', 'Đóng hộp chống va đập', NULL, 12, NULL, 0, '2026-11-06 16:30:00', '2026-11-06 17:00:00'),
(20, 'DH-20261107-217', 11, 'Lê Hoàng Nam', '0903112233', 'Tầng 12 Keangnam Landmark 72, Nam Từ Liêm, Hà Nội', 2490000.00, 0.00, 100000.00, 2390000.00, 'BANK_TRANSFER', 'PAID', 'CONFIRMED', 'Giao lên tầng 12', NULL, 7, NULL, 0, '2026-11-07 10:00:00', '2026-11-07 10:15:00'),
(21, 'DH-20261108-218', 12, 'Nguyễn Mai Anh', '0904223344', 'Căn hộ chung cư Vinhomes Central Park, Bình Thạnh, TP. Hồ Chí Minh', 11690000.00, 0.00, 1000000.00, 10690000.00, 'BANK_TRANSFER', 'PAID', 'COMPLETED', 'Đã nhận máy', NULL, 4, '2026-11-10 15:30:00', 0, '2026-11-08 09:00:00', '2026-11-10 15:30:00');

INSERT INTO order_items (order_item_id, order_order_id, variant_variant_id, sku_snapshot, product_name_snapshot, color_snapshot, rom_snapshot, image_url, price, quantity, total_price, created_at) VALUES
(11, 7, 12, 'DELL-XPS13-512-SLV', 'Dell XPS 13 9340 (2024)', 'Bạc Platinum', '512GB', 'http://localhost:9000/products/variants/dell-xps-13.png', 42990000.00, 1, 42990000.00, '2026-10-05 10:00:00'),
(12, 8, 21, 'AW-S9-45-MID', 'Apple Watch Series 9 GPS 45mm', 'Đen Midnight', 'N/A', 'http://localhost:9000/products/variants/apple-watch-s9.png', 10490000.00, 1, 10490000.00, '2026-10-08 14:15:00'),
(13, 9, 17, 'PIXEL8P-128-BLU', 'Google Pixel 8 Pro', 'Xanh Vịnh Bay', '128GB', 'http://localhost:9000/products/variants/pixel-8-pro.png', 23990000.00, 1, 23990000.00, '2026-10-12 09:30:00'),
(14, 10, 25, 'JBL-CHARGE5-BLK', 'Loa Bluetooth JBL Charge 5', 'Đen', 'N/A', 'http://localhost:9000/products/variants/jbl-charge-5.png', 3890000.00, 1, 3890000.00, '2026-10-16 11:00:00'),
(15, 11, 14, 'THINKPAD-X1-512-BLK', 'ThinkPad X1 Carbon Gen 12', 'Đen Nhung', '512GB', 'http://localhost:9000/products/variants/thinkpad-x1.png', 45990000.00, 1, 45990000.00, '2026-10-20 15:20:00'),
(16, 12, 28, 'SONY-WF-XM5-BLK', 'Tai nghe Sony WF-1000XM5', 'Đen Nhám', 'N/A', 'http://localhost:9000/products/variants/sony-wf-1000xm5.png', 6490000.00, 1, 6490000.00, '2026-10-23 08:45:00'),
(17, 13, 19, 'ZFOLD5-256-BLU', 'Galaxy Z Fold 5 256GB', 'Xanh Icy', '256GB', 'http://localhost:9000/products/variants/galaxy-z-fold-5.png', 32990000.00, 1, 32990000.00, '2026-10-26 16:00:00'),
(18, 14, 23, 'AW-ULTRA2-49-TRAIL', 'Apple Watch Ultra 2 49mm', 'Titan Tự Nhiên', 'N/A', 'http://localhost:9000/products/variants/apple-watch-ultra-2.png', 21490000.00, 1, 21490000.00, '2026-10-28 10:10:00'),
(19, 15, 29, 'SENN-MOM4-BLK', 'Tai nghe Sennheiser Momentum 4', 'Đen Kim Loại', 'N/A', 'http://localhost:9000/products/variants/sennheiser-momentum-4.png', 7990000.00, 1, 7990000.00, '2026-10-30 13:40:00'),
(20, 16, 31, 'LG-27GR95QE-OLED', 'Màn hình LG UltraGear 27GR95QE', 'Đen Gaming', 'N/A', 'http://localhost:9000/products/variants/lg-ultragear-monitor.png', 19990000.00, 1, 19990000.00, '2026-11-02 09:15:00'),
(21, 17, 26, 'LOGI-MXM3S-GRY', 'Chuột Logitech MX Master 3S', 'Xám Graphite', 'N/A', 'http://localhost:9000/products/variants/logitech-mx-master-3s.png', 2490000.00, 1, 2490000.00, '2026-11-04 14:20:00'),
(22, 18, 27, 'LOGI-MXMECH-TAC', 'Bàn phím cơ Logitech MX Mechanical', 'Xám Không Gian', 'N/A', 'http://localhost:9000/products/variants/logitech-mx-mechanical.png', 3690000.00, 1, 3690000.00, '2026-11-05 11:00:00'),
(23, 19, 30, 'BASEUS-BLADE-100W', 'Pin dự phòng Baseus Blade 100W', 'Đen Nhám', 'N/A', 'http://localhost:9000/products/variants/baseus-blade-100w.png', 1390000.00, 1, 1390000.00, '2026-11-06 16:30:00'),
(24, 20, 26, 'LOGI-MXM3S-GRY', 'Chuột Logitech MX Master 3S', 'Xám Graphite', 'N/A', 'http://localhost:9000/products/variants/logitech-mx-master-3s.png', 2490000.00, 1, 2490000.00, '2026-11-07 10:00:00'),
(25, 21, 24, 'GARMIN-FR265-BLK', 'Garmin Forerunner 265', 'Đen Viền Vàng', 'N/A', 'http://localhost:9000/products/variants/garmin-forerunner-265.png', 11690000.00, 1, 11690000.00, '2026-11-08 09:00:00'),
(26, 7, 26, 'LOGI-MXM3S-GRY', 'Chuột Logitech MX Master 3S', 'Xám Graphite', 'N/A', 'http://localhost:9000/products/variants/logitech-mx-master-3s.png', 2490000.00, 1, 2490000.00, '2026-10-05 10:00:00'),
(27, 9, 30, 'BASEUS-BLADE-100W', 'Pin dự phòng Baseus Blade 100W', 'Đen Nhám', 'N/A', 'http://localhost:9000/products/variants/baseus-blade-100w.png', 1390000.00, 1, 1390000.00, '2026-10-12 09:30:00'),
(28, 13, 27, 'LOGI-MXMECH-TAC', 'Bàn phím cơ Logitech MX Mechanical', 'Xám Không Gian', 'N/A', 'http://localhost:9000/products/variants/logitech-mx-mechanical.png', 3690000.00, 1, 3690000.00, '2026-10-26 16:00:00'),
(29, 16, 26, 'LOGI-MXM3S-GRY', 'Chuột Logitech MX Master 3S', 'Xám Graphite', 'N/A', 'http://localhost:9000/products/variants/logitech-mx-master-3s.png', 2490000.00, 1, 2490000.00, '2026-11-02 09:15:00'),
(30, 21, 30, 'BASEUS-BLADE-100W', 'Pin dự phòng Baseus Blade 100W', 'Đen Nhám', 'N/A', 'http://localhost:9000/products/variants/baseus-blade-100w.png', 1390000.00, 1, 1390000.00, '2026-11-08 09:00:00');

INSERT INTO payments (payment_id, order_order_id, payment_method, payment_provider, status, amount, transaction_ref, paid_at, created_at, updated_at) VALUES
(10, 7, 'BANK_TRANSFER', 'VNPAY', 'SUCCESS', 41490000.00, 'VNP20261005201', '2026-10-05 10:06:00', '2026-10-05 10:05:00', '2026-10-05 10:06:00'),
(11, 8, 'COD', 'INTERNAL', 'SUCCESS', 9490000.00, 'COD20261008202', '2026-10-11 11:30:00', '2026-10-11 11:30:00', '2026-10-11 11:30:00'),
(12, 9, 'BANK_TRANSFER', 'VIETCOMBANK', 'SUCCESS', 22990000.00, 'MB20261012203', '2026-10-12 09:35:00', '2026-10-12 09:35:00', '2026-10-12 09:35:00'),
(13, 10, 'COD', 'INTERNAL', 'SUCCESS', 3420000.00, 'COD20261016204', '2026-10-18 14:30:00', '2026-10-18 14:30:00', '2026-10-18 14:30:00'),
(14, 11, 'BANK_TRANSFER', 'VNPAY', 'SUCCESS', 44490000.00, 'VNP20261020205', '2026-10-20 15:25:00', '2026-10-20 15:25:00', '2026-10-20 15:25:00'),
(15, 12, 'BANK_TRANSFER', 'VIETCOMBANK', 'SUCCESS', 5990000.00, 'TCB20261023206', '2026-10-23 08:50:00', '2026-10-23 08:50:00', '2026-10-23 08:50:00'),
(16, 13, 'BANK_TRANSFER', 'VNPAY', 'SUCCESS', 31790000.00, 'VNP20261026207', '2026-10-26 16:10:00', '2026-10-26 16:10:00', '2026-10-26 16:10:00'),
(17, 14, 'COD', 'INTERNAL', 'PENDING', 20490000.00, 'COD20261028208', NULL, '2026-10-28 10:10:00', '2026-10-28 10:10:00'),
(18, 15, 'BANK_TRANSFER', 'VIETCOMBANK', 'SUCCESS', 7490000.00, 'BIDV20261030209', '2026-10-30 13:45:00', '2026-10-30 13:45:00', '2026-10-30 13:45:00'),
(19, 16, 'BANK_TRANSFER', 'VNPAY', 'SUCCESS', 18390800.00, 'VNP20261102213', '2026-11-02 09:20:00', '2026-11-02 09:20:00', '2026-11-02 09:20:00'),
(20, 17, 'COD', 'INTERNAL', 'PENDING', 2420000.00, 'COD20261104214', NULL, '2026-11-04 14:20:00', '2026-11-04 14:20:00'),
(21, 18, 'BANK_TRANSFER', 'VIETCOMBANK', 'SUCCESS', 3590000.00, 'VCB20261105215', '2026-11-05 11:05:00', '2026-11-05 11:05:00', '2026-11-05 11:05:00'),
(22, 19, 'COD', 'INTERNAL', 'PENDING', 1270000.00, 'COD20261106216', NULL, '2026-11-06 16:30:00', '2026-11-06 16:30:00'),
(23, 20, 'BANK_TRANSFER', 'VNPAY', 'SUCCESS', 2390000.00, 'VNP20261107217', '2026-11-07 10:05:00', '2026-11-07 10:05:00', '2026-11-07 10:05:00'),
(24, 21, 'BANK_TRANSFER', 'VIETCOMBANK', 'SUCCESS', 10690000.00, 'TCB20261108218', '2026-11-08 09:10:00', '2026-11-08 09:10:00', '2026-11-08 09:10:00');

INSERT INTO shipments (shipment_id, order_order_id, tracking_number, carrier_name, shipping_status, estimated_delivery, shipped_at, delivered_at, fail_reason, notes, version, updated_by_user_id, created_at, updated_at) VALUES
(7, 7, 'GHN-20261005201', 'Giao Hàng Nhanh Express', 'DELIVERED', '2026-10-08 17:00:00', '2026-10-06 09:00:00', '2026-10-08 16:00:00', NULL, 'Giao thành công, khách đã ký nhận', 0, 2, '2026-10-05 11:00:00', '2026-10-08 16:00:00'),
(8, 8, 'GHTK-5521092', 'Giao Hàng Tiết Kiệm', 'DELIVERED', '2026-10-11 18:00:00', '2026-10-09 10:00:00', '2026-10-11 11:30:00', NULL, 'Thu tiền COD 9.490.000đ', 0, 3, '2026-10-08 15:00:00', '2026-10-11 11:30:00'),
(9, 9, 'VTP-9921823', 'Viettel Post', 'DELIVERED', '2026-10-15 17:00:00', '2026-10-13 08:30:00', '2026-10-15 15:00:00', NULL, 'Khách hàng nhận tại bưu cục Đà Nẵng', 0, 2, '2026-10-12 10:00:00', '2026-10-15 15:00:00'),
(10, 10, 'GHN-20261016204', 'Giao Hàng Nhanh Express', 'DELIVERED', '2026-10-18 18:00:00', '2026-10-17 09:00:00', '2026-10-18 14:30:00', NULL, 'Giao tận tay lễ tân tòa Bitexco', 0, 2, '2026-10-16 14:00:00', '2026-10-18 14:30:00'),
(11, 11, 'VTP-7712390', 'Viettel Post', 'IN_TRANSIT', '2026-10-23 18:00:00', '2026-10-21 09:00:00', NULL, NULL, 'Đang luân chuyển qua bưu cục trung tâm miền Trung', 0, 3, '2026-10-20 16:00:00', '2026-10-21 09:00:00'),
(12, 12, 'GHTK-8812304', 'Giao Hàng Tiết Kiệm', 'IN_TRANSIT', '2026-10-26 17:00:00', '2026-10-24 10:00:00', NULL, NULL, 'Đang giao tuyến TP. Vũng Tàu', 0, 2, '2026-10-23 09:30:00', '2026-10-24 10:00:00'),
(13, 13, 'GHN-20261026207', 'Giao Hàng Nhanh Express', 'READY_TO_PICK', '2026-10-29 18:00:00', NULL, NULL, NULL, 'Đã đóng hộp dán seal niêm phong', 0, 2, '2026-10-26 16:45:00', '2026-10-26 16:45:00'),
(14, 14, 'VTP-3312908', 'Viettel Post', 'READY_TO_PICK', '2026-10-31 18:00:00', NULL, NULL, NULL, 'Chờ bàn giao bưu tá chiều', 0, 3, '2026-10-28 11:00:00', '2026-10-28 11:00:00'),
(15, 15, 'GHTK-1122994', 'Giao Hàng Tiết Kiệm', 'READY_TO_PICK', '2026-11-02 18:00:00', NULL, NULL, NULL, 'Chờ gom hàng đợt 2', 0, 2, '2026-10-30 14:30:00', '2026-10-30 14:30:00'),
(16, 16, 'GHN-20261102213', 'Giao Hàng Nhanh Express', 'IN_TRANSIT', '2026-11-05 18:00:00', '2026-11-03 08:30:00', NULL, NULL, 'Kiện hàng cồng kềnh xe tải vận chuyển Cần Thơ', 0, 2, '2026-11-02 10:00:00', '2026-11-03 08:30:00'),
(17, 17, 'VTP-6655441', 'Viettel Post', 'READY_TO_PICK', '2026-11-07 18:00:00', NULL, NULL, NULL, 'Đơn chờ thu COD', 0, 3, '2026-11-04 15:00:00', '2026-11-04 15:00:00'),
(18, 18, 'GHTK-9900112', 'Giao Hàng Tiết Kiệm', 'READY_TO_PICK', '2026-11-08 18:00:00', NULL, NULL, NULL, 'Đã chuẩn bị hàng', 0, 2, '2026-11-05 11:30:00', '2026-11-05 11:30:00'),
(19, 19, 'GHN-20261106216', 'Giao Hàng Nhanh Express', 'READY_TO_PICK', '2026-11-09 18:00:00', NULL, NULL, NULL, 'Đóng gói chờ điều phối', 0, 3, '2026-11-06 17:15:00', '2026-11-06 17:15:00'),
(20, 20, 'GHTK-4433221', 'Giao Hàng Tiết Kiệm', 'READY_TO_PICK', '2026-11-10 18:00:00', NULL, NULL, NULL, 'Đơn chuyển phát nhanh nội thành Hà Nội', 0, 2, '2026-11-07 10:30:00', '2026-11-07 10:30:00'),
(21, 21, 'GHN-20261108218', 'Giao Hàng Nhanh Express', 'DELIVERED', '2026-11-10 18:00:00', '2026-11-09 08:30:00', '2026-11-10 15:30:00', NULL, 'Khách hàng nhận tại sảnh chung cư', 0, 2, '2026-11-08 10:00:00', '2026-11-10 15:30:00');

INSERT INTO order_status_history (history_id, order_order_id, changed_by_user_id, previous_status, new_status, note, created_at) VALUES
(11, 7, 11, NULL, 'PENDING_PAYMENT', 'Khách tạo đơn hàng thanh toán qua VNPAY', '2026-10-05 10:00:00'),
(12, 7, 1, 'PENDING_PAYMENT', 'CONFIRMED', 'Thanh toán thành công qua VNPAY', '2026-10-05 10:06:00'),
(13, 7, 2, 'CONFIRMED', 'SHIPPING', 'Bàn giao cho GHN Express', '2026-10-06 09:00:00'),
(14, 7, 2, 'SHIPPING', 'COMPLETED', 'Khách đã nhận kiện hàng thành công', '2026-10-08 16:00:00'),
(15, 8, 12, NULL, 'CONFIRMED', 'Đặt hàng COD thành công', '2026-10-08 14:15:00'),
(16, 8, 3, 'CONFIRMED', 'SHIPPING', 'GHTK đã lấy hàng từ kho', '2026-10-09 10:00:00'),
(17, 8, 3, 'SHIPPING', 'COMPLETED', 'Giao hàng và thu COD thành công', '2026-10-11 11:30:00'),
(18, 9, 13, NULL, 'PENDING_PAYMENT', 'Tạo đơn đặt mua Google Pixel 8 Pro', '2026-10-12 09:30:00'),
(19, 9, 1, 'PENDING_PAYMENT', 'CONFIRMED', 'Xác nhận chuyển khoản VietQR', '2026-10-12 09:35:00'),
(20, 9, 2, 'CONFIRMED', 'SHIPPING', 'Viettel Post nhận đơn', '2026-10-13 08:30:00'),
(21, 9, 2, 'SHIPPING', 'COMPLETED', 'Giao hàng thành công', '2026-10-15 15:00:00'),
(22, 10, 14, NULL, 'CONFIRMED', 'Khách đặt đơn COD loa JBL', '2026-10-16 11:00:00'),
(23, 10, 2, 'CONFIRMED', 'SHIPPING', 'Bàn giao bưu tá GHN', '2026-10-17 09:00:00'),
(24, 10, 2, 'SHIPPING', 'COMPLETED', 'Khách nhận máy tại Bitexco', '2026-10-18 14:30:00'),
(25, 11, 15, NULL, 'PENDING_PAYMENT', 'Khách tạo đơn ThinkPad X1', '2026-10-20 15:20:00'),
(26, 11, 1, 'PENDING_PAYMENT', 'CONFIRMED', 'Xác nhận thanh toán VNPAY', '2026-10-20 15:25:00'),
(27, 11, 3, 'CONFIRMED', 'SHIPPING', 'Đang vận chuyển Huế', '2026-10-21 09:00:00'),
(28, 16, 20, NULL, 'PENDING_PAYMENT', 'Khách đặt màn hình LG UltraGear OLED', '2026-11-02 09:15:00'),
(29, 16, 1, 'PENDING_PAYMENT', 'CONFIRMED', 'Thanh toán thành công', '2026-11-02 09:20:00'),
(30, 16, 2, 'CONFIRMED', 'SHIPPING', 'Xe tải GHN vận chuyển chuyên dụng', '2026-11-03 08:30:00');

INSERT INTO inventory_transactions (txn_id, variant_variant_id, order_order_id, performed_by_user_id, transaction_type, form_code, quantity, qty_before, qty_after, reason, created_at) VALUES
(5, 12, 7, 2, 'EXPORT', 'MB04-20261006-0005', 1, 16, 15, 'Xuất kho giao đơn hàng DH-20261005-201', '2026-10-06 09:00:00'),
(6, 21, 8, 3, 'EXPORT', 'MB04-20261009-0006', 1, 36, 35, 'Xuất kho giao đơn hàng DH-20261008-202', '2026-10-09 10:00:00'),
(7, 17, 9, 2, 'EXPORT', 'MB04-20261013-0007', 1, 26, 25, 'Xuất kho giao đơn hàng DH-20261012-203', '2026-10-13 08:30:00'),
(8, 25, 10, 2, 'EXPORT', 'MB04-20261017-0008', 1, 51, 50, 'Xuất kho giao đơn hàng DH-20261016-204', '2026-10-17 09:00:00'),
(9, 14, 11, 3, 'EXPORT', 'MB04-20261021-0009', 1, 21, 20, 'Xuất kho giao đơn hàng DH-20261020-205', '2026-10-21 09:00:00'),
(10, 28, 12, 2, 'EXPORT', 'MB04-20261024-0010', 1, 31, 30, 'Xuất kho giao đơn hàng DH-20261023-206', '2026-10-24 10:00:00'),
(11, 31, 16, 2, 'EXPORT', 'MB04-20261103-0011', 1, 17, 16, 'Xuất kho màn hình OLED DH-20261102-213', '2026-11-03 08:30:00'),
(12, 24, 21, 2, 'EXPORT', 'MB04-20261109-0012', 1, 29, 28, 'Xuất kho đồng hồ Garmin DH-20261108-218', '2026-11-09 08:30:00'),
(13, 12, NULL, 1, 'IMPORT', 'MB01-20260301-0013', 20, 0, 20, 'Nhập kho lô Dell XPS 13 chính hãng', '2026-03-01 08:00:00'),
(14, 14, NULL, 1, 'IMPORT', 'MB01-20260305-0014', 25, 0, 25, 'Nhập kho lô ThinkPad X1 Carbon', '2026-03-05 08:00:00'),
(15, 21, NULL, 1, 'IMPORT', 'MB01-20260401-0015', 40, 0, 40, 'Nhập kho lô Apple Watch Series 9', '2026-04-01 08:00:00'),
(16, 25, NULL, 1, 'IMPORT', 'MB01-20260415-0016', 60, 0, 60, 'Nhập kho loa JBL Charge 5', '2026-04-15 08:00:00'),
(17, 26, NULL, 1, 'IMPORT', 'MB01-20260420-0017', 80, 0, 80, 'Nhập kho chuột Logitech MX Master 3S', '2026-04-20 08:00:00'),
(18, 30, NULL, 1, 'IMPORT', 'MB01-20260510-0018', 100, 0, 100, 'Nhập kho sạc Baseus Blade 100W', '2026-05-10 08:00:00'),
(19, 31, NULL, 1, 'IMPORT', 'MB01-20260515-0019', 20, 0, 20, 'Nhập kho màn hình LG UltraGear OLED', '2026-05-15 08:00:00');

INSERT INTO return_requests (return_id, order_order_id, requested_by_user_id, inspected_by_user_id, reason, status, refund_amount, evidence_images, bank_name, bank_account_number, bank_account_holder, inspection_result, reject_reason, admin_note, inspected_at, approved_at, created_at, updated_at) VALUES
(38, 7, 11, NULL, 'Bàn phím bị kẹt phím Spacebar khi nhấn góc bên phải', 'PENDING', NULL, '["http://localhost:9000/reviews/evidence-scratch.jpg"]', 'Techcombank', '190311223344', 'LE HOANG NAM', NULL, NULL, 'Chờ khách gửi máy về TTBH Cầu Giấy', NULL, NULL, '2026-10-10 09:30:00', '2026-10-10 09:30:00'),
(39, 8, 12, 201, 'Cảm biến nhịp tim mặt sau đồng hồ bị nứt khi mở hộp', 'APPROVED', 9490000.00, '["http://localhost:9000/reviews/evidence-soc-man-hinh-1.jpg"]', 'Vietcombank', '0071004223344', 'NGUYEN MAI ANH', 'Xác nhận lỗi nứt kính cảm biến mặt lưng trước khi giao, đồng ý hoàn tiền', NULL, 'Đã hoàn tiền chuyển khoản Vietcombank', '2026-10-13 14:00:00', '2026-10-13 14:00:00', '2026-10-12 10:15:00', '2026-10-13 14:00:00'),
(40, 9, 13, 201, 'Khách báo máy nóng khi quay video 4K ngoài trời nắng', 'REJECTED', NULL, '["http://localhost:9000/reviews/evidence-power-1.jpg"]', NULL, NULL, NULL, 'Kiểm tra máy hoạt động bình thường, nhiệt độ trong giới hạn tiêu chuẩn tản nhiệt của nhà sản xuất Google', 'Nhiệt độ thiết bị nằm trong thông số cho phép khi quay 4K 60fps', 'Đã tư vấn giải thích kỹ thuật cho khách', '2026-10-17 11:00:00', NULL, '2026-10-16 14:00:00', '2026-10-17 11:00:00'),
(41, 10, 14, NULL, 'Loa JBL bị rè âm bass khi mở mức âm lượng trên 80%', 'PENDING', NULL, '["http://localhost:9000/reviews/review-jbl-1.jpg"]', 'MBBank', '0906445566', 'VO THI BICH TRAM', NULL, NULL, NULL, NULL, NULL, '2026-10-19 16:20:00', '2026-10-19 16:20:00'),
(42, 21, 12, 201, 'Màn hình đồng hồ Garmin bị 1 điểm sọc chết pixel màu trắng', 'APPROVED', 10690000.00, '["http://localhost:9000/reviews/evidence-soc-man-hinh-2.jpg"]', 'Vietcombank', '0071004223344', 'NGUYEN MAI ANH', 'Xác nhận lỗi hiển thị panel OLED từ Garmin, đổi mới 1-1', NULL, 'Đã gửi mã vận đơn đổi mới hàng', '2026-11-12 10:00:00', '2026-11-12 10:00:00', '2026-11-11 15:00:00', '2026-11-12 10:00:00'),
(43, 7, 11, 201, 'Khách đổi ý muốn nâng cấp lên bản RAM 32GB', 'REJECTED', NULL, '["http://localhost:9000/reviews/evidence-scratch.jpg"]', NULL, NULL, NULL, 'Sản phẩm đã active sử dụng quá 7 ngày theo chính sách chỉ áp dụng lỗi phần cứng NSX', 'Từ chối đổi trả theo nhu cầu cá nhân sau khi đã kích hoạt máy', 'Đã hướng dẫn khách bán lại theo chính sách thu cũ đổi mới', '2026-10-18 10:00:00', NULL, '2026-10-17 09:00:00', '2026-10-18 10:00:00'),
(44, 1, 4, NULL, 'Cáp sạc Type-C đi kèm cắm sạc chập chờn', 'PENDING', NULL, '["http://localhost:9000/reviews/evidence-power-1.jpg"]', 'Vietcombank', '0123456789', 'TRAN THU HUYEN', NULL, NULL, 'Hỗ trợ đổi cáp mới tại showroom', NULL, NULL, '2026-10-25 09:00:00', '2026-10-25 09:00:00'),
(45, 2, 5, 201, 'Bút S-Pen bị kẹt nhẹ ở khay cắm máy', 'APPROVED', 500000.00, '["http://localhost:9000/reviews/evidence-scratch.jpg"]', 'Techcombank', '190712345678', 'HOANG MINH KHOI', 'Hỗ trợ thay thế ngòi và vệ sinh khe cắm S-Pen miễn phí', NULL, 'Đã xử lý xong trả máy cho khách', '2026-10-27 14:00:00', '2026-10-27 14:00:00', '2026-10-26 10:00:00', '2026-10-27 14:00:00'),
(46, 3, 6, NULL, 'Loa Marshall bị trầy chân đế cao su dưới đáy', 'REJECTED', NULL, '["http://localhost:9000/reviews/evidence-scratch.jpg"]', NULL, NULL, NULL, 'Trầy xước chân đế do bề mặt đặt loa có sỏi cát sau 15 ngày dùng', 'Không áp dụng bảo hành thẩm mỹ sau khi ký nhận giao hàng', 'Đã liên hệ giải thích cho khách', '2026-11-01 10:00:00', NULL, '2026-10-31 11:00:00', '2026-11-01 10:00:00'),
(47, 4, 7, NULL, 'Đệm tai nghe Sony XM5 bị cấn lõm nhẹ khi vận chuyển', 'PENDING', NULL, '["http://localhost:9000/reviews/evidence-scratch.jpg"]', 'BIDV', '68689999000', 'DO BAO NGOC', NULL, NULL, NULL, NULL, NULL, '2026-11-05 14:30:00', '2026-11-05 14:30:00');

INSERT INTO reviews (review_id, order_item_order_item_id, rating, comment, is_verified_purchase, status, reply, replied_at, created_at, updated_at) VALUES
(4, 11, 5, 'Dell XPS 13 viền màn hình siêu mỏng, touchpad ẩn cực sang, bàn phím gõ rất thích!', b'1', 'VISIBLE', 'Cảm ơn anh Nam đã dành lời khen cho dòng XPS 13 cao cấp của NextGen!', '2026-10-09 10:00:00', '2026-10-09 09:30:00', '2026-10-09 10:00:00'),
(5, 12, 5, 'Apple Watch Series 9 pin dùng thoải mái cả ngày, tính năng Double Tap rất tiện lợi khi đang bận tay.', b'1', 'VISIBLE', 'Chúc bạn Mai Anh có những trải nghiệm thể thao và sức khỏe tuyệt vời!', '2026-10-12 14:00:00', '2026-10-12 13:00:00', '2026-10-12 14:00:00'),
(6, 13, 5, 'Pixel 8 Pro chụp chân dung và chụp đêm quá đỉnh, AI xử lý ảnh siêu thực!', b'1', 'VISIBLE', NULL, NULL, '2026-10-16 11:20:00', '2026-10-16 11:20:00'),
(7, 14, 5, 'Loa JBL Charge 5 âm bass đập tức ngực, mang đi picnic ngoài trời nghe cực đã!', b'1', 'VISIBLE', 'NextGen cảm ơn bạn Trâm, chúc bạn luôn tràn ngập năng lượng âm nhạc!', '2026-10-19 10:30:00', '2026-10-19 09:45:00', '2026-10-19 10:30:00'),
(8, 15, 5, 'ThinkPad X1 Carbon đúng là đẳng cấp bàn phím số 1, máy siêu nhẹ mang đi công tác nhẹ tênh.', b'1', 'VISIBLE', NULL, NULL, '2026-10-23 15:00:00', '2026-10-23 15:00:00'),
(9, 16, 4, 'Chất âm Sony WF-1000XM5 chi tiết, chống ồn rất tốt, tuy nhiên tip bọt biển đeo hơi khít tai chút.', b'1', 'VISIBLE', 'Dạ chị Dung có thể thử size tip silicon kèm theo hộp để đeo vừa vặn êm hơn ạ!', '2026-10-25 11:00:00', '2026-10-25 10:15:00', '2026-10-25 11:00:00'),
(10, 17, 5, 'Galaxy Z Fold 5 gập mở mượt mà, chia đôi màn hình làm việc xử lý excel rất tiện lợi.', b'1', 'VISIBLE', NULL, NULL, '2026-10-28 16:30:00', '2026-10-28 16:30:00'),
(11, 18, 5, 'Apple Watch Ultra 2 thiết kế hầm hố, GPS cực kỳ chuẩn khi chạy trail leo núi.', b'1', 'VISIBLE', 'Cảm ơn anh Tiên đã tin dùng phiên bản cao cấp nhất của Apple Watch!', '2026-10-31 09:00:00', '2026-10-31 08:30:00', '2026-10-31 09:00:00'),
(12, 19, 5, 'Sennheiser Momentum 4 pin 60 giờ nghe cả tuần không cần sạc, âm trường rộng thênh thang.', b'1', 'VISIBLE', NULL, NULL, '2026-11-02 14:00:00', '2026-11-02 14:00:00'),
(13, 20, 5, 'Màn hình OLED 240Hz màu đen sâu thăm thẳm, bắn súng FPS không có một chút bóng mờ nào!', b'1', 'VISIBLE', 'Cực phẩm màn hình gaming OLED của LG, chúc bạn Minh leo rank thần tốc!', '2026-11-05 10:00:00', '2026-11-05 09:15:00', '2026-11-05 10:00:00'),
(14, 21, 5, 'Chuột MX Master 3S click êm ru không tiếng động, làm việc văn phòng ban đêm không sợ ồn ai.', b'1', 'VISIBLE', NULL, NULL, '2026-11-06 15:20:00', '2026-11-06 15:20:00'),
(15, 22, 5, 'Bàn phím MX Mechanical gõ nảy và êm, cảm biến tự sáng đèn khi đưa tay lại gần rất thông minh.', b'1', 'VISIBLE', 'NextGen cảm ơn anh Độ, combo MX Master + MX Mechanical là chuẩn công thái học luôn ạ!', '2026-11-07 14:00:00', '2026-11-07 13:30:00', '2026-11-07 14:00:00'),
(16, 23, 5, 'Cục sạc Baseus Blade 100W mỏng dính nhét balo cực gọn, sạc đầy pin laptop nhanh chóng.', b'1', 'VISIBLE', NULL, NULL, '2026-11-08 17:00:00', '2026-11-08 17:00:00'),
(17, 24, 5, 'Mua thêm chuột MX Master 3S dùng cho máy tính bàn ở công ty, form ôm tay đỡ mỏi cổ tay hẳn.', b'1', 'VISIBLE', NULL, NULL, '2026-11-09 11:30:00', '2026-11-09 11:30:00'),
(18, 25, 5, 'Garmin 265 đo nhịp tim và tải luyện tập rất chuẩn, màn hình AMOLED nhìn rõ dưới trời nắng gắt.', b'1', 'VISIBLE', 'Chúc bạn Mai Anh hoàn thành tốt các cự ly chạy bộ tiếp theo nhé!', '2026-11-11 10:00:00', '2026-11-11 09:10:00', '2026-11-11 10:00:00');

INSERT INTO review_images (image_id, review_id, image_url, bucket, object_key, content_type, file_size, display_order, created_at) VALUES
(4, 4, 'http://localhost:9000/products/dell-xps-13.png', 'products', 'dell-xps-13.png', 'image/png', 523100, 1, '2026-10-09 09:35:00'),
(5, 5, 'http://localhost:9000/reviews/review-watch-1.jpg', 'reviews', 'review-watch-1.jpg', 'image/jpeg', 234100, 1, '2026-10-12 13:05:00'),
(6, 6, 'http://localhost:9000/reviews/review-pixel-1.jpg', 'reviews', 'review-pixel-1.jpg', 'image/jpeg', 198200, 1, '2026-10-16 11:25:00'),
(7, 7, 'http://localhost:9000/reviews/review-jbl-1.jpg', 'reviews', 'review-jbl-1.jpg', 'image/jpeg', 421000, 1, '2026-10-19 09:50:00'),
(8, 8, 'http://localhost:9000/products/thinkpad-x1.png', 'products', 'thinkpad-x1.png', 'image/png', 345600, 1, '2026-10-23 15:05:00'),
(9, 10, 'http://localhost:9000/reviews/review-fold-1.jpg', 'reviews', 'review-fold-1.jpg', 'image/jpeg', 678900, 1, '2026-10-28 16:35:00'),
(10, 11, 'http://localhost:9000/products/apple-watch-ultra-2.png', 'products', 'apple-watch-ultra-2.png', 'image/png', 450100, 1, '2026-10-31 08:35:00'),
(11, 13, 'http://localhost:9000/products/lg-ultragear-monitor.png', 'products', 'lg-ultragear-monitor.png', 'image/png', 589200, 1, '2026-11-05 09:20:00'),
(12, 14, 'http://localhost:9000/reviews/review-mouse-1.jpg', 'reviews', 'review-mouse-1.jpg', 'image/jpeg', 312000, 1, '2026-11-06 15:25:00'),
(13, 18, 'http://localhost:9000/products/garmin-forerunner-265.png', 'products', 'garmin-forerunner-265.png', 'image/png', 412300, 1, '2026-11-11 09:15:00');

INSERT INTO support_tickets (ticket_id, user_user_id, handled_by_user_id, subject, description, priority, status, resolution_note, resolved_at, created_at, updated_at) VALUES
(4, 11, 2, 'Tư vấn nâng cấp SSD cho Dell XPS 13', 'Máy mình có gắn thêm được ổ SSD M.2 thứ hai không shop?', 'NORMAL', 'RESOLVED', 'Đã tư vấn dòng XPS 13 9340 có 1 khe M.2 PCIe Gen4 hỗ trợ nâng cấp thay thế tối đa 2TB', '2026-10-09 11:30:00', '2026-10-09 10:15:00', '2026-10-09 11:30:00'),
(5, 12, 3, 'Hướng dẫn kết nối eSIM cho Apple Watch', 'Làm sao để kích hoạt gói cước MultiSIM Viettel trên Apple Watch?', 'NORMAL', 'RESOLVED', 'Đã gửi tài liệu hướng dẫn kích hoạt MultiSIM trên ứng dụng Watch', '2026-10-12 16:00:00', '2026-10-12 15:00:00', '2026-10-12 16:00:00'),
(6, 13, 2, 'Kiểm tra sạc nhanh Pixel 8 Pro', 'Mình dùng củ sạc 65W nhưng máy chỉ nhận tối đa 30W có phải lỗi không?', 'LOW', 'RESOLVED', 'Dạ chuẩn sạc của Pixel 8 Pro theo NSX giới hạn ở mức 30W PD 3.0 PPS nên củ 65W tự động hạ dòng an toàn', '2026-10-16 14:00:00', '2026-10-16 13:30:00', '2026-10-16 14:00:00'),
(7, 14, 3, 'Hỏi về tính năng kết nối PartyBoost loa JBL', 'Làm sao để kết nối 2 loa JBL Charge 5 phát âm thanh stereo?', 'NORMAL', 'RESOLVED', 'Nhấn nút PartyBoost trên cả 2 thân loa hoặc ghép đôi qua app JBL Portable', '2026-10-19 11:45:00', '2026-10-19 11:00:00', '2026-10-19 11:45:00'),
(8, 15, 2, 'Cài đặt vân tay một chạm trên ThinkPad X1', 'Cảm biến vân tay trên nút nguồn thỉnh thoảng không nhận', 'HIGH', 'RESOLVED', 'Hướng dẫn khách cập nhật driver Synaptics Fingerprint trên phần mềm Lenovo Vantage', '2026-10-24 16:00:00', '2026-10-24 14:20:00', '2026-10-24 16:00:00'),
(9, 16, 3, 'Kết nối đồng thời 2 thiết bị tai nghe Sony', 'Làm sao để tai nghe kết nối vừa laptop vừa điện thoại?', 'NORMAL', 'RESOLVED', 'Bật tính năng Connect to 2 devices simultaneously trong ứng dụng Sony Headphones Connect', '2026-10-26 10:30:00', '2026-10-26 09:15:00', '2026-10-26 10:30:00'),
(10, 17, 2, 'Dán màn hình bảo vệ Z Fold 5', 'Shop có dán miếng dán chống xước màn hình trong không?', 'LOW', 'OPEN', NULL, NULL, '2026-10-29 14:00:00', '2026-10-29 14:00:00'),
(11, 18, 3, 'Hỏi chế độ pin GPS Garmin', 'Cần cài đặt thế nào để pin đồng hồ hoạt động liên tục 20 giờ chạy trail?', 'NORMAL', 'OPEN', NULL, NULL, '2026-11-01 10:30:00', '2026-11-01 10:30:00'),
(12, 19, 2, 'Tương thích tai nghe Sennheiser trên iPhone', 'App Sennheiser Smart Control có hỗ trợ chỉnh EQ trên iOS không?', 'NORMAL', 'RESOLVED', 'Đã hướng dẫn tải app trên App Store và cấu hình Sound Personalization', '2026-11-03 15:00:00', '2026-11-03 14:10:00', '2026-11-03 15:00:00'),
(13, 20, 3, 'Cài đặt màu HDR cho màn hình LG OLED', 'Windows 11 bật HDR màn hình bị nhạt màu', 'HIGH', 'OPEN', NULL, NULL, '2026-11-06 09:30:00', '2026-11-06 09:30:00'),
(14, 21, 2, 'Gán nút macro chuột MX Master 3S', 'Hướng dẫn gán nút chuyển tab trên trình duyệt', 'LOW', 'RESOLVED', 'Hướng dẫn cài app Logi Options+ và tạo Smart Actions', '2026-11-08 11:20:00', '2026-11-08 10:45:00', '2026-11-08 11:20:00'),
(15, 22, 3, 'Đổi switch phím cơ Logitech', 'Switch của phím MX Mechanical có nhổ hot-swap được không?', 'LOW', 'RESOLVED', 'Dạ dòng MX Mechanical switch hàn trực tiếp trên bo mạch nên không hỗ trợ hot-swap', '2026-11-10 14:15:00', '2026-11-10 13:50:00', '2026-11-10 14:15:00');

INSERT INTO wishlists (wishlist_id, user_user_id, product_product_id, created_at) VALUES
(6, 11, 13, '2026-10-02 08:30:00'),
(7, 11, 17, '2026-10-02 08:31:00'),
(8, 12, 16, '2026-10-03 09:15:00'),
(9, 12, 22, '2026-10-03 09:16:00'),
(10, 13, 14, '2026-10-04 11:00:00'),
(11, 14, 19, '2026-10-06 14:20:00'),
(12, 15, 12, '2026-10-08 10:40:00'),
(13, 16, 23, '2026-10-10 15:10:00'),
(14, 17, 15, '2026-10-12 16:30:00'),
(15, 18, 18, '2026-10-15 08:45:00'),
(16, 19, 23, '2026-10-18 13:20:00'),
(17, 20, 25, '2026-10-20 17:00:00'),
(18, 21, 20, '2026-10-22 10:10:00'),
(19, 22, 21, '2026-10-25 14:40:00'),
(20, 25, 24, '2026-10-28 09:50:00');

INSERT INTO faqs (faq_id, question, answer, category, display_order, is_active, created_at, updated_at) VALUES
(5, 'NextGen có hỗ trợ thu cũ đổi mới (Trade-in) không?', 'Có, NextGen trợ giá lên đời thu cũ đổi mới lên đến 2.000.000đ cho tất cả các dòng smartphone, tablet, laptop và smartwatch chính hãng.', 'Chính sách mua hàng', 5, b'1', '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
(6, 'Thời gian giao hàng toàn quốc mất bao lâu?', 'Đơn hàng nội thành Hà Nội và TP.HCM giao hỏa tốc 2 giờ hoặc trong ngày. Các tỉnh thành khác thời gian giao từ 1 đến 3 ngày làm việc qua đối tác GHN, GHTK, Viettel Post.', 'Giao hàng & Vận chuyển', 6, b'1', '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
(7, 'Làm thế nào để áp dụng mã giảm giá voucher khi thanh toán?', 'Tại bước giỏ hàng hoặc xác nhận đơn hàng, bạn nhập mã voucher vào ô "Mã giảm giá" và bấm "Áp dụng". Hệ thống sẽ tự động trừ số tiền giảm giá trực tiếp vào tổng đơn.', 'Khuyến mãi & Voucher', 7, b'1', '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
(8, 'Sản phẩm tại NextGen có được bảo hành chính hãng không?', '100% sản phẩm phân phối tại NextGen đều là hàng chính hãng mới nguyên seal, được bảo hành điện tử chính hãng từ 12 đến 24 tháng tại các TTBH của Apple, Samsung, Dell, Asus trên toàn quốc.', 'Bảo hành & Hỗ trợ kỹ thuật', 8, b'1', '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
(9, 'Quy trình đổi trả sản phẩm lỗi diễn ra như thế nào?', 'Khách hàng có thể tạo yêu cầu đổi trả online tại mục "Đơn hàng của tôi" trong vòng 30 ngày kể từ khi nhận hàng. Kỹ thuật viên NextGen sẽ kiểm tra và hỗ trợ đổi 1-1 hoặc hoàn tiền nhanh chóng.', 'Chính sách đổi trả', 9, b'1', '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
(10, 'NextGen hỗ trợ những hình thức thanh toán nào?', 'NextGen hỗ trợ thanh toán khi nhận hàng (COD), chuyển khoản ngân hàng qua VietQR tự động, thanh toán thẻ ATM/Visa/MasterCard qua cổng VNPAY an toàn bảo mật.', 'Thanh toán & Hóa đơn', 10, b'1', '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
(11, 'NextGen có xuất hóa đơn giá trị gia tăng (VAT) công ty không?', 'Có, khách hàng vui lòng tích chọn "Xuất hóa đơn VAT doanh nghiệp" và điền thông tin Mã số thuế, Tên công ty khi tiến hành đặt hàng.', 'Thanh toán & Hóa đơn', 11, b'1', '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
(12, 'Khách hàng có được mở hộp kiểm tra hàng trước khi thanh toán COD không?', 'Có, NextGen áp dụng chính sách đồng kiểm: quý khách được kiểm tra ngoại quan kiện hàng, tem niêm phong và sản phẩm nguyên vẹn trước khi thanh toán cho nhân viên giao hàng.', 'Giao hàng & Vận chuyển', 12, b'1', '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
(13, 'Chính sách bảo hành điểm chết màn hình được quy định ra sao?', 'Áp dụng đổi mới trong 30 ngày nếu màn hình xuất hiện từ 3 điểm chết (dead pixel) hoặc có sọc panel màn hình theo tiêu chuẩn của hãng sản xuất.', 'Bảo hành & Hỗ trợ kỹ thuật', 13, b'1', '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
(14, 'Thời gian giải quyết yêu cầu hoàn tiền đổi trả mất bao lâu?', 'Sau khi kỹ thuật viên xác nhận thiết bị đủ điều kiện đổi trả, tiền sẽ được hoàn về tài khoản ngân hàng của quý khách trong vòng 24 đến 48 giờ làm việc.', 'Chính sách đổi trả', 14, b'1', '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
(15, 'NextGen có chương trình trả góp 0% lãi suất không?', 'Có, NextGen liên kết với hơn 20 ngân hàng lớn hỗ trợ trả góp 0% lãi suất qua thẻ tín dụng kỳ hạn linh hoạt 3, 6, 9, 12 tháng với thủ tục duyệt online nhanh chóng.', 'Chính sách mua hàng', 15, b'1', '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
(16, 'Làm thế nào để liên hệ trực tiếp với bộ phận chăm sóc khách hàng?', 'Quý khách có thể gửi yêu cầu hỗ trợ qua mục Hỗ trợ kỹ thuật trên website hoặc gọi trực tiếp hotline miễn phí 1800 6868 từ 8h00 đến 21h30 hàng ngày.', 'Liên hệ & Hỗ trợ', 16, b'1', '2026-01-01 00:00:00', '2026-01-01 00:00:00');

INSERT INTO notifications (notification_id, user_user_id, order_order_id, title, content, is_read, created_at) VALUES
(5, 11, 7, 'Đơn hàng giao thành công', 'Đơn hàng DH-20261005-201 mua Dell XPS 13 đã được giao thành công. Cảm ơn bạn!', b'1', '2026-10-08 16:05:00'),
(6, 12, 8, 'Đơn hàng hoàn tất', 'Đơn hàng DH-20261008-202 mua Apple Watch Series 9 đã hoàn tất.', b'1', '2026-10-11 11:35:00'),
(7, 13, 9, 'Đơn hàng giao thành công', 'Kiện hàng Google Pixel 8 Pro DH-20261012-203 đã được ký nhận.', b'1', '2026-10-15 15:05:00'),
(8, 14, 10, 'Giao hàng thành công', 'Loa JBL Charge 5 đơn DH-20261016-204 đã bàn giao tới bạn.', b'1', '2026-10-18 14:35:00'),
(9, 15, 11, 'Đơn hàng đang trên đường giao', 'Đơn hàng ThinkPad X1 DH-20261020-205 đã xuất kho và đang giao qua Viettel Post.', b'0', '2026-10-21 09:10:00'),
(10, 16, 12, 'Đơn hàng đang vận chuyển', 'Tai nghe Sony WF-1000XM5 đơn DH-20261023-206 đang giao tới Vũng Tàu.', b'0', '2026-10-24 10:15:00'),
(11, 17, 13, 'Đơn hàng đã được xác nhận', 'Đơn hàng Galaxy Z Fold 5 DH-20261026-207 đã xác nhận và chuẩn bị đóng gói.', b'1', '2026-10-26 16:35:00'),
(12, 18, 14, 'Đơn hàng đang xử lý', 'Đơn hàng Apple Watch Ultra 2 DH-20261028-208 đang được nhân viên chuẩn bị.', b'0', '2026-10-28 10:35:00'),
(13, 19, 15, 'Thanh toán thành công', 'Bạn đã thanh toán thành công đơn hàng Sennheiser Momentum 4 DH-20261030-209.', b'1', '2026-10-30 14:05:00'),
(14, 20, 16, 'Đơn hàng đang vận chuyển', 'Màn hình LG UltraGear OLED đơn DH-20261102-213 đang trên xe tải chuyên dụng.', b'0', '2026-11-03 08:40:00'),
(15, 21, 17, 'Chờ thanh toán đơn hàng', 'Đơn hàng chuột Logitech DH-20261104-214 đang chờ bạn xác nhận.', b'0', '2026-11-04 14:25:00'),
(16, 22, 18, 'Đơn hàng đã xác nhận', 'Bàn phím cơ MX Mechanical DH-20261105-215 đã được xác nhận.', b'1', '2026-11-05 11:20:00'),
(17, 25, 19, 'Đơn hàng đang chuẩn bị', 'Pin sạc Baseus Blade 100W DH-20261106-216 đang được đóng hộp.', b'0', '2026-11-06 17:05:00'),
(18, 11, 20, 'Đơn hàng đã xác nhận', 'Chuột Logitech MX Master 3S DH-20261107-217 đã được thanh toán thành công.', b'1', '2026-11-07 10:20:00'),
(19, 12, 21, 'Đơn hàng giao thành công', 'Đồng hồ thể thao Garmin 265 DH-20261108-218 đã giao thành công.', b'1', '2026-11-10 15:35:00'),
(20, 12, NULL, 'Yêu cầu đổi trả đã được phê duyệt', 'Yêu cầu đổi trả #39 cho đơn hàng của bạn đã được phê duyệt hoàn tiền.', b'1', '2026-10-13 14:05:00');

INSERT INTO audit_logs (log_id, user_user_id, action, entity_name, entity_id, old_value, new_value, created_at) VALUES
(8, 1, 'CREATE_PRODUCT', 'Product', 11, NULL, 'Dell XPS 13 9340 (2024)', '2026-03-01 10:00:00'),
(9, 1, 'CREATE_PRODUCT', 'Product', 12, NULL, 'Lenovo ThinkPad X1 Carbon Gen 12', '2026-03-05 11:00:00'),
(10, 1, 'CREATE_PRODUCT', 'Product', 13, NULL, 'Asus ROG Zephyrus G14 (2024)', '2026-03-10 14:00:00'),
(11, 1, 'CREATE_PRODUCT', 'Product', 14, NULL, 'Google Pixel 8 Pro', '2026-03-15 09:00:00'),
(12, 1, 'CREATE_PRODUCT', 'Product', 15, NULL, 'Samsung Galaxy Z Fold 5 5G', '2026-03-20 15:30:00'),
(13, 2, 'IMPORT_INVENTORY', 'Inventory', 13, '0', '15', '2026-03-01 08:05:00'),
(14, 2, 'IMPORT_INVENTORY', 'Inventory', 15, '0', '20', '2026-03-05 08:05:00'),
(15, 1, 'CREATE_PROMOTION', 'Promotion', 2, NULL, 'Đại Tiệc Âm Thanh & Phụ Kiện', '2026-10-01 00:00:00'),
(16, 1, 'CREATE_PROMOTION', 'Promotion', 3, NULL, 'Tuần Lễ Laptop & Smartwatch', '2026-10-15 00:00:00'),
(17, 1, 'CREATE_VOUCHER', 'Voucher', 4, NULL, 'SMARTWATCH10', '2026-10-01 00:05:00'),
(18, 11, 'PLACE_ORDER', 'Order', 7, 'PENDING_PAYMENT', 'COMPLETED', '2026-10-05 10:00:00'),
(19, 12, 'PLACE_ORDER', 'Order', 8, 'CONFIRMED', 'COMPLETED', '2026-10-08 14:15:00'),
(20, 201, 'APPROVE_RETURN', 'ReturnRequest', 39, 'PENDING', 'APPROVED', '2026-10-13 14:00:00'),
(21, 201, 'REJECT_RETURN', 'ReturnRequest', 40, 'PENDING', 'REJECTED', '2026-10-17 11:00:00'),
(22, 201, 'APPROVE_RETURN', 'ReturnRequest', 42, 'PENDING', 'APPROVED', '2026-11-12 10:00:00');


SET FOREIGN_KEY_CHECKS = 1;
-- ====================================================================
-- End of Seed Data (27 Tables Initialized)
-- ====================================================================
