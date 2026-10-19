package com.gym.core;

import com.gym.core.exception.BusinessException;
import com.gym.core.exception.DataAccessException;

import java.sql.SQLException;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Đổi mọi lỗi (đặc biệt lỗi từ trigger và ràng buộc của SQL Server) thành câu tiếng Việt dễ hiểu.
 * Giao diện chỉ cần gọi {@link #toMessage(Throwable)} rồi hiện hộp thoại.
 */
public final class ErrorTranslator {

    /** Lỗi do trigger THROW 50001–50029 (xem db/02_triggers.sql). */
    private static final Map<Integer, String> TRIGGER_MESSAGES = Map.ofEntries(
            Map.entry(50001, "Người này đang giữ vé ngày nên không thể đăng ký làm hội viên."),
            Map.entry(50002, "Người này đã là hội viên nên không thể mua vé ngày."),
            Map.entry(50003, "Hội viên chưa có ảnh nên chưa thể đăng ký gói tập. Hãy thêm ảnh trước."),
            Map.entry(50004, "Hội viên chưa có ảnh nên chưa thể đăng ký lớp. Hãy thêm ảnh trước."),
            Map.entry(50005, "Hội viên chưa có ảnh nên chưa thể đặt lịch PT. Hãy thêm ảnh trước."),
            Map.entry(50006, "Hóa đơn phải thuộc về chủ của gói tập được chọn."),
            Map.entry(50007, "Nhân viên này đã là lễ tân nên không thể đồng thời là huấn luyện viên."),
            Map.entry(50008, "Nhân viên này đã là huấn luyện viên nên không thể đồng thời là lễ tân."),
            Map.entry(50009, "Số chỗ tối đa của lớp không được lớn hơn sức chứa của phòng."),
            Map.entry(50010, "Phòng mới quá nhỏ so với số chỗ tối đa của lớp."),
            Map.entry(50011, "Có lớp trong phòng này đang có số chỗ lớn hơn sức chứa mới của phòng."),
            Map.entry(50012, "Lớp đã đủ người, không thể đăng ký thêm."),
            Map.entry(50013, "Phòng đã có buổi khác trong khoảng giờ này. Hãy chọn phòng hoặc giờ khác."),
            Map.entry(50014, "Huấn luyện viên đã bận trong khoảng giờ này. Hãy chọn người hoặc giờ khác."),
            Map.entry(50015, "Không thể khôi phục buổi PT: phòng đã bị buổi khác chiếm sau khi hủy."),
            Map.entry(50016, "Không thể khôi phục buổi PT: huấn luyện viên đã nhận buổi khác sau khi hủy."),
            Map.entry(50017, "Hội viên không có gói tập còn hiệu lực nên không thể đăng ký lớp."),
            Map.entry(50018, "Buổi này không phải là lớp tập (hoặc đã là buổi PT)."),
            Map.entry(50019, "Buổi này không phải là buổi PT (hoặc đã là lớp tập)."),
            Map.entry(50020, "Không được đổi loại buổi sau khi đã tạo lớp hoặc lịch PT."),
            Map.entry(50021, "Thiết bị đang hỏng hoặc đang bảo trì nên không thể đặt."),
            Map.entry(50022, "Nhật ký bảo trì phải do lễ tân hoặc quản lý ghi."),
            Map.entry(50023, "Giờ đặt thiết bị phải nằm trong giờ của buổi tập."),
            Map.entry(50024, "Thiết bị đã được buổi khác đặt trong khoảng giờ này."),
            Map.entry(50025, "Đổi giờ buổi tập sẽ làm giờ đặt thiết bị nằm ngoài buổi. Hãy sửa đặt thiết bị trước."),
            Map.entry(50026, "Chỉ nhân viên có vai trò Huấn luyện viên mới có hồ sơ huấn luyện viên."),
            Map.entry(50027, "Chỉ nhân viên có vai trò Lễ tân mới có hồ sơ lễ tân."),
            Map.entry(50028, "Không thể đổi vai trò vì nhân viên đã có hồ sơ huấn luyện viên hoặc lễ tân."),
            Map.entry(50029, "Không thể khôi phục buổi PT: thiết bị đã được buổi khác đặt sau khi hủy."));

    /** Ràng buộc UNIQUE (lỗi 2601, 2627). Khóa là tên constraint hoặc index. */
    private static final Map<String, String> UNIQUE_MESSAGES = Map.of(
            "UQ_VISITOR_Phone", "Số điện thoại này đã được dùng cho một khách khác.",
            "UQ_MEMBER_Email", "Email này đã được dùng cho một hội viên khác.",
            "UQ_PLAN_Name", "Đã có gói tập trùng tên.",
            "UQ_STAFF_Phone", "Số điện thoại này đã được dùng cho một nhân viên khác.",
            "UQ_ROOM_Name", "Đã có phòng trùng tên.");

    /** Ràng buộc CHECK (lỗi 547). */
    private static final Map<String, String> CHECK_MESSAGES = Map.ofEntries(
            Map.entry("CK_DPT_Rate", "Giá vé ngày phải lớn hơn 0."),
            Map.entry("CK_PLAN_Duration", "Thời hạn gói phải lớn hơn 0 tháng."),
            Map.entry("CK_PLAN_Price", "Giá gói không được âm."),
            Map.entry("CK_MEMBERSHIP_Dates", "Ngày kết thúc phải sau ngày bắt đầu."),
            Map.entry("CK_BILL_Amount", "Số tiền không được âm."),
            Map.entry("CK_STAFF_Salary", "Lương phải lớn hơn 0."),
            Map.entry("CK_ROOM_Capacity", "Sức chứa phòng phải lớn hơn 0."),
            Map.entry("CK_SESSION_Time", "Giờ kết thúc phải sau giờ bắt đầu."),
            Map.entry("CK_CLASS_MaxCap", "Số chỗ tối đa của lớp phải lớn hơn 0."),
            Map.entry("CK_MLOG_Cost", "Chi phí bảo trì không được âm."),
            Map.entry("CK_SEU_Time", "Giờ kết thúc đặt thiết bị phải sau giờ bắt đầu."));

    private static final Pattern CONSTRAINT_NAME = Pattern.compile("(?:constraint|index) ['\"]([^'\"]+)['\"]");

    private ErrorTranslator() {
    }

    /** Thông điệp tiếng Việt để hiện cho người dùng. Không bao giờ trả về null. */
    public static String toMessage(Throwable error) {
        if (error == null) {
            return "Đã xảy ra lỗi không xác định.";
        }
        if (error instanceof BusinessException || error instanceof IllegalArgumentException) {
            return String.valueOf(error.getMessage());
        }
        SQLException sql = findSqlException(error);
        if (sql != null) {
            return fromSql(sql);
        }
        if (error instanceof DataAccessException && error.getMessage() != null) {
            return error.getMessage();
        }
        return "Đã xảy ra lỗi: " + error.getMessage();
    }

    private static SQLException findSqlException(Throwable error) {
        Throwable t = error;
        while (t != null) {
            if (t instanceof SQLException sql) {
                return sql;
            }
            t = t.getCause() == t ? null : t.getCause();
        }
        return null;
    }

    private static String fromSql(SQLException e) {
        int code = e.getErrorCode();
        String raw = String.valueOf(e.getMessage());
        String known = TRIGGER_MESSAGES.get(code);
        if (known != null) {
            return known;
        }
        String constraint = constraintName(raw);
        switch (code) {
            case 2601:
            case 2627:
                if (constraint != null && UNIQUE_MESSAGES.containsKey(constraint)) {
                    return UNIQUE_MESSAGES.get(constraint);
                }
                return "Dữ liệu bị trùng với bản ghi đã có.";
            case 547:
                if (constraint != null && CHECK_MESSAGES.containsKey(constraint)) {
                    return CHECK_MESSAGES.get(constraint);
                }
                if (raw.contains("DELETE") || raw.contains("REFERENCE constraint")) {
                    return "Không thể xóa vì dữ liệu này đang được sử dụng ở nơi khác.";
                }
                return "Dữ liệu tham chiếu không tồn tại hoặc không hợp lệ.";
            case 229:
            case 230:
            case 262:
                // Thiếu quyền trên bảng/cột/lệnh (script 10, 11 phân quyền RBAC)
                return "Bạn không có quyền thực hiện thao tác này.";
            case 1205:
                return "Hệ thống đang bận xử lý, vui lòng thử lại.";
            default:
                return "Lỗi cơ sở dữ liệu: " + raw;
        }
    }

    private static String constraintName(String message) {
        Matcher m = CONSTRAINT_NAME.matcher(message);
        return m.find() ? m.group(1) : null;
    }
}
