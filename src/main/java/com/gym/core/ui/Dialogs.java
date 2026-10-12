package com.gym.core.ui;

import com.gym.core.ErrorTranslator;

import javax.swing.JOptionPane;
import java.awt.Component;

/**
 * Hộp thoại thông báo dùng chung. Màn hình KHÔNG tự gọi JOptionPane; luôn dùng lớp này
 * để mọi thông báo cùng một kiểu và mọi lỗi đều được dịch ra tiếng Việt.
 */
public final class Dialogs {

    private Dialogs() {
    }

    public static void info(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "Thông báo", JOptionPane.INFORMATION_MESSAGE);
    }

    public static void warn(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "Chú ý", JOptionPane.WARNING_MESSAGE);
    }

    public static void error(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "Lỗi", JOptionPane.ERROR_MESSAGE);
    }

    /** Hiện lỗi bất kỳ: ErrorTranslator tự đổi thành câu tiếng Việt dễ hiểu. */
    public static void error(Component parent, Throwable cause) {
        error(parent, ErrorTranslator.toMessage(cause));
    }

    /** Hỏi Có/Không. Trả về true nếu người dùng chọn Có. */
    public static boolean confirm(Component parent, String message) {
        return JOptionPane.showConfirmDialog(parent, message, "Xác nhận",
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION;
    }

    /** Hỏi xác nhận xóa, nút mặc định là Không để tránh bấm nhầm. */
    public static boolean confirmDelete(Component parent, String what) {
        Object[] options = {"Xóa", "Không xóa"};
        int choice = JOptionPane.showOptionDialog(parent, "Bạn có chắc muốn xóa " + what + "?\nThao tác này không thể hoàn tác.",
                "Xác nhận xóa", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE, null, options, options[1]);
        return choice == 0;
    }
}
