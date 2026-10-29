package com.gym.member.ui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

import com.gym.core.model.Member;
import com.gym.core.ui.DataTable;
import com.gym.core.ui.Dialogs;
import com.gym.member.service.MemberService;

public class MemberListPanel extends JPanel {

    private final MemberService memberService = new MemberService();
    private final DataTable<Member> dataTable = new DataTable<>(20);
    private final JTextField txtSearch = new JTextField(16);

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

        dataTable.onDoubleClick(this::openMemberDetail);

        JPanel topBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        JButton btnSearch = new JButton("Tìm (Enter)");
        JButton btnDetail = new JButton("Hồ sơ");
        JButton btnAdd = new JButton("+ Thêm hội viên (Ctrl+N)");
        JButton btnEdit = new JButton("Sửa");
        JButton btnDailyPass = new JButton("Bán vé ngày");
        JButton btnUpgrade = new JButton("Nâng cấp vé ngày");
        JButton btnExport = new JButton("Xuất CSV (Ctrl+E)");
        JButton btnHelp = new JButton("?");

        // UX-17: Tooltips giải thích chức năng
        txtSearch.setToolTipText("Nhập họ tên hoặc số điện thoại (Ctrl+F)");
        btnSearch.setToolTipText("Lọc danh sách hội viên");
        btnDetail.setToolTipText("Xem hồ sơ 5 tab của hội viên được chọn");
        btnAdd.setToolTipText("Thêm hội viên mới (Ctrl+N)");
        btnDailyPass.setToolTipText("Bán vé ngày cho khách vãng lai");
        btnUpgrade.setToolTipText("Nâng cấp vé ngày thành hội viên chính thức");
        btnExport.setToolTipText("Xuất kết quả tìm kiếm ra tệp CSV (Ctrl+E)");
        btnHelp.setToolTipText("Xem hướng dẫn thao tác (UX-17)");

        topBar.add(new JLabel("Tìm kiếm:"));
        topBar.add(txtSearch);
        topBar.add(btnSearch);
        topBar.add(Box.createHorizontalStrut(8));
        topBar.add(btnDetail);
        topBar.add(btnAdd);
        topBar.add(btnEdit);
        topBar.add(btnDailyPass);
        topBar.add(btnUpgrade);
        topBar.add(btnExport);
        topBar.add(btnHelp);

        add(topBar, BorderLayout.NORTH);
        add(dataTable, BorderLayout.CENTER);

        // Sự kiện các nút
        btnSearch.addActionListener(e -> applyFilter());
        txtSearch.addActionListener(e -> applyFilter());
        btnDetail.addActionListener(e -> handleOpenDetail());
        btnAdd.addActionListener(e -> handleAdd());
        btnEdit.addActionListener(e -> handleEdit());
        btnDailyPass.addActionListener(e -> handleDailyPass());
        btnUpgrade.addActionListener(e -> handleUpgradeDailyPass());
        btnExport.addActionListener(e -> handleExportCsv());
        btnHelp.addActionListener(e -> showHelpDialog());

