package com.wedbackendservice.nextgen.exception;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
@AllArgsConstructor
public enum     ErrorCode {
    UNCATEGORIZED_EXCEPTION(9999, "Lỗi hệ thống không xác định", HttpStatus.INTERNAL_SERVER_ERROR),
    UNCATEGORIZE_EXCEPTION(9999, "Lỗi hệ thống không xác định", HttpStatus.INTERNAL_SERVER_ERROR), // alias for backwards compatibility
    UNAUTHENTICATED(1001, "Tài khoản hoặc mật khẩu không chính xác.", HttpStatus.UNAUTHORIZED),
    USER_LOCKED(1002, "Tài khoản đang bị khóa.", HttpStatus.FORBIDDEN),
    UNAUTHORIZED(1003, "Bạn không có quyền thực hiện hành động này.", HttpStatus.FORBIDDEN),
    USER_NOT_EXIST(1004, "Người dùng không tồn tại.", HttpStatus.NOT_FOUND),
    USER_NOT_EXIT(1004, "Người dùng không tồn tại.", HttpStatus.NOT_FOUND), // alias for backwards compatibility
    TOO_MANY_REQUESTS(1005, "Đăng nhập sai quá 5 lần liên tiếp. Vui lòng thử lại sau.", HttpStatus.TOO_MANY_REQUESTS),
    OTP_INVALID_OR_EXPIRED(1006, "Mã OTP không chính xác hoặc đã hết hạn (quá 2 phút).", HttpStatus.BAD_REQUEST),
    SESSION_NOT_FOUND(1007, "Phiên xác thực không tồn tại hoặc đã được xử lý trước đó!", HttpStatus.NOT_FOUND),
    INVALID_REQUEST(1008, "Dữ liệu yêu cầu không hợp lệ.", HttpStatus.BAD_REQUEST),
    SMS_SEND_FAILED(1010, "Sent Otp Lỗi", HttpStatus.INTERNAL_SERVER_ERROR),
    USER_ALREADY_EXISTS(1011, "Số điện thoại hoặc Email này đã được đăng ký tài khoản trên hệ thống.", HttpStatus.CONFLICT),
    PASSWORD_NOT_MATCH(1012, "Mật khẩu xác nhận không khớp.", HttpStatus.BAD_REQUEST),
    OTP_COOLDOWN_ACTIVE(1013, "Vui lòng chờ hết 60 giây đếm ngược trước khi yêu cầu gửi lại mã.", HttpStatus.BAD_REQUEST),
    SESSION_EXPIRED(1014, "Phiên làm việc đã hết hạn. Vui lòng đăng nhập lại.", HttpStatus.UNAUTHORIZED),
    CURRENT_PASSWORD_INCORRECT(1015, "Mật khẩu hiện tại không chính xác.", HttpStatus.BAD_REQUEST),
    PASSWORD_CONFIRM_NOT_MATCH(1016, "Mật khẩu xác nhận không khớp. Vui lòng kiểm tra lại.", HttpStatus.BAD_REQUEST),
    NEW_PASSWORD_SAME_AS_CURRENT(1017, "Mật khẩu mới không được trùng với mật khẩu hiện tại.", HttpStatus.BAD_REQUEST),
    ADDRESS_NOT_FOUND(1018, "Địa chỉ không tồn tại hoặc không thuộc quyền sở hữu.", HttpStatus.NOT_FOUND),
    LOCK_REASON_REQUIRED(1019, "Lý do khóa tài khoản không được để trống.", HttpStatus.BAD_REQUEST),
    ROLE_INVALID(1020, "Vai trò người dùng không hợp lệ.", HttpStatus.BAD_REQUEST),
    CATEGORY_NOT_FOUND(1021, "Danh mục không tồn tại.", HttpStatus.NOT_FOUND),
    CATEGORY_HAS_CHILDREN(1022, "Không thể xóa danh mục đang có danh mục con.", HttpStatus.BAD_REQUEST),
    CATEGORY_HAS_CHILDREN_OR_PRODUCTS(1033, "Không thể xóa danh mục đang chứa danh mục con hoặc sản phẩm.", HttpStatus.BAD_REQUEST),
    CATEGORY_HAS_PRODUCTS(1030, "Không thể xóa danh mục đang có sản phẩm. Vui lòng chuyển sản phẩm sang danh mục khác trước.", HttpStatus.BAD_REQUEST),
    CATEGORY_ALREADY_EXISTS(1031, "Tên danh mục đã tồn tại trong cùng một nhánh cha.", HttpStatus.CONFLICT),
    BRAND_NOT_FOUND(1023, "Thương hiệu không tồn tại.", HttpStatus.NOT_FOUND),
    BRAND_HAS_PRODUCTS(1024, "Không thể xóa thương hiệu này vì đang có sản phẩm thuộc hãng trên hệ thống. Hãy đổi hãng cho các sản phẩm trước hoặc ẩn thương hiệu thay vì xóa.", HttpStatus.BAD_REQUEST),
    BRAND_ALREADY_EXISTS(1032, "Tên thương hiệu đã tồn tại trên hệ thống.", HttpStatus.CONFLICT),
    VARIANT_NOT_FOUND(1025, "Biến thể sản phẩm không tồn tại.", HttpStatus.NOT_FOUND),
    INSUFFICIENT_STOCK(1026, "Số lượng tồn kho không đủ.", HttpStatus.BAD_REQUEST),
    CART_ITEM_NOT_FOUND(1027, "Sản phẩm không tồn tại trong giỏ hàng.", HttpStatus.NOT_FOUND),
    SHIPMENT_NOT_FOUND(1028, "Vận đơn không tồn tại.", HttpStatus.NOT_FOUND),
    ORDER_NOT_FOUND(1029, "Đơn hàng không tồn tại.", HttpStatus.NOT_FOUND),
    STATUS_INVALID(1034, "Trạng thái người dùng không hợp lệ.", HttpStatus.BAD_REQUEST),
    INVALID_PAGE_NUMBER(1035, "Số trang phải lớn hơn hoặc bằng 1.", HttpStatus.BAD_REQUEST),
    INVALID_PAGE_SIZE(1036, "Kích thước trang không hợp lệ.", HttpStatus.BAD_REQUEST),
    ADDRESS_LIMIT_EXCEEDED(1035, "Số lượng địa chỉ đã đạt giới hạn tối đa (tối đa 10 địa chỉ).", HttpStatus.BAD_REQUEST),
    CANNOT_UNSET_DEFAULT_ADDRESS(1036, "Không thể hủy mặc định. Vui lòng chọn một địa chỉ khác làm mặc định thay thế.", HttpStatus.BAD_REQUEST),
    CANNOT_DEMOTE_LAST_ADMIN(1037, "Không thể hạ quyền Admin duy nhất còn hoạt động trong hệ thống.", HttpStatus.BAD_REQUEST),
    CANNOT_LOCK_LAST_ADMIN(1038, "Không thể khóa Admin duy nhất còn hoạt động trong hệ thống.", HttpStatus.BAD_REQUEST),
    CATEGORY_CYCLE_DETECTED(1040, "Không thể chọn chính nó hoặc danh mục con cháu làm danh mục cha (phát hiện vòng lặp cây).", HttpStatus.BAD_REQUEST),
    CATEGORY_SLUG_ALREADY_EXISTS(1041, "Đường dẫn slug của danh mục đã tồn tại.", HttpStatus.CONFLICT),
    BRAND_SLUG_ALREADY_EXISTS(1042, "Đường dẫn slug của thương hiệu đã tồn tại.", HttpStatus.CONFLICT),
    INVALID_SHIPMENT_STATUS_TRANSITION(1043, "Chuyển đổi trạng thái vận đơn không hợp lệ theo quy trình giao hàng.", HttpStatus.BAD_REQUEST),
    OTP_REQUIRED(1044, "Cần mã OTP để xác thực thay đổi số điện thoại hoặc email.", HttpStatus.BAD_REQUEST),
    PRODUCT_INACTIVE(1045, "Sản phẩm hiện đang tạm ngừng kinh doanh hoặc đã ẩn.", HttpStatus.BAD_REQUEST),
    DUPLICATE_EMAIL(1046, "Email đã được sử dụng bởi một tài khoản khác!", HttpStatus.CONFLICT),
    DUPLICATE_PHONE(1047, "Số điện thoại đã được sử dụng bởi một tài khoản khác!", HttpStatus.CONFLICT),
    EMAIL_ALREADY_EXISTS(1046, "Địa chỉ Email đã được sử dụng bởi tài khoản khác!", HttpStatus.CONFLICT),
    PHONE_ALREADY_EXISTS(1047, "Số điện thoại đã được đăng ký bởi tài khoản khác!", HttpStatus.CONFLICT),
    CANNOT_DELETE_DEFAULT_ADDRESS(1048, "Không thể xóa địa chỉ mặc định khi tài khoản vẫn còn các địa chỉ khác. Vui lòng đặt địa chỉ khác làm mặc định trước khi xóa.", HttpStatus.BAD_REQUEST),
    OTP_ATTEMPTS_EXCEEDED(1050, "Bạn đã nhập sai mã OTP quá 5 lần. Phiên xác thực đã bị hủy để đảm bảo an toàn.", HttpStatus.BAD_REQUEST),
    OTP_SEND_LIMIT_EXCEEDED(1049, "Bạn đã yêu cầu gửi mã OTP quá số lần cho phép. Vui lòng thử lại sau.", HttpStatus.TOO_MANY_REQUESTS),

