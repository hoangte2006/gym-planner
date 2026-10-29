package com.gym.member.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Window;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableModel;

import com.gym.app.AppModule;
import com.gym.app.ModuleRegistry;
import com.gym.core.model.BillingLog;
import com.gym.core.model.Member;
import com.gym.core.model.Membership;
import com.gym.core.ui.Dialogs;
import com.gym.member.service.MemberService;

public class MemberDetailDialog extends JDialog {

    private final MemberService memberService;
    private final Member member;
    private boolean deleted = false;

    public MemberDetailDialog(Window owner, MemberService service, Member member) {
        super(owner, "Hồ sơ hội viên: " + member.getLastName() + " " + member.getFirstName() + " [#" + member.getVisitorId() + "]", ModalityType.APPLICATION_MODAL);
        this.memberService = service;
        this.member = member;

        initUI();
        setupUX12Esc();

        setSize(700, 480);
        setLocationRelativeTo(owner);
    }

    private void initUI() {
        setLayout(new BorderLayout(8, 8));

        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.addTab("1. Thông tin", createInfoTab());
        tabbedPane.addTab("2. Gói tập", createMembershipTab());
        tabbedPane.addTab("3. Hóa đơn", createBillingTab());
        tabbedPane.addTab("4. Lịch tập & Lớp", createSchedulingTab());
        tabbedPane.addTab("5. Thẻ ra vào", createAccessTab());

        add(tabbedPane, BorderLayout.CENTER);

        // Thanh điều khiển dưới cùng
        JPanel pnlBottom = new JPanel(new BorderLayout());
        
        // UX-17: Nút trợ giúp ngắn
        JButton btnHelp = new JButton("?");
        btnHelp.setToolTipText("Mục đích hồ sơ hội viên (UX-17)");
        btnHelp.addActionListener(e -> showHelpDialog());

        JPanel pnlLeft = new JPanel(new FlowLayout(FlowLayout.LEFT));
        pnlLeft.add(btnHelp);

        JPanel pnlRight = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        JButton btnDelete = new JButton("Xóa hội viên");
        btnDelete.setForeground(Color.RED);
        btnDelete.setToolTipText("Ràng buộc BR-MEM-08: Không thể xóa khi còn hóa đơn hoặc hợp đồng");

        JButton btnClose = new JButton("Đóng (Esc)");
        pnlRight.add(btnDelete);
        pnlRight.add(btnClose);

        pnlBottom.add(pnlLeft, BorderLayout.WEST);
        pnlBottom.add(pnlRight, BorderLayout.EAST);
        add(pnlBottom, BorderLayout.SOUTH);

        // UX-12: Nút Enter nhận hành động đóng mặc định khi mở xem
        getRootPane().setDefaultButton(btnClose);
        btnClose.addActionListener(e -> dispose());
        btnDelete.addActionListener(e -> handleDelete());
    }

    // UX-12: Phím Esc đóng cửa sổ ngay lập tức
    private void setupUX12Esc() {
        getRootPane().registerKeyboardAction(e -> dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW);
    }

    private JPanel createInfoTab() {
        JPanel pnl = new JPanel(new BorderLayout(15, 15));
        pnl.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JLabel lblAvatar = new JLabel("Chưa có ảnh", SwingConstants.CENTER);
        lblAvatar.setPreferredSize(new Dimension(130, 160));
        lblAvatar.setBorder(BorderFactory.createLineBorder(Color.GRAY));

        byte[] photo = memberService.getMemberPhoto(member.getVisitorId());
        if (photo != null && photo.length > 0) {
            try {
                BufferedImage img = ImageIO.read(new ByteArrayInputStream(photo));
                lblAvatar.setIcon(new ImageIcon(img.getScaledInstance(130, 160, Image.SCALE_SMOOTH)));
                lblAvatar.setText("");
            } catch (java.io.IOException e) {
                lblAvatar.setText("Ảnh lỗi");
            }
        }
        pnl.add(lblAvatar, BorderLayout.WEST);

        JPanel pnlDetails = new JPanel(new GridLayout(6, 1, 6, 6));
        pnlDetails.add(new JLabel("Mã số: #" + member.getVisitorId()));
        pnlDetails.add(new JLabel("Họ và tên: " + member.getLastName() + " " + member.getFirstName()));
        pnlDetails.add(new JLabel("Số điện thoại: " + member.getPhone()));
        pnlDetails.add(new JLabel("Email: " + (member.getEmail() != null ? member.getEmail() : "(Trống)")));
        pnlDetails.add(new JLabel("Ngày sinh: " + (member.getBirthDate() != null ? member.getBirthDate().toString() : "(Chưa cập nhật)")));
        pnlDetails.add(new JLabel("Ngày đăng ký: " + (member.getRegistrationDate() != null ? member.getRegistrationDate().toString() : "")));

        pnl.add(pnlDetails, BorderLayout.CENTER);
        return pnl;
    }

