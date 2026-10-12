package com.gym.core.ui;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JWindow;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.Color;
import java.awt.Component;
import java.awt.Window;

/** Thông báo nhỏ tự biến mất sau vài giây (không chặn người dùng), dùng cho "Đã lưu", "Đã xóa"... */
public final class Toast {

    private static final int DURATION_MS = 2200;
    private static final int MARGIN = 24;

    private Toast() {
    }

    public static void show(Component owner, String message) {
        Window window = owner instanceof Window w ? w : (owner == null ? null : SwingUtilities.getWindowAncestor(owner));
        JWindow toast = new JWindow(window);
        toast.setFocusableWindowState(false);

        JLabel label = new JLabel(message);
        label.setForeground(Color.WHITE);
        label.setOpaque(true);
        label.setBackground(new Color(0x323232));
        label.setBorder(BorderFactory.createEmptyBorder(10, 18, 10, 18));
        toast.add(label);
        toast.pack();

        if (window != null && window.isShowing()) {
            int x = window.getX() + window.getWidth() - toast.getWidth() - MARGIN;
            int y = window.getY() + window.getHeight() - toast.getHeight() - MARGIN - 40;
            toast.setLocation(x, y);
        } else {
            toast.setLocationRelativeTo(null);
        }
        toast.setVisible(true);

        Timer timer = new Timer(DURATION_MS, e -> toast.dispose());
        timer.setRepeats(false);
        timer.start();
    }
}
