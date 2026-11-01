package com.wedbackendservice.nextgen.exception;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
@AllArgsConstructor
public enum ErrorCode {
    UNCATEGORIZE_EXCEPTION(9999, "Lỗi hệ thống không xác định", HttpStatus.INTERNAL_SERVER_ERROR),
    UNAUTHENTICATED(1001, "Tài khoản hoặc mật khẩu không chính xác.", HttpStatus.UNAUTHORIZED),
    USER_LOCKED(1002, "Tài khoản đang bị khóa.", HttpStatus.FORBIDDEN),
    UNAUTHORIZED(1003, "Bạn không có quyền thực hiện hành động này.", HttpStatus.FORBIDDEN),
    USER_NOT_EXIT(1004, "Người dùng không tồn tại.", HttpStatus.NOT_FOUND),
    TOO_MANY_REQUESTS(1005, "Đăng nhập sai quá 5 lần liên tiếp. Vui lòng thử lại sau.", HttpStatus.TOO_MANY_REQUESTS),
    OTP_INVALID_OR_EXPIRED(1006, "Mã OTP không chính xác hoặc đã hết hạn (quá 2 phút).", HttpStatus.BAD_REQUEST),
    SESSION_NOT_FOUND(1007, "sessionToken không tồn tại hoặc đã bị hủy.", HttpStatus.NOT_FOUND),
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
    CATEGORY_HAS_CHILDREN_OR_PRODUCTS(1022, "Không thể xóa danh mục đang chứa danh mục con hoặc sản phẩm.", HttpStatus.BAD_REQUEST),
    CATEGORY_HAS_PRODUCTS(1030, "Không thể xóa danh mục đang có sản phẩm. Vui lòng chuyển sản phẩm sang danh mục khác trước.", HttpStatus.BAD_REQUEST),
    CATEGORY_ALREADY_EXISTS(1031, "Tên danh mục đã tồn tại trong cùng một nhánh cha.", HttpStatus.CONFLICT),
    BRAND_NOT_FOUND(1023, "Thương hiệu không tồn tại.", HttpStatus.NOT_FOUND),
    BRAND_HAS_PRODUCTS(1024, "Không thể xóa thương hiệu này vì đang có sản phẩm thuộc hãng trên hệ thống. Hãy đổi hãng cho các sản phẩm trước hoặc ẩn thương hiệu thay vì xóa.", HttpStatus.BAD_REQUEST),
    BRAND_ALREADY_EXISTS(1032, "Tên thương hiệu đã tồn tại trên hệ thống.", HttpStatus.CONFLICT),
    VARIANT_NOT_FOUND(1025, "Biến thể sản phẩm không tồn tại.", HttpStatus.NOT_FOUND),
    INSUFFICIENT_STOCK(1026, "Số lượng tồn kho không đủ.", HttpStatus.BAD_REQUEST),
    CART_ITEM_NOT_FOUND(1027, "Sản phẩm không tồn tại trong giỏ hàng.", HttpStatus.NOT_FOUND),
    SHIPMENT_NOT_FOUND(1028, "Vận đơn không tồn tại.", HttpStatus.NOT_FOUND),
    ORDER_NOT_FOUND(1029, "Đơn hàng không tồn tại.", HttpStatus.NOT_FOUND);

    int code;
    String message;
    HttpStatus httpStatus;
}
