package com.gym.dashboard.ui;

import com.gym.core.ui.Theme;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Font;

/** Một thẻ số liệu: tiêu đề nhỏ, con số lớn có màu, dòng chú thích. Dùng cho mọi chỉ số của Dashboard. */
public class StatCard extends JPanel {

    private static final long serialVersionUID = 1L;
    private static final String EMPTY_VALUE = "—";

    private final JLabel valueLabel = new JLabel(EMPTY_VALUE);
    private final JLabel noteLabel = new JLabel();

    public StatCard(String title, String note, Color accent) {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 5, 0, 0, accent),               // vạch màu bên trái
                BorderFactory.createEmptyBorder(14, 16, 14, 16)));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setForeground(Theme.MUTED);

        valueLabel.setFont(valueLabel.getFont().deriveFont(Font.BOLD, 32f));
        valueLabel.setForeground(accent);

        noteLabel.setText(note);
        noteLabel.setForeground(Theme.MUTED);
        noteLabel.setFont(noteLabel.getFont().deriveFont(12f));

        add(titleLabel);
        add(valueLabel);
        add(noteLabel);
    }

    /** Hiện con số (gọi trên luồng giao diện, ví dụ trong phần onSuccess của Async). */
    public void setValue(String text) {
        valueLabel.setText(text);
    }

    /** Đổi dòng chú thích nhỏ dưới con số (ví dụ khi đổi khoảng thời gian). */
    public void setNote(String text) {
        noteLabel.setText(text);
    }

    /** Về trạng thái chưa có số liệu. */
    public void clear() {
        valueLabel.setText(EMPTY_VALUE);
    }
}
