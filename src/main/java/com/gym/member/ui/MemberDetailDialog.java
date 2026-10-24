package com.gym.member.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Window;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableModel;

import com.gym.core.model.Member;
import com.gym.core.model.Membership;
import com.gym.core.ui.Dialogs;
import com.gym.member.service.MemberService;

public class MemberDetailDialog extends JDialog {

    private final MemberService memberService;
    private final Member member;
    private boolean deleted = false;

    public MemberDetailDialog(Window owner, MemberService service, Member member) {
        super(owner, "Hồ sơ hội viên: " + member.getLastName() + " " + member.getFirstName(), ModalityType.APPLICATION_MODAL);
        this.memberService = service;
        this.member = member;

        initUI();
        setSize(560, 420);
        setLocationRelativeTo(owner);
    }

    private void initUI() {
        setLayout(new BorderLayout(8, 8));

        JTabbedPane tabbedPane = new JTabbedPane();

        // TAB 1: THÔNG TIN HỘI VIÊN
        JPanel pnlInfo = new JPanel(new BorderLayout(12, 12));
        pnlInfo.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JLabel lblAvatar = new JLabel("Chưa có ảnh", SwingConstants.CENTER);
        lblAvatar.setPreferredSize(new Dimension(110, 130));
        lblAvatar.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));

        byte[] photo = memberService.getMemberPhoto(member.getVisitorId());
        if (photo != null && photo.length > 0) {
            try {
                BufferedImage img = ImageIO.read(new ByteArrayInputStream(photo));
                lblAvatar.setIcon(new ImageIcon(img.getScaledInstance(110, 130, Image.SCALE_SMOOTH)));
                lblAvatar.setText("");
            } catch (java.io.IOException e) {
                lblAvatar.setText("Ảnh lỗi");
            }
        } else {
            lblAvatar.setText("Thiếu ảnh");
        }
        pnlInfo.add(lblAvatar, BorderLayout.WEST);

        JPanel pnlDetails = new JPanel(new GridLayout(5, 1, 6, 6));
        pnlDetails.add(new JLabel("Mã số: #" + member.getVisitorId()));
        pnlDetails.add(new JLabel("Họ và tên: " + member.getLastName() + " " + member.getFirstName()));
        pnlDetails.add(new JLabel("Số điện thoại: " + member.getPhone()));
        pnlDetails.add(new JLabel("Email: " + (member.getEmail() != null ? member.getEmail() : "(Trống)")));
        pnlDetails.add(new JLabel("Ngày sinh: " + (member.getBirthDate() != null ? member.getBirthDate().toString() : "(Chưa cập nhật)")));
        pnlInfo.add(pnlDetails, BorderLayout.CENTER);

        tabbedPane.addTab("Thông tin", pnlInfo);

        // TAB 2: LỊCH SỬ GÓI TẬP
        JPanel pnlHistory = new JPanel(new BorderLayout());
        String[] colNames = {"Mã HĐ", "Mã gói", "Ngày bắt đầu", "Ngày kết thúc", "Trạng thái"};
        DefaultTableModel model = new DefaultTableModel(colNames, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };

        List<Membership> list = memberService.getMembershipHistory(member.getVisitorId());
        for (Membership ms : list) {
            model.addRow(new Object[]{
                    ms.getMembershipId(),
                    ms.getPlanId(),
                    ms.getStartDate(),
                    ms.getEndDate(),
                    ms.getStatus() != null ? ms.getStatus().getLabel() : ""
            });
        }

        JTable tbl = new JTable(model);
        pnlHistory.add(new JScrollPane(tbl), BorderLayout.CENTER);
        tabbedPane.addTab("Lịch sử gói", pnlHistory);

        add(tabbedPane, BorderLayout.CENTER);

        // NÚT CHỨC NĂNG PHÍA DƯỚI
        JPanel pnlBottom = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        JButton btnDelete = new JButton("Xóa hội viên");
        btnDelete.setForeground(Color.RED);
        JButton btnClose = new JButton("Đóng");

        pnlBottom.add(btnDelete);
        pnlBottom.add(btnClose);
        add(pnlBottom, BorderLayout.SOUTH);

        btnClose.addActionListener(e -> dispose());
        btnDelete.addActionListener(e -> {
            if (Dialogs.confirm(this, "Hành động này sẽ xóa vĩnh viễn hồ sơ của hội viên #" + member.getVisitorId()
                    + ".\nBạn có chắc chắn muốn xóa?")) {
                try {
                    memberService.deleteMember(member.getVisitorId());
                    Dialogs.info(this, "Đã xóa hội viên thành công.");
                    deleted = true;
                    dispose();
                } catch (Exception ex) {
                    Dialogs.error(this, ex);
                }
            }
        });
    }

    public boolean isDeleted() {
        return deleted;
    }
}