package com.gym.core;

import com.gym.core.exception.BusinessException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Kiểm tra dữ liệu người dùng nhập trước khi gửi xuống DB. Gom MỌI lỗi để hiện một lần
 * (người dùng không phải sửa từng lỗi một).
 *
 * <pre>
 * Validator v = new Validator()
 *         .name("Họ", lastName)
 *         .phone("Số điện thoại", phone)
 *         .email("Email", email);
 * v.throwIfInvalid();      // ném BusinessException liệt kê tất cả lỗi
 * </pre>
 */
public final class Validator {

    private static final Pattern PHONE = Pattern.compile("^0\\d{9,10}$");
    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final Pattern NAME = Pattern.compile("^[\\p{L}][\\p{L} .'-]*$");

    public static final int NAME_MAX = 50;
    public static final int EMAIL_MAX = 100;
    public static final int MIN_AGE = 10;
    public static final int MAX_AGE = 100;

    private final List<String> errors = new ArrayList<>();

    // ---------- Kiểm tra từng giá trị, dùng được riêng lẻ ----------

    public static boolean isPhone(String value) {
        return value != null && PHONE.matcher(value.trim()).matches();
    }

    public static boolean isEmail(String value) {
        return value != null && value.trim().length() <= EMAIL_MAX && EMAIL.matcher(value.trim()).matches();
    }

    public static boolean isName(String value) {
        return value != null && value.trim().length() <= NAME_MAX && NAME.matcher(value.trim()).matches();
    }

    // ---------- Quy tắc xếp chuỗi (gọi nối tiếp nhau) ----------

    /** Bắt buộc nhập. */
    public Validator required(String label, String value) {
        if (value == null || value.isBlank()) {
            errors.add(label + " không được để trống.");
        }
        return this;
    }

    /** Họ/tên người: bắt buộc, chỉ chữ cái, dấu cách, dấu chấm, gạch nối, nháy; tối đa 50 ký tự. */
    public Validator name(String label, String value) {
        if (value == null || value.isBlank()) {
            errors.add(label + " không được để trống.");
        } else if (value.trim().length() > NAME_MAX) {
            errors.add(label + " tối đa " + NAME_MAX + " ký tự.");
        } else if (!isName(value)) {
            errors.add(label + " chỉ được chứa chữ cái và dấu cách.");
        }
        return this;
    }

    /** SĐT: bắt buộc, 10 hoặc 11 chữ số, bắt đầu bằng 0. */
    public Validator phone(String label, String value) {
        if (value == null || value.isBlank()) {
            errors.add(label + " không được để trống.");
        } else if (!isPhone(value)) {
            errors.add(label + " phải gồm 10–11 chữ số và bắt đầu bằng 0 (ví dụ: 0912345678).");
        }
        return this;
    }

    /** Email: bắt buộc, đúng định dạng, tối đa 100 ký tự. */
    public Validator email(String label, String value) {
        if (value == null || value.isBlank()) {
            errors.add(label + " không được để trống.");
        } else if (!isEmail(value)) {
            errors.add(label + " không đúng định dạng (ví dụ: ten@gmail.com).");
        }
        return this;
    }

    public Validator maxLength(String label, String value, int max) {
        if (value != null && value.trim().length() > max) {
            errors.add(label + " tối đa " + max + " ký tự.");
        }
        return this;
    }

    // ---------- Số và ngày ----------

    public Validator positive(String label, BigDecimal value) {
        if (value == null || value.signum() <= 0) {
            errors.add(label + " phải lớn hơn 0.");
        }
        return this;
    }

    public Validator nonNegative(String label, BigDecimal value) {
        if (value == null || value.signum() < 0) {
            errors.add(label + " không được âm.");
        }
        return this;
    }

    public Validator positive(String label, int value) {
        if (value <= 0) {
            errors.add(label + " phải lớn hơn 0.");
        }
        return this;
    }

    /** Ngày sinh (không bắt buộc): nếu có thì không ở tương lai và tuổi từ 10 đến 100. */
    public Validator birthDate(String label, LocalDate value, LocalDate today) {
        if (value == null) {
            return this;
        }
        if (value.isAfter(today)) {
            errors.add(label + " không được ở tương lai.");
        } else {
            int age = today.getYear() - value.getYear()
                    - (today.getDayOfYear() < value.getDayOfYear() ? 1 : 0);
            if (age < MIN_AGE || age > MAX_AGE) {
                errors.add(label + " không hợp lý (tuổi phải từ " + MIN_AGE + " đến " + MAX_AGE + ").");
            }
        }
        return this;
    }

    /** Ngày kết thúc phải sau ngày bắt đầu. */
    public Validator dateOrder(String label, LocalDate start, LocalDate end) {
        if (start == null || end == null) {
            errors.add(label + ": phải chọn đủ ngày bắt đầu và ngày kết thúc.");
        } else if (!end.isAfter(start)) {
            errors.add(label + ": ngày kết thúc phải sau ngày bắt đầu.");
        }
        return this;
    }

    /** Điều kiện tùy ý: sai thì thêm lỗi với câu thông báo cho sẵn. */
    public Validator check(boolean condition, String message) {
        if (!condition) {
            errors.add(message);
        }
        return this;
    }

    // ---------- Kết quả ----------

    public boolean isValid() {
        return errors.isEmpty();
    }

    public List<String> getErrors() {
        return Collections.unmodifiableList(errors);
    }

    /** Mọi lỗi, mỗi lỗi một dòng. Chuỗi rỗng nếu hợp lệ. */
    public String message() {
        return String.join("\n", errors);
    }

    /** Ném BusinessException (thông điệp tiếng Việt) nếu có lỗi. */
    public void throwIfInvalid() {
        if (!isValid()) {
            throw new BusinessException(message());
        }
    }
}