    // Nhóm 4: Sản phẩm & Biến thể SKU
    PRODUCT_NOT_FOUND(1039, "Không tìm thấy sản phẩm!", HttpStatus.NOT_FOUND),
    DUPLICATE_SKU(1054, "Mã SKU đã tồn tại trong hệ thống!", HttpStatus.CONFLICT),
    INVALID_PRODUCT_STATUS(1055, "Trạng thái sản phẩm không hợp lệ.", HttpStatus.BAD_REQUEST),

    // Nhóm 5: Kho vận & Tồn kho biến thể
    ERR_STOCK_ADJUST_INVALID(1050, "Số lượng kiểm kê thực tế không được nhỏ hơn số lượng đang giữ chỗ.", HttpStatus.BAD_REQUEST),
    ERR_INVALID_DATE_FORMAT(1051, "Định dạng ngày lọc không hợp lệ.", HttpStatus.BAD_REQUEST),
    ERR_VARIANT_NOT_FOUND(1052, "Không tìm thấy dữ liệu tồn kho cho biến thể.", HttpStatus.NOT_FOUND),
    ERR_FORBIDDEN_ACCESS(1053, "Bạn không có quyền truy cập vào danh mục kho vận này!", HttpStatus.FORBIDDEN),

    // Nhóm 6: Khuyến mãi & Voucher
    ERR_PROMO_PRICE_INVALID(1060, "Giá khuyến mãi phải nhỏ hơn giá niêm yết của biến thể", HttpStatus.BAD_REQUEST),
    ERR_PROMOTION_NOT_FOUND(1061, "Không tìm thấy chiến dịch khuyến mãi", HttpStatus.NOT_FOUND),
    ERR_VOUCHER_NOT_FOUND(1062, "Không tìm thấy voucher", HttpStatus.NOT_FOUND),
    ERR_VOUCHER_DUPLICATE_CODE(1063, "Mã voucher đã tồn tại trên hệ thống!", HttpStatus.CONFLICT),
    ERR_VOUCHER_EXPIRED(1064, "Mã giảm giá đã hết hạn sử dụng", HttpStatus.BAD_REQUEST),
    ERR_VOUCHER_LIMIT_REACHED(1065, "Mã giảm giá đã hết lượt sử dụng trong toàn hệ thống", HttpStatus.BAD_REQUEST),
    ERR_VOUCHER_MIN_ORDER(1066, "Giá trị đơn hàng chưa đạt mức tối thiểu để áp dụng mã", HttpStatus.BAD_REQUEST),
    ERR_FORBIDDEN(1067, "Tài khoản của bạn không có quyền quản trị phân hệ khuyến mãi!", HttpStatus.FORBIDDEN),

