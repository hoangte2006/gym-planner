package com.gym.app;

import com.gym.core.model.Staff;
import com.gym.core.service.AuthService;
import com.gym.core.ui.Async;
import com.gym.core.ui.Dialogs;
import com.gym.core.ui.Theme;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

/** Màn hình đăng nhập: số điện thoại nhân viên và mật khẩu. Thành công thì mở MainFrame. */
public class LoginFrame extends JFrame {

    private static final long serialVersionUID = 1L;

    private final AuthService authService = new AuthService();
    private final JTextField phoneField = new JTextField(20);
    private final JPasswordField passwordField = new JPasswordField(20);
    private final JButton loginButton = new JButton("Đăng nhập");

    public LoginFrame() {
        super("Gym Planner – Đăng nhập");
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        JPanel content = new JPanel(new GridBagLayout());
        content.setBorder(BorderFactory.createEmptyBorder(28, 36, 28, 36));
        setContentPane(content);

        JLabel title = new JLabel("GYM PLANNER", SwingConstants.CENTER);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 24f));
        title.setForeground(Theme.ACCENT);
        JLabel subtitle = new JLabel("Đăng nhập bằng số điện thoại nhân viên", SwingConstants.CENTER);
        subtitle.setForeground(Theme.MUTED);

        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        c.insets = new Insets(4, 0, 4, 0);

        c.gridy = 0;
        content.add(title, c);
        c.gridy = 1;
        c.insets = new Insets(0, 0, 18, 0);
        content.add(subtitle, c);
        c.insets = new Insets(4, 0, 4, 0);
        c.gridy = 2;
        content.add(new JLabel("Số điện thoại"), c);
        c.gridy = 3;
        content.add(phoneField, c);
        c.gridy = 4;
        content.add(new JLabel("Mật khẩu"), c);
        c.gridy = 5;
        content.add(passwordField, c);
        c.gridy = 6;
        c.insets = new Insets(18, 0, 0, 0);
        loginButton.setBackground(Theme.ACCENT);
        loginButton.setForeground(Color.WHITE);
        content.add(loginButton, c);

        loginButton.addActionListener(e -> doLogin());
        getRootPane().setDefaultButton(loginButton);   // nhấn Enter là đăng nhập

        pack();
        setResizable(false);
        setLocationRelativeTo(null);
    }

    private void doLogin() {
        String phone = phoneField.getText();
        String password = new String(passwordField.getPassword());
        loginButton.setEnabled(false);
        //là một việc nền, xong thì gọi onLoginSuccess() hoặc hiện lỗi
        Async.run(this, () -> authService.login(phone, password),
                this::onLoginSuccess,
                error -> {
                    loginButton.setEnabled(true);
                    passwordField.setText("");
                    Dialogs.error(this, error);
                    passwordField.requestFocusInWindow();
                });
    }

    private void onLoginSuccess(Staff staff) {
        Session.login(staff);
        dispose();
        new MainFrame().setVisible(true);
    }
}
