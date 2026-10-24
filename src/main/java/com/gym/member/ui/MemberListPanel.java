package com.gym.member.ui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import com.gym.core.model.Member;
import com.gym.core.ui.DataTable;
import com.gym.core.ui.Dialogs;
import com.gym.member.service.MemberService;

public class MemberListPanel extends JPanel {

    private final MemberService memberService = new MemberService();
    private final DataTable<Member> dataTable = new DataTable<>(20);
    private final JTextField txtSearch = new JTextField(18);

    public MemberListPanel() {
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        dataTable.addColumn("ID", Member::getVisitorId, 60)
                 .addColumn("Họ", Member::getLastName, 120)
                 .addColumn("Tên", Member::getFirstName, 100)
                 .addColumn("Số điện thoại", Member::getPhone, 120)
                 .addColumn("Email", Member::getEmail, 180)
                 .addColumn("Ngày sinh", Member::getBirthDate, 100)
                 .addColumn("Có ảnh", Member::hasPhoto, 80);

        applyFilter();

        dataTable.onDoubleClick(this::openMemberDetail);

        JPanel topBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        JButton btnSearch = new JButton("Tìm");
        JButton btnDetail = new JButton("Hồ sơ");
        JButton btnAdd = new JButton("Thêm hội viên");
        JButton btnEdit = new JButton("Sửa");
        JButton btnDailyPass = new JButton("Bán vé ngày");

        topBar.add(new JLabel("Tìm kiếm:"));
        topBar.add(txtSearch);
        topBar.add(btnSearch);
        topBar.add(Box.createHorizontalStrut(15));
        topBar.add(btnDetail);
        topBar.add(btnAdd);
        topBar.add(btnEdit);
        topBar.add(btnDailyPass);

        add(topBar, BorderLayout.NORTH);
        add(dataTable, BorderLayout.CENTER);

        btnSearch.addActionListener(e -> applyFilter());
        txtSearch.addActionListener(e -> applyFilter());

        btnDetail.addActionListener(e -> {
            Member selected = dataTable.getSelected();
            if (selected == null) {
                Dialogs.warn(this, "Vui lòng chọn một hội viên để xem hồ sơ.");
                return;
            }
            openMemberDetail(selected);
        });

        btnAdd.addActionListener(e -> {
            MemberFormDialog dlg = new MemberFormDialog(SwingUtilities.getWindowAncestor(this), memberService, null);
            dlg.setVisible(true);
            if (dlg.isSaved()) dataTable.reload();
        });

        btnEdit.addActionListener(e -> {
            Member selected = dataTable.getSelected();
            if (selected == null) {
                Dialogs.warn(this, "Vui lòng chọn một hội viên cần sửa.");
                return;
            }
            MemberFormDialog dlg = new MemberFormDialog(SwingUtilities.getWindowAncestor(this), memberService, selected);
            dlg.setVisible(true);
            if (dlg.isSaved()) dataTable.reload();
        });

        btnDailyPass.addActionListener(e -> {
            DailyPassDialog dlg = new DailyPassDialog(SwingUtilities.getWindowAncestor(this), memberService);
            dlg.setVisible(true);
        });
    }

    private void applyFilter() {
        String keyword = txtSearch.getText().trim();
        dataTable.setLoader((page, size) -> memberService.searchMembers(keyword, page, size));
        dataTable.reload();
    }

    private void openMemberDetail(Member member) {
        MemberDetailDialog dlg = new MemberDetailDialog(SwingUtilities.getWindowAncestor(this), memberService, member);
        dlg.setVisible(true);
        if (dlg.isDeleted()) dataTable.reload();
    }
}