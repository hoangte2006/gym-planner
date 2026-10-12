package com.gym.app;

import com.gym.core.Config;
import com.gym.core.ui.PlaceholderPanel;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Cửa sổ chính: menu bên trái theo quyền, nội dung ở giữa, thanh trạng thái ở dưới. */
public class MainFrame extends JFrame {

    private static final long serialVersionUID = 1L;
    private static final String HOME_ID = "__home";

    private final CardLayout cards = new CardLayout();
    private final JPanel content = new JPanel(cards);
    private final Set<String> created = new HashSet<>();

    public MainFrame() {
        super("Gym Planner – Quản lý phòng tập");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1000, 640));
        setSize(1200, 720);
        setLocationRelativeTo(null);

        add(buildMenu(), BorderLayout.WEST);
        add(content, BorderLayout.CENTER);
        add(buildStatusBar(), BorderLayout.SOUTH);

        content.add(new PlaceholderPanel("Việc cần làm hôm nay", "TV1"), HOME_ID);
        created.add(HOME_ID);
        cards.show(content, HOME_ID);
    }

    private JPanel buildMenu() {
        JPanel menu = new JPanel();
        menu.setLayout(new BoxLayout(menu, BoxLayout.Y_AXIS));
        menu.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        menu.setPreferredSize(new Dimension(220, 0));

        JLabel brand = new JLabel("GYM PLANNER");
        brand.setFont(brand.getFont().deriveFont(Font.BOLD, 18f));
        brand.setBorder(BorderFactory.createEmptyBorder(0, 4, 16, 0));
        menu.add(brand);

        ButtonGroup group = new ButtonGroup();
        JToggleButton home = menuButton("Trang chủ", group);
        home.setSelected(true);
        home.addActionListener(e -> cards.show(content, HOME_ID));
        menu.add(home);
        menu.add(Box.createVerticalStrut(6));

        List<AppModule> modules = ModuleRegistry.forRole(Session.getRole());
        for (AppModule module : modules) {
            JToggleButton button = menuButton(module.getTitle(), group);
            button.addActionListener(e -> open(module));
            menu.add(button);
            menu.add(Box.createVerticalStrut(6));
        }
        menu.add(Box.createVerticalGlue());
        return menu;
    }

    private JToggleButton menuButton(String text, ButtonGroup group) {
        JToggleButton b = new JToggleButton(text);
        b.setAlignmentX(LEFT_ALIGNMENT);
        b.setHorizontalAlignment(JToggleButton.LEFT);
        b.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        b.setFocusPainted(false);
        group.add(b);
        return b;
    }

    /** Tạo panel của module ở lần mở đầu tiên, các lần sau chỉ chuyển thẻ. */
    private void open(AppModule module) {
        if (created.add(module.getId())) {
            content.add(module.createPanel(), module.getId());
        }
        cards.show(content, module.getId());
    }

    private JPanel buildStatusBar() {
        JPanel bar = new JPanel(new GridLayout(1, 2));
        bar.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));
        bar.add(new JLabel("Người dùng: " + Session.getStaffName()
                + "  |  Vai trò: " + Session.getRole().getLabel()));
        JLabel db = new JLabel("CSDL: " + Config.get().get("db.name", "GymPlanner")
                + " @ " + Config.get().get("db.host", "localhost"), JLabel.RIGHT);
        bar.add(db);
        return bar;
    }
}
