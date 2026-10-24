package com.gym.app;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import com.gym.core.DBConnection;
import com.gym.core.exception.DataAccessException;
import com.gym.core.ui.Theme;

/** Điểm khởi động: giao diện FlatLaf, kiểm tra kết nối DB, rồi mở màn hình đăng nhập. */
public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        Theme.install();
        SwingUtilities.invokeLater(() -> {
            try {
                DBConnection.test();
            } catch (DataAccessException e) {
                JOptionPane.showMessageDialog(null, e.getMessage(),
                        "Không kết nối được cơ sở dữ liệu", JOptionPane.ERROR_MESSAGE);
                System.exit(1);
            }
            new LoginFrame().setVisible(true);
        });
    }
}
