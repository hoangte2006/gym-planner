package com.gym.billing.ui;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Image;
import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import com.gym.billing.model.MemberChoice;
import com.gym.billing.model.SalePreview;
import com.gym.billing.service.BillingService;
import com.gym.core.DateTimeUtils;
import com.gym.core.model.MembershipPlan;
import com.gym.core.ui.Async;
import com.gym.core.ui.Dialogs;

/**
 * Wizard 3 bước bán mới/gia hạn: chọn hội viên → chọn gói và xem ngày → xác
 * nhận giao dịch.
 */
public class SellWizardPanel extends JPanel {

    private enum Mode {
        SELL("Bán gói mới", false), RENEW("Gia hạn gói", true);
        final String label;
        final boolean renewal;

        Mode(String label, boolean renewal) {
            this.label = label;
            this.renewal = renewal;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    private final BillingService service = new BillingService();
    private final CardLayout cardLayout = new CardLayout();
    private final JPanel cards = new JPanel(cardLayout);
    private final JLabel stepLabel = new JLabel("BƯỚC 1/3 · Chọn hội viên");
    private final JTextField search = new JTextField(22);
    private final JComboBox<MemberChoice> members = new JComboBox<>();
    private final JLabel photo = new JLabel("Chưa chọn hội viên", JLabel.CENTER);
    private final JLabel memberInfo = new JLabel("Tìm theo tên hoặc số điện thoại.");
    private final JButton nextMember = new JButton("Tiếp theo →");
    private final JButton helpPhoto = new JButton("Thêm ảnh ngay");
    private final JComboBox<Mode> mode = new JComboBox<>(Mode.values());
    private final JComboBox<MembershipPlan> plans = new JComboBox<>();
    private final JLabel preview = new JLabel("Chọn loại giao dịch và gói tập, sau đó bấm Xem xác nhận.");
    private final JLabel summary = new JLabel(" ");
    private final JButton confirm = new JButton("Xác nhận và tạo hóa đơn");
    private boolean loadingMembers;
    private MemberChoice selectedMember;
    private SalePreview currentPreview;

    public SellWizardPanel() {
        super(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        stepLabel.setFont(stepLabel.getFont().deriveFont(Font.BOLD, 15f));
        add(stepLabel, BorderLayout.NORTH);
        cards.add(buildMemberStep(), "member");
        cards.add(buildPlanStep(), "plan");
        cards.add(buildConfirmStep(), "confirm");
        add(cards, BorderLayout.CENTER);
        members.setRenderer((list, value, index, selected, focus) -> {
            String display = value == null ? "— Chọn hội viên —"
                    : "#" + value.visitorId() + " · " + value.displayName() + " · " + value.phone()
                    + " · " + (value.hasPhoto() ? "Có ảnh" : "THIẾU ẢNH")
                    + " · " + (value.activePlanName() == null ? "Chưa có gói Active"
                    : "Đang dùng " + value.activePlanName() + " đến " + DateTimeUtils.format(value.activeEndDate()));
            JLabel label = new JLabel(display);
            label.setOpaque(true);
            label.setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 6));
            if (selected) {
                label.setBackground(list.getSelectionBackground());
                label.setForeground(list.getSelectionForeground());
            }
            if (value != null && !value.hasPhoto()) {
                label.setForeground(selected ? list.getSelectionForeground() : new java.awt.Color(0xB71C1C));
            }
            return label;
        });
        plans.setRenderer((list, value, index, selected, focus) -> {
            JLabel label = new JLabel(value == null ? "— Chọn gói —"
                    : value.getPlanName() + " · " + value.getDurationMonths() + " tháng · " + BillingFormat.money(value.getPrice()));
            label.setOpaque(true);
            label.setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 6));
            if (selected) {
                label.setBackground(list.getSelectionBackground());
                label.setForeground(list.getSelectionForeground());
            }
            return label;
        });
        members.addActionListener(e -> {
            if (!loadingMembers) {
                updateSelectedMember();

            }
        });
        helpPhoto.addActionListener(e -> Dialogs.info(this,
                "Hội viên bắt buộc phải có ảnh để mua/gia hạn gói.\nHãy mở module Hội viên trong menu, tìm hội viên này, chọn Sửa/Thêm ảnh, lưu ảnh JPG/PNG rồi quay lại đây và bấm Tải lại."));
        nextMember.setEnabled(false);
        nextMember.addActionListener(e -> goToPlanStep());
        mode.addActionListener(e -> {
            preview.setText("Đã đổi loại giao dịch. Bấm Xem xác nhận để tính lại ngày.");
            currentPreview = null;
        });
        plans.addActionListener(e -> {
            currentPreview = null;
        });
        JButton reload = new JButton("Tải lại danh sách");
        reload.addActionListener(e -> loadMembers());
    }
    private boolean initialized = false;

    public void initialize() {
        if (initialized) {
            return;
        }

        initialized = true;

        loadMembers();

        Async.run(
                this,
                () -> service.findPlans(),
                this::populatePlans
        );
    }

    private JPanel buildMemberStep() {
        JPanel root = new JPanel(new BorderLayout(12, 12));
        JPanel searchRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
        searchRow.add(new JLabel("Tìm tên / SĐT / mã hội viên:"));
        searchRow.add(search);
        JButton find = new JButton("Tìm");
        find.addActionListener(e -> loadMembers());
        search.addActionListener(e -> loadMembers());
        JButton reload = new JButton("Tải lại");
        reload.addActionListener(e -> loadMembers());
        searchRow.add(find);
        searchRow.add(reload);
        JPanel selector = new JPanel();
        selector.setLayout(new BoxLayout(selector, BoxLayout.Y_AXIS));
        selector.setBorder(BorderFactory.createTitledBorder("1. Chọn hội viên (ảnh bắt buộc)"));
        selector.add(members);
        selector.add(Box.createVerticalStrut(10));
        memberInfo.setVerticalAlignment(JLabel.TOP);
        memberInfo.setPreferredSize(new Dimension(520, 90));
        selector.add(memberInfo);
        selector.add(Box.createVerticalStrut(8));
        selector.add(helpPhoto);
        selector.add(Box.createVerticalGlue());
        selector.add(nextMember);
        photo.setPreferredSize(new Dimension(165, 210));
        photo.setBorder(BorderFactory.createEtchedBorder());
        JPanel photoPanel = new JPanel(new BorderLayout());
        photoPanel.setBorder(BorderFactory.createTitledBorder("Ảnh hồ sơ"));
        photoPanel.add(photo, BorderLayout.CENTER);
        root.add(searchRow, BorderLayout.NORTH);
        root.add(selector, BorderLayout.CENTER);
        root.add(photoPanel, BorderLayout.EAST);
        return root;
    }

    private JPanel buildPlanStep() {
        JPanel root = new JPanel(new BorderLayout(12, 12));
        JPanel fields = new JPanel();
        fields.setLayout(new BoxLayout(fields, BoxLayout.Y_AXIS));
        fields.setBorder(BorderFactory.createTitledBorder("2. Chọn thao tác và gói tập"));
        fields.add(new JLabel("Loại giao dịch"));
        fields.add(mode);
        fields.add(Box.createVerticalStrut(12));
        fields.add(new JLabel("Bảng giá gói tập"));
        fields.add(plans);
        fields.add(Box.createVerticalStrut(12));
        preview.setVerticalAlignment(JLabel.TOP);
        fields.add(preview);
        fields.add(Box.createVerticalGlue());
        JButton back = new JButton("← Quay lại");
        back.addActionListener(e -> showStep("member"));
        JButton next = new JButton("Xem xác nhận →");
        next.addActionListener(e -> calculatePreview());
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        actions.add(back);
        actions.add(next);
        root.add(fields, BorderLayout.CENTER);
        root.add(actions, BorderLayout.SOUTH);
        return root;
    }

    private JPanel buildConfirmStep() {
        JPanel root = new JPanel(new BorderLayout(12, 12));
        JPanel details = new JPanel(new BorderLayout());
        details.setBorder(BorderFactory.createTitledBorder("3. Kiểm tra trước khi tạo"));
        summary.setVerticalAlignment(JLabel.TOP);
        summary.setFont(summary.getFont().deriveFont(14f));
        details.add(summary, BorderLayout.NORTH);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton back = new JButton("← Sửa thông tin");
        back.addActionListener(e -> showStep("plan"));
        confirm.addActionListener(e -> submitSale());
        actions.add(back);
        actions.add(confirm);
        root.add(details, BorderLayout.CENTER);
        root.add(actions, BorderLayout.SOUTH);
        return root;
    }

    private void loadMembers() {
        String keyword = search.getText();
        Async.run(this, () -> service.searchMembers(keyword), rows -> {
            loadingMembers = true;
            members.removeAllItems();
            for (MemberChoice member : rows) {
                members.addItem(member);
            }
            loadingMembers = false;
            updateSelectedMember();
            if (rows.isEmpty()) {
                memberInfo.setText("Không tìm thấy hội viên phù hợp. Hãy xóa bớt từ khóa hoặc mở module Hội viên.");
            }
        });
    }

    private void populatePlans(List<MembershipPlan> items) {
        plans.removeAllItems();
        for (MembershipPlan p : items) {
            plans.addItem(p);
        }
    }

    private void updateSelectedMember() {
        selectedMember = (MemberChoice) members.getSelectedItem();
        currentPreview = null;
        photo.setIcon(null);
        if (selectedMember == null) {
            photo.setText("Chưa chọn hội viên");
            memberInfo.setText("Tìm theo tên hoặc số điện thoại.");
            nextMember.setEnabled(false);
            return;
        }
        String activeText = selectedMember.activePlanName() == null ? "Chưa có gói Active."
                : "Gói đang Active: " + selectedMember.activePlanName() + " · đến " + DateTimeUtils.format(selectedMember.activeEndDate());
        String photoText = selectedMember.hasPhoto() ? "Đã có ảnh." : "THIẾU ẢNH — không thể bán/gia hạn.";
        memberInfo.setText("<html><b>" + selectedMember.displayName() + "</b><br>SĐT: " + selectedMember.phone()
                + "<br>" + photoText + "<br>" + activeText + "</html>");
        nextMember.setEnabled(selectedMember.hasPhoto());
        helpPhoto.setEnabled(!selectedMember.hasPhoto());
        if (!selectedMember.hasPhoto()) {
            photo.setText("Chưa có ảnh");
            return;
        }
        photo.setText("Đang tải ảnh...");
        int id = selectedMember.visitorId();
        Async.run(this, () -> service.findMemberPhoto(id), imageBytes -> {
            if (selectedMember == null || selectedMember.visitorId() != id) {
                return;
            }
            if (imageBytes.isEmpty() || imageBytes.get() == null) {
                photo.setText("Ảnh không còn");
                nextMember.setEnabled(false);
                return;
            }
            try {
                var buffered = ImageIO.read(new ByteArrayInputStream(imageBytes.get()));
                if (buffered == null) {
                    photo.setText("Không đọc được ảnh");
                    return;
                }
                Image scaled = buffered.getScaledInstance(140, 175, Image.SCALE_SMOOTH);
                photo.setIcon(new ImageIcon(scaled));
                photo.setText("");
            } catch (java.io.IOException ex) {
                photo.setText("Không đọc được ảnh");
                System.err.println("Lỗi đọc ảnh: " + ex.getMessage());
            }
        });
    }

    private void goToPlanStep() {
        selectedMember = (MemberChoice) members.getSelectedItem();
        if (selectedMember == null) {
            Dialogs.warn(this, "Hãy chọn một hội viên trước.");
            return;
        }
        if (!selectedMember.hasPhoto()) {
            Dialogs.warn(this, "Hội viên chưa có ảnh. Bấm Thêm ảnh ngay để xem hướng dẫn.");
            return;
        }
        preview.setText("Chọn Bán mới hoặc Gia hạn, chọn gói rồi bấm Xem xác nhận.");
        showStep("plan");
    }

    private void calculatePreview() {
        MemberChoice member = (MemberChoice) members.getSelectedItem();
        MembershipPlan plan = (MembershipPlan) plans.getSelectedItem();
        Mode selectedMode = (Mode) mode.getSelectedItem();
        if (member == null || plan == null || selectedMode == null) {
            Dialogs.warn(this, "Hãy chọn hội viên, thao tác và gói tập.");
            return;
        }
        if (!member.hasPhoto()) {
            Dialogs.warn(this, "Hội viên chưa có ảnh. Không thể mua hoặc gia hạn gói.");
            return;
        }
        if (!selectedMode.renewal && member.activePlanName() != null && member.activeEndDate() != null
                && !member.activeEndDate().isBefore(LocalDate.now())) {
            Dialogs.warn(this, "Hội viên đang có gói '" + member.activePlanName() + "' đến "
                    + DateTimeUtils.format(member.activeEndDate()) + ". Hãy chọn Gia hạn gói để không tạo gói Active trùng.");
            return;
        }
        Async.run(this, () -> service.previewSale(member.visitorId(), plan.getPlanId(), selectedMode.renewal, LocalDate.now()), result -> {
            currentPreview = result;
            preview.setText("<html>Ngày bắt đầu dự kiến: <b>" + DateTimeUtils.format(result.startDate())
                    + "</b><br>Ngày kết thúc dự kiến: <b>" + DateTimeUtils.format(result.endDate())
                    + "</b><br>Thời hạn: " + result.durationMonths() + " tháng<br>Giá gói: <b>"
                    + BillingFormat.money(result.price()) + "</b><br>Hóa đơn: Chờ thanh toán · Hạn thanh toán: "
                    + DateTimeUtils.format(LocalDate.now().plusDays(7))
                    + "<br><br><i>Ngày được kiểm tra lại trong transaction lúc xác nhận.</i></html>");
            summary.setText("<html><b>Hội viên:</b> " + member.displayName() + " · " + member.phone()
                    + "<br><b>Thao tác:</b> " + selectedMode.label
                    + "<br><b>Gói:</b> " + result.planName()
                    + "<br><b>Ngày bắt đầu:</b> " + DateTimeUtils.format(result.startDate())
                    + "<br><b>Ngày kết thúc:</b> " + DateTimeUtils.format(result.endDate())
                    + "<br><b>Số tiền phải thu:</b> " + BillingFormat.money(result.price())
                    + "<br><b>Trạng thái hóa đơn mới:</b> Chờ thanh toán"
                    + "<br><b>Hạn thanh toán:</b> " + DateTimeUtils.format(LocalDate.now().plusDays(7))
                    + "<br><br>Nhấn Xác nhận chỉ khi bạn đã kiểm tra đúng hội viên và giá tiền.</html>");
            showStep("confirm");
        });
    }

    private void submitSale() {
        MemberChoice member = (MemberChoice) members.getSelectedItem();
        MembershipPlan plan = (MembershipPlan) plans.getSelectedItem();
        Mode selectedMode = (Mode) mode.getSelectedItem();
        if (member == null || plan == null || selectedMode == null || currentPreview == null) {
            Dialogs.warn(this, "Hãy quay lại kiểm tra thông tin giao dịch.");
            return;
        }
        if (!Dialogs.confirm(this, "Tạo gói " + selectedMode.label.toLowerCase() + " cho " + member.displayName()
                + " với giá " + BillingFormat.money(plan.getPrice()) + "?\nGói mới và hóa đơn Pending được lưu trong cùng transaction.")) {
            return;
        }
        confirm.setEnabled(false);
        Async.run(this, () -> selectedMode.renewal
                ? service.renewPlan(member.visitorId(), plan.getPlanId())
                : service.sellPlan(member.visitorId(), plan.getPlanId()), result -> {
            confirm.setEnabled(true);
            Dialogs.info(this, (result.renewed() ? "Đã gia hạn gói thành công." : "Đã bán gói thành công.")
                    + "\nMã gói mới: " + result.membershipId()
                    + "\nMã hóa đơn: HD-" + result.billId()
                    + "\nNgày bắt đầu: " + DateTimeUtils.format(result.startDate())
                    + "\nNgày kết thúc: " + DateTimeUtils.format(result.endDate())
                    + "\nSố tiền: " + BillingFormat.money(result.planPrice())
                    + "\nHóa đơn đang Chờ thanh toán, hạn " + DateTimeUtils.format(LocalDate.now().plusDays(7)) + ".");
            showStep("member");
            loadMembers();
        }, error -> {
            confirm.setEnabled(true);
            com.gym.core.ui.Dialogs.error(this, error);
        });
    }

    private void showStep(String step) {
        cardLayout.show(cards, step);
        stepLabel.setText(switch (step) {
            case "member" ->
                "BƯỚC 1/3 · Chọn hội viên";
            case "plan" ->
                "BƯỚC 2/3 · Chọn thao tác và gói tập";
            default ->
                "BƯỚC 3/3 · Xác nhận giao dịch";
        });
    }
}