    // Nhóm 8: Đơn hàng & Tiến trình xử lý
    ERR_STOCK_INSUFFICIENT(1070, "Số lượng tồn kho khả dụng không đủ để đáp ứng đơn hàng.", HttpStatus.CONFLICT),
    ERR_ORDER_CANNOT_CANCEL(1071, "Không thể hủy đơn hàng do đơn đã được xác nhận hoặc đang trong quá trình giao hàng!", HttpStatus.BAD_REQUEST),
    ERR_ORDER_INVALID_STATE(1072, "Thao tác không hợp lệ đối với trạng thái hiện tại của đơn hàng!", HttpStatus.BAD_REQUEST),
    ERR_ORDER_NOT_CONFIRMED(1073, "Đơn hàng phải được duyệt xác nhận (CONFIRMED) trước khi thực hiện xuất kho!", HttpStatus.BAD_REQUEST),
    ERR_ORDER_NOT_FOUND(1074, "Không tìm thấy đơn hàng!", HttpStatus.NOT_FOUND),
    INVALID_OTP_CODE(1083, "Mã xác thực OTP không chính xác. Vui lòng kiểm tra lại hộp thư email!", HttpStatus.BAD_REQUEST),
    OTP_EXPIRED(1084, "Mã xác thực OTP đã hết hạn hiệu lực. Vui lòng nhấn gửi lại mã mới!", HttpStatus.BAD_REQUEST),
    RESEND_COOLDOWN_ACTIVE(1086, "Vui lòng chờ trước khi bấm gửi lại mã!", HttpStatus.TOO_MANY_REQUESTS),
    INVALID_PHONE_FORMAT(1087, "Số điện thoại không đúng định dạng 10 chữ số!", HttpStatus.BAD_REQUEST),

