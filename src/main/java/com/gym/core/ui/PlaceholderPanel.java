package com.gym.core.ui;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Font;

/** Panel tạm cho module chưa làm. Chủ module thay bằng màn hình thật. */
public class PlaceholderPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    public PlaceholderPanel(String title, String owner) {
        super(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));
        JLabel heading = new JLabel(title, SwingConstants.CENTER);
        heading.setFont(heading.getFont().deriveFont(Font.BOLD, 24f));
        JLabel note = new JLabel("Module đang được phát triển (" + owner + ")", SwingConstants.CENTER);
        add(heading, BorderLayout.CENTER);
        add(note, BorderLayout.SOUTH);
    }
}
