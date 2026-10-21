package com.gym.billing.ui;

import com.gym.billing.service.PlanService;
import com.gym.core.model.MembershipPlan;
import com.gym.core.ui.Async;
import com.gym.core.ui.DataTable;
import com.gym.core.ui.FormPanel;
import com.gym.core.ui.Toast;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

/** Màn hình bảng giá gói tập: danh sách DB thật, thêm và sửa. */
public class PlanPanel extends JPanel {

    private final PlanService service = new PlanService();
    private final DataTable<MembershipPlan> table = new DataTable<>(15);
    private final FormPanel form = new FormPanel();
    private final JLabel modeLabel = new JLabel("Thêm gói tập mới");
    private Integer editingPlanId;

    public PlanPanel() {
        super(new BorderLayout(12, 12));
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        table.addColumn("Mã", MembershipPlan::getPlanId, 55)
                .addColumn("Tên gói", MembershipPlan::getPlanName, 180)
                .addColumn("Thời hạn (tháng)", MembershipPlan::getDurationMonths, 115)
                .addColumn("Giá", p -> formatPrice(p.getPrice()), 120)
                .addColumn("Mô tả", MembershipPlan::getDescription)
                .setLoader(service::findPage)
                .onDoubleClick(this::fillForm);

        form.addText("name", "Tên gói", true);
        form.addText("duration", "Thời hạn (tháng)", true);
        form.addText("price", "Giá (VNĐ)", true);
        form.addTextArea("description", "Mô tả", false, 4);

        JPanel editor = new JPanel(new BorderLayout(8, 8));
        editor.setBorder(BorderFactory.createTitledBorder("Thông tin gói tập"));
        editor.setPreferredSize(new Dimension(390, 0));
        editor.add(modeLabel, BorderLayout.NORTH);
        editor.add(form, BorderLayout.CENTER);

        JButton addButton = new JButton("Thêm mới");
        JButton saveButton = new JButton("Lưu");
        JButton clearButton = new JButton("Làm mới form");
        addButton.addActionListener(e -> resetForm());
        saveButton.addActionListener(e -> save());
        clearButton.addActionListener(e -> resetForm());

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        actions.add(addButton);
        actions.add(clearButton);
        actions.add(saveButton);
        editor.add(actions, BorderLayout.SOUTH);

        add(new JLabel("Gói tập & Thanh toán  /  Bảng giá gói tập — nhấp đúp một dòng để sửa"), BorderLayout.NORTH);
        add(table, BorderLayout.CENTER);
        add(editor, BorderLayout.EAST);
        table.reload();
    }

    private void fillForm(MembershipPlan plan) {
        editingPlanId = plan.getPlanId();
        modeLabel.setText("Đang sửa gói #" + editingPlanId);
        form.setText("name", plan.getPlanName());
        form.setText("duration", String.valueOf(plan.getDurationMonths()));
        form.setText("price", plan.getPrice() == null ? "" : plan.getPrice().toPlainString());
        form.setText("description", plan.getDescription());
    }

    private void save() {
        String name = form.getText("name");
        String duration = form.getText("duration");
        String price = form.getText("price");
        String description = form.getText("description");
        Integer targetId = editingPlanId;

        Async.run(this, () -> targetId == null
                        ? service.create(name, duration, price, description)
                        : service.update(targetId, name, duration, price, description),
                saved -> {
                    Toast.show(this, targetId == null ? "Đã thêm gói tập." : "Đã cập nhật gói tập.");
                    resetForm();
                    table.reload();
                });
    }

    private void resetForm() {
        editingPlanId = null;
        modeLabel.setText("Thêm gói tập mới");
        form.clear();
    }

    private static String formatPrice(BigDecimal price) {
        if (price == null) return "";
        NumberFormat format = NumberFormat.getNumberInstance(Locale.forLanguageTag("vi-VN"));
        format.setMinimumFractionDigits(0);
        format.setMaximumFractionDigits(2);
        return format.format(price) + " đ";
    }
}