        setupKeyboardShortcuts();
        applyFilter();
    }

    /** UX-12: Phím tắt Ctrl+F, Ctrl+N, Ctrl+E, F5 */
    private void setupKeyboardShortcuts() {
        InputMap im = getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        ActionMap am = getActionMap();

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_F, KeyEvent.CTRL_DOWN_MASK), "focusSearch");
        am.put("focusSearch", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                txtSearch.requestFocusInWindow();
                txtSearch.selectAll();
            }
        });

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_N, KeyEvent.CTRL_DOWN_MASK), "addMember");
        am.put("addMember", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                handleAdd();
            }
        });

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_E, KeyEvent.CTRL_DOWN_MASK), "exportCsv");
        am.put("exportCsv", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                handleExportCsv();
            }
        });

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0), "reloadTable");
        am.put("reloadTable", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                dataTable.reload();
            }
        });
    }

    private void applyFilter() {
        String keyword = txtSearch.getText().trim();
        dataTable.setLoader((page, size) -> memberService.searchMembers(keyword, page, size));
        dataTable.reload();
    }

    private void handleOpenDetail() {
        Member selected = dataTable.getSelected();
        if (selected == null) {
            Dialogs.warn(this, "Vui lòng chọn một hội viên để xem hồ sơ.");
            return;
        }
        openMemberDetail(selected);
    }

    private void handleAdd() {
        MemberFormDialog dlg = new MemberFormDialog(SwingUtilities.getWindowAncestor(this), memberService, null);
        dlg.setVisible(true);
        if (dlg.isSaved()) dataTable.reload();
    }

    private void handleEdit() {
        Member selected = dataTable.getSelected();
        if (selected == null) {
            Dialogs.warn(this, "Vui lòng chọn một hội viên cần sửa.");
            return;
        }
        MemberFormDialog dlg = new MemberFormDialog(SwingUtilities.getWindowAncestor(this), memberService, selected);
        dlg.setVisible(true);
        if (dlg.isSaved()) dataTable.reload();
    }

    private void handleDailyPass() {
        DailyPassDialog dlg = new DailyPassDialog(SwingUtilities.getWindowAncestor(this), memberService);
        dlg.setVisible(true);
    }

    private void handleUpgradeDailyPass() {
        String idStr = JOptionPane.showInputDialog(this, "Nhập Mã vé ngày (Visitor ID) cần nâng cấp:", "Nâng cấp vé ngày", JOptionPane.QUESTION_MESSAGE);
        if (idStr == null || idStr.trim().isEmpty()) return;

        try {
            int visitorId = Integer.parseInt(idStr.trim());
            String email = JOptionPane.showInputDialog(this, "Nhập Email hội viên (tùy chọn):", "Email", JOptionPane.PLAIN_MESSAGE);

            String confirmMsg = """
                    HỆ QUẢ KHI NÂNG CẤP VÉ NGÀY:
                    - Tất toán bản ghi vé ngày của khách #%d.
                    - Chuyển thông tin khách vãng lai sang hồ sơ Hội viên chính thức.
                    - Khách hàng đủ điều kiện đăng ký gói tập và lịch tập mới.
                    
                    Bạn có muốn tiếp tục nâng cấp?""".formatted(visitorId);

            if (Dialogs.confirm(this, confirmMsg)) {
                Member created = memberService.upgradeDailyPassToMember(visitorId, email, null);
                Dialogs.info(this, "Nâng cấp thành công! Mã hội viên mới: #" + created.getVisitorId());
                dataTable.reload();
            }
        } catch (NumberFormatException e) {
            Dialogs.error(this, "Mã vé ngày phải là dạng số.");
        } catch (RuntimeException ex) {
            Dialogs.error(this, ex);
        }
    }

    @SuppressWarnings("unchecked")
    private void handleExportCsv() {
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File("Danh_Sach_Hoi_Vien.csv"));
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                var pageResult = memberService.searchMembers(txtSearch.getText().trim(), 1, 1000);
                List<Member> list = List.of();
                try {
                    var method = pageResult.getClass().getMethod("items");
                    list = (List<Member>) method.invoke(pageResult);
                } catch (ReflectiveOperationException ignored) {
                    try {
                        var method = pageResult.getClass().getMethod("getItems");
                        list = (List<Member>) method.invoke(pageResult);
                    } catch (ReflectiveOperationException ignored2) {
                        // Giữ nguyên list rỗng nếu không đọc được
                    }
                }

                byte[] data = memberService.exportMembersToCsv(list);
                try (FileOutputStream fos = new FileOutputStream(chooser.getSelectedFile())) {
                    fos.write(data);
                }
                Dialogs.info(this, "Đã xuất tệp CSV thành công.");
            } catch (java.io.IOException | RuntimeException ex) {
            Dialogs.error(this, ex);
            }
        }
    }

    /** UX-17: Hướng dẫn ngắn đúng 3 dòng */
    private void showHelpDialog() {
        String msg = """
                Màn hình quản lý danh sách và hồ sơ toàn bộ hội viên phòng gym.
                - Tìm kiếm nhanh bằng họ tên hoặc số điện thoại (phím tắt: Ctrl+F).
                - Nhấn [+ Thêm hội viên] hoặc dùng Ctrl+N để tạo hồ sơ khách hàng mới.
                - Nhấp đúp vào một dòng để xem chi tiết 5 tab thông tin, lịch sử và gói tập.
                """;
        JOptionPane.showMessageDialog(this, msg.strip(), "Hướng dẫn màn hình (UX-17)", JOptionPane.INFORMATION_MESSAGE);
    }

    private void openMemberDetail(Member member) {
        MemberDetailDialog dlg = new MemberDetailDialog(SwingUtilities.getWindowAncestor(this), memberService, member);
        dlg.setVisible(true);
        if (dlg.isDeleted()) dataTable.reload();
    }
}