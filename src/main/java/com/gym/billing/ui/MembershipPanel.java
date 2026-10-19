package com.gym.billing.ui;

import com.gym.billing.model.CancellationResult;
import com.gym.billing.model.ClassImpact;
import com.gym.billing.model.MembershipView;
import com.gym.billing.service.BillingService;
import com.gym.core.DateTimeUtils;
import com.gym.core.model.MembershipStatus;
import com.gym.core.ui.Async;
import com.gym.core.ui.DataTable;
import com.gym.core.ui.Dialogs;
import com.gym.core.ui.Toast;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.List;

/** Theo dõi các gói đã bán và hủy gói có kiểm tra lịch lớp tương lai. */
public class MembershipPanel extends JPanel {
    private enum StatusFilter {
        ACTIVE("Đang hiệu lực", MembershipStatus.ACTIVE), ALL("Tất cả", null),
        EXPIRED("Hết hạn", MembershipStatus.EXPIRED), CANCELLED("Đã hủy", MembershipStatus.CANCELLED);
        final String label; final MembershipStatus value;
        StatusFilter(String label, MembershipStatus value) { this.label = label; this.value = value; }
        @Override public String toString() { return label; }
    }

    private final BillingService service = new BillingService();
    private final DataTable<MembershipView> table = new DataTable<>(20);
    private final JTextField keyword = new JTextField(16);
    private final JComboBox<StatusFilter> filter = new JComboBox<>(StatusFilter.values());
    private final JButton cancelButton = new JButton("Hủy gói đã chọn");

    public MembershipPanel() {
        super(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        top.add(new JLabel("Tìm tên/SĐT/gói:")); top.add(keyword);
        top.add(new JLabel("Trạng thái:")); top.add(filter);
        JButton reload = new JButton("Tìm / Tải lại");
        reload.addActionListener(e -> table.goToPage(1));
        keyword.addActionListener(e -> table.goToPage(1));
        top.add(reload);

        table.addColumn("Mã gói", MembershipView::membershipId, 70)
                .addColumn("Mã HV", MembershipView::visitorId, 65)
                .addColumn("Hội viên", MembershipView::memberName, 150)
                .addColumn("SĐT", MembershipView::phone, 100)
                .addColumn("Gói", MembershipView::planName, 140)
                .addColumn("Từ ngày", m -> DateTimeUtils.format(m.startDate()), 105)
                .addColumn("Đến ngày", m -> DateTimeUtils.format(m.endDate()), 105)
                .addColumn("Trạng thái", m -> m.status().getLabel(), 115)
                .setLoader((page, size) -> service.findMemberships(page, size, keyword.getText(), selectedStatus()))
                .onSelectionChanged(row -> cancelButton.setEnabled(row != null && row.status() == MembershipStatus.ACTIVE));

        cancelButton.setEnabled(false);
        cancelButton.addActionListener(e -> beginCancellation());
        JButton refresh = new JButton("Cập nhật trạng thái hết hạn");
        refresh.addActionListener(e -> Async.run(this, () -> service.expirePastMemberships(java.time.LocalDate.now()), n -> {
            Toast.show(this, "Đã cập nhật " + n + " gói hết hạn."); table.reload();
        }));
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottom.add(refresh); bottom.add(cancelButton);

        add(top, BorderLayout.NORTH);
        add(table, BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);
        table.reload();
    }

    private MembershipStatus selectedStatus() {
        StatusFilter selected = (StatusFilter) filter.getSelectedItem();
        return selected == null ? MembershipStatus.ACTIVE : selected.value;
    }

    private void beginCancellation() {
        MembershipView selected = table.getSelected();
        if (selected == null || selected.status() != MembershipStatus.ACTIVE) return;
        Async.run(this, () -> service.previewCancellation(selected.membershipId()), preview -> {
            String warning = "Hủy gói #" + selected.membershipId() + " của " + selected.memberName() + "?\n"
                    + "Hóa đơn hiện tại sẽ được giữ nguyên; hệ thống không tự hoàn tiền.\n"
                    + "Đăng ký lớp tương lai không tự bị hủy, lễ tân cần xử lý riêng.\n\n"
                    + formatImpacts(preview.futureClasses());
            if (!Dialogs.confirm(this, warning)) return;
            Async.run(this, () -> service.cancelMembership(selected.membershipId()), this::showCancellationResult);
        });
    }

    private void showCancellationResult(CancellationResult result) {
        table.reload();
        String message = "Đã hủy gói #" + result.membershipId() + " của " + result.memberName() + ".\n\n"
                + "Các đăng ký lớp cần kiểm tra:\n" + formatImpacts(result.futureClasses())
                + "\n\nHãy xử lý các đăng ký này ở module Lịch tập; hóa đơn được giữ nguyên và không tự hoàn tiền.";
        Dialogs.info(this, message);
    }

    private String formatImpacts(List<ClassImpact> impacts) {
        if (impacts == null || impacts.isEmpty()) return "Không có đăng ký lớp nào trong tương lai.";
        List<String> lines = new ArrayList<>();
        int displayCount = Math.min(12, impacts.size());
        for (int i = 0; i < displayCount; i++) {
            ClassImpact c = impacts.get(i);
            lines.add("• #" + c.sessionId() + " " + c.className() + " — "
                    + DateTimeUtils.format(c.startTime()) + "–" + DateTimeUtils.formatTime(c.endTime())
                    + " (" + c.registrationStatus() + ")");
        }
        if (impacts.size() > displayCount) lines.add("… và " + (impacts.size() - displayCount) + " lớp khác.");
        return String.join("\n", lines);
    }
}
