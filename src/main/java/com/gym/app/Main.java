package com.gym.app;

import com.formdev.flatlaf.FlatLightLaf;
import com.gym.core.DBConnection;
import com.gym.core.exception.DataAccessException;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

/** Điểm khởi động: giao diện FlatLaf, kiểm tra kết nối DB rồi mở cửa sổ chính. */
public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        FlatLightLaf.setup();
        SwingUtilities.invokeLater(() -> {
            try {
                DBConnection.test();
            } catch (DataAccessException e) {
                JOptionPane.showMessageDialog(null, e.getMessage(),
                        "Không kết nối được cơ sở dữ liệu", JOptionPane.ERROR_MESSAGE);
                System.exit(1);
            }
            new MainFrame().setVisible(true);
        });
    }
}
