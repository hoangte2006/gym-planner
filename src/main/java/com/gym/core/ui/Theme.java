package com.gym.core.ui;

import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.FlatLightLaf;

import javax.swing.UIManager;
import java.awt.Color;

/** Giao diện chung của ứng dụng: FlatLaf, font hỗ trợ tiếng Việt, bảng màu. Gọi Theme.install() một lần ở Main. */
public final class Theme {

    public static final Color ACCENT = new Color(0x1565C0);
    public static final Color SUCCESS = new Color(0x2E7D32);
    public static final Color WARNING = new Color(0xEF6C00);
    public static final Color DANGER = new Color(0xC62828);
    public static final Color MUTED = new Color(0x757575);
    public static final Color INFO = new Color(0x6A1B9A);      // tím, trùng màu "Lớp tập" (SessionType.CLASS)

    private static boolean installed;

    private Theme() {
    }

    public static synchronized void install() {
        if (installed) {
            return;
        }
        FlatLaf.setPreferredFontFamily("Segoe UI");   // hỗ trợ đầy đủ tiếng Việt trên Windows
        FlatLightLaf.setup();
        UIManager.put("Component.arc", 8);
        UIManager.put("Button.arc", 8);
        UIManager.put("TextComponent.arc", 8);
        UIManager.put("Table.rowHeight", 28);
        UIManager.put("Component.accentColor", ACCENT);
        installed = true;
    }
}
