package com.gym.core.ui;

import javax.swing.JComponent;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.KeyAdapter;
import java.awt.event.MouseAdapter;

/** Lớp phủ mờ "Đang xử lý..." chặn thao tác trong lúc chạy việc nền. Dùng qua {@link Async}. */
public class LoadingOverlay extends JComponent {

    private static final long serialVersionUID = 1L;

    private final String text;

    public LoadingOverlay(String text) {
        this.text = text;
        setOpaque(false);
        addMouseListener(new MouseAdapter() { });       // nuốt chuột để không bấm xuyên xuống dưới
        addMouseMotionListener(new MouseAdapter() { });
        addKeyListener(new KeyAdapter() { });
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setComposite(AlphaComposite.SrcOver.derive(0.65f));
        g2.setColor(Color.WHITE);
        g2.fillRect(0, 0, getWidth(), getHeight());

        g2.setComposite(AlphaComposite.SrcOver);
        g2.setFont(getFont().deriveFont(Font.BOLD, 16f));
        FontMetrics fm = g2.getFontMetrics();
        int w = fm.stringWidth(text) + 48;
        int h = fm.getHeight() + 24;
        int x = (getWidth() - w) / 2;
        int y = (getHeight() - h) / 2;
        g2.setColor(new Color(0x323232));
        g2.fillRoundRect(x, y, w, h, 12, 12);
        g2.setColor(Color.WHITE);
        g2.drawString(text, x + 24, y + 12 + fm.getAscent());
        g2.dispose();
    }
}