    private JPanel createMembershipTab() {
        JPanel pnl = new JPanel(new BorderLayout(6, 6));
        pnl.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        List<Membership> list = memberService.getMembershipHistory(member.getVisitorId());

        // UX-11: Empty State khi hội viên chưa có gói tập
        if (list.isEmpty()) {
            JPanel pnlEmpty = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 20));
            pnlEmpty.add(new JLabel("Hội viên chưa đăng ký gói tập nào. Bấm"));
            JButton btnAction = new JButton("[+ Mua gói tập]");
            btnAction.addActionListener(e -> navigateToModule("facility"));
            pnlEmpty.add(btnAction);
            pnlEmpty.add(new JLabel("để bắt đầu."));
            pnl.add(pnlEmpty, BorderLayout.CENTER);
        } else {
            String[] cols = {"Mã HĐ", "Mã gói", "Ngày bắt đầu", "Ngày kết thúc", "Trạng thái"};
            DefaultTableModel model = new DefaultTableModel(cols, 0) {
                @Override public boolean isCellEditable(int r, int c) { return false; }
            };
            for (Membership ms : list) {
                model.addRow(new Object[]{
                        ms.getMembershipId(), ms.getPlanId(), ms.getStartDate(), ms.getEndDate(),
                        ms.getStatus() != null ? ms.getStatus().getLabel() : ""
                });
            }
            pnl.add(new JScrollPane(new JTable(model)), BorderLayout.CENTER);
        }

        JPanel pnlNav = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnGo = new JButton("Đến Module Gói tập ->");
        btnGo.addActionListener(e -> navigateToModule("facility"));
        pnlNav.add(btnGo);
        pnl.add(pnlNav, BorderLayout.SOUTH);

        return pnl;
    }

    private JPanel createBillingTab() {
        JPanel pnl = new JPanel(new BorderLayout(6, 6));
        pnl.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        List<BillingLog> bills = memberService.getBillingHistory(member.getVisitorId());

        // UX-11: Empty State khi chưa có hóa đơn
        if (bills.isEmpty()) {
            JPanel pnlEmpty = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 20));
            pnlEmpty.add(new JLabel("Chưa có hóa đơn nào của hội viên này. Bấm"));
            JButton btnAction = new JButton("[+ Lập hóa đơn]");
            btnAction.addActionListener(e -> navigateToModule("billing"));
            pnlEmpty.add(btnAction);
            pnlEmpty.add(new JLabel("để thanh toán."));
            pnl.add(pnlEmpty, BorderLayout.CENTER);
        } else {
            String[] cols = {"Mã HĐ", "Số tiền", "Trạng thái", "Ngày TT", "Hạn TT"};
            DefaultTableModel model = new DefaultTableModel(cols, 0) {
                @Override public boolean isCellEditable(int r, int c) { return false; }
            };
            for (BillingLog b : bills) {
                model.addRow(new Object[]{
                        b.getBillId(), b.getAmountPaid(), b.getBillingStatus(), b.getPaymentDate(), b.getDueDate()
                });
            }
            pnl.add(new JScrollPane(new JTable(model)), BorderLayout.CENTER);
        }

        JPanel pnlNav = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnGo = new JButton("Mở Module Hóa đơn (Billing) ->");
        btnGo.addActionListener(e -> navigateToModule("billing"));
        pnlNav.add(btnGo);
        pnl.add(pnlNav, BorderLayout.SOUTH);

        return pnl;
    }

    private JPanel createSchedulingTab() {
        JPanel pnl = new JPanel(new BorderLayout(6, 6));
        pnl.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        // UX-11: Empty State kèm nút hành động chính
        JPanel pnlEmpty = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 20));
        pnlEmpty.add(new JLabel("Hội viên chưa có lịch học lớp hoặc PT. Bấm"));
        JButton btnAction = new JButton("[+ Đặt lịch tập]");
        btnAction.addActionListener(e -> navigateToModule("scheduling"));
        pnlEmpty.add(btnAction);
        pnlEmpty.add(new JLabel("để xếp lịch."));
        pnl.add(pnlEmpty, BorderLayout.CENTER);

        JPanel pnlNav = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnGo = new JButton("Mở Lịch tập (Scheduling) ->");
        btnGo.addActionListener(e -> navigateToModule("scheduling"));
        pnlNav.add(btnGo);
        pnl.add(pnlNav, BorderLayout.SOUTH);

        return pnl;
    }

    private JPanel createAccessTab() {
        JPanel pnl = new JPanel(new BorderLayout(6, 6));
        pnl.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JPanel pnlContent = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 20));
        pnlContent.add(new JLabel("Hồ sơ đã kích hoạt nhận diện. Bấm"));
        JButton btnAction = new JButton("[Kiểm tra thiết bị]");
        btnAction.addActionListener(e -> navigateToModule("facility"));
        pnlContent.add(btnAction);
        pnlContent.add(new JLabel("để đồng bộ cửa từ."));
        pnl.add(pnlContent, BorderLayout.CENTER);

        return pnl;
    }

    private void navigateToModule(String moduleId) {
        boolean exists = false;
        for (AppModule m : ModuleRegistry.all()) {
            if (m.getId().equalsIgnoreCase(moduleId)) {
                exists = true;
                break;
            }
        }
        if (exists) {
            dispose();
            Dialogs.info(getOwner(), "Chuyển hướng sang module: " + moduleId.toUpperCase());
        } else {
            Dialogs.warn(this, "Module [" + moduleId + "] chưa sẵn sàng.");
        }
    }

    private void handleDelete() {
        if (Dialogs.confirmDelete(this, "hội viên #" + member.getVisitorId())) {
            try {
                memberService.deleteMember(member.getVisitorId());
                Dialogs.info(this, "Đã xóa hội viên thành công.");
                deleted = true;
                dispose();
            } catch (Exception ex) {
                Dialogs.error(this, ex);
            }
        }
    }

    // UX-17: Hướng dẫn ngắn đúng 3 dòng
    private void showHelpDialog() {
        String msg = """
                Hồ sơ tổng hợp toàn diện thông tin một hội viên.
                - Theo dõi gói tập, lịch sử hóa đơn và các lớp học đã đăng ký.
                - Cung cấp liên kết nhanh sang các phân hệ liên quan mà không cần đóng ứng dụng.
                """;
        JOptionPane.showMessageDialog(this, msg.strip(), "Mục đích màn hình (UX-17)", JOptionPane.INFORMATION_MESSAGE);
    }

    public boolean isDeleted() {
        return deleted;
    }
}