    // Nhóm 12: Đánh giá, hỗ trợ, FAQ
    ERR_REVIEW_NOT_FOUND(1090, "Không tìm thấy đánh giá!", HttpStatus.NOT_FOUND),
    ERR_REVIEW_ALREADY_EXISTS(1091, "Bạn đã đánh giá sản phẩm này cho đơn hàng hiện tại.", HttpStatus.CONFLICT),
    ERR_REVIEW_NOT_ALLOWED(1092, "Chỉ có thể đánh giá sản phẩm sau khi đơn hàng đã hoàn thành.", HttpStatus.BAD_REQUEST),
    ERR_TICKET_NOT_FOUND(1093, "Không tìm thấy phiếu hỗ trợ!", HttpStatus.NOT_FOUND),
    ERR_TICKET_INVALID_STATUS(1094, "Trạng thái phiếu hỗ trợ không hợp lệ.", HttpStatus.BAD_REQUEST),
    ERR_FAQ_NOT_FOUND(1095, "Không tìm thấy câu hỏi thường gặp!", HttpStatus.NOT_FOUND),

    // Nhóm 11: Đổi trả & Biên bản hoàn kho
    RETURN_PERIOD_EXPIRED(1101, "Đơn hàng đã hoàn thành quá 30 ngày (Chính sách đổi trả áp dụng trong vòng 30 ngày kể từ ngày nhận hàng)!", HttpStatus.BAD_REQUEST),
    DUPLICATE_RETURN_REQUEST(1102, "Đơn hàng này đã có yêu cầu đổi trả đang trong quá trình xử lý!", HttpStatus.CONFLICT),
    RETURN_ALREADY_PROCESSED(1103, "Yêu cầu đổi trả này đã được thẩm định trước đó, không thể thay đổi biên bản!", HttpStatus.BAD_REQUEST),
    RETURN_NOT_FOUND(1104, "Không tìm thấy yêu cầu đổi trả!", HttpStatus.NOT_FOUND),
    RETURN_ORDER_NOT_COMPLETED(1105, "Đơn hàng chưa hoàn thành, chỉ được gửi yêu cầu đổi trả khi đơn hàng đã giao thành công (COMPLETED)!", HttpStatus.BAD_REQUEST),
    RETURN_UNAUTHORIZED(1106, "Bạn không có quyền gửi yêu cầu đổi trả cho đơn hàng này!", HttpStatus.FORBIDDEN),
    RETURN_BANK_INFO_REQUIRED(1107, "Vui lòng cung cấp đầy đủ thông tin tài khoản ngân hàng nhận tiền hoàn đối với đơn thanh toán COD!", HttpStatus.BAD_REQUEST),
    RETURN_INVALID_STATUS(1108, "Trạng thái thẩm định chỉ được là APPROVED hoặc REJECTED!", HttpStatus.BAD_REQUEST),
    RETURN_REJECT_REASON_REQUIRED(1109, "Lý do từ chối không được để trống khi từ chối yêu cầu đổi trả!", HttpStatus.BAD_REQUEST),
    RETURN_RESTOCK_ITEMS_REQUIRED(1110, "Danh sách sản phẩm hoàn kho không được để trống khi chấp thuận yêu cầu đổi trả!", HttpStatus.BAD_REQUEST),

    // Nhóm 13: Storage & MinIO Upload
    STORAGE_INVALID_FILE_TYPE(1120, "Định dạng tập tin không hợp lệ hoặc không phải ảnh cho phép (hỗ trợ JPG, PNG, WEBP, GIF, SVG).", HttpStatus.BAD_REQUEST),
    STORAGE_FILE_SIZE_EXCEEDED(1121, "Kích thước tập tin vượt quá giới hạn tối đa cho phép (10MB).", HttpStatus.BAD_REQUEST),
    STORAGE_SESSION_NOT_FOUND(1122, "Phiên tải lên không tồn tại hoặc đã hết hạn.", HttpStatus.NOT_FOUND),
    STORAGE_SESSION_INVALID(1123, "Phiên tải lên không hợp lệ hoặc không thuộc quyền sở hữu.", HttpStatus.FORBIDDEN),
    STORAGE_FILE_NOT_FOUND(1124, "Tập tin chưa được tải lên máy chủ lưu trữ.", HttpStatus.BAD_REQUEST),
    STORAGE_FILE_CORRUPTED(1125, "Tập tin bị hỏng hoặc chữ ký định dạng (magic bytes) không hợp lệ.", HttpStatus.BAD_REQUEST),
    STORAGE_ERROR(1126, "Lỗi kết nối hoặc xử lý lưu trữ tập tin.", HttpStatus.INTERNAL_SERVER_ERROR),
    STORAGE_LIMIT_EXCEEDED(1127, "Số lượng tập tin tải lên vượt quá giới hạn cho phép.", HttpStatus.BAD_REQUEST);

    int code;
    String message;
    HttpStatus httpStatus;
}
