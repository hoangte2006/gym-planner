package com.gym.dashboard.ui;

import com.github.lgooddatepicker.components.DatePicker;
import com.gym.core.exception.BusinessException;
import com.gym.core.ui.DatePickers;
import com.gym.core.ui.Dialogs;
import com.gym.dashboard.model.DateRange;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.FlowLayout;
import java.time.LocalDate;
import java.util.function.Consumer;

/**
 * Thanh chọn khoảng thời gian: một ô chọn nhanh (3/6/12 tháng gần nhất, năm nay, tùy chọn) và hai ô chọn ngày.
 * Chọn nhanh thì hai ô ngày tự điền; tự sửa ngày thì ô chọn nhanh chuyển sang "Tùy chọn". Mỗi lần khoảng thay đổi
 * hợp lệ, thanh gọi onChange(khoảng mới). Khoảng không hợp lệ (thiếu ngày, ngày đầu sau ngày cuối) thì báo lỗi
 * và KHÔNG gọi onChange. Thanh không tự gọi onChange lúc vừa tạo; nơi dùng tự lấy khoảng ban đầu bằng getRange().
 */
public class DateRangeBar extends JPanel {

    private static final long serialVersionUID = 1L;

    /** Các cách chọn nhanh. Mục CUSTOM nghĩa là người dùng tự chọn ngày. */
    enum Preset {
        LAST_3("3 tháng gần nhất"),
        LAST_6("6 tháng gần nhất"),
        LAST_12("12 tháng gần nhất"),
        THIS_YEAR("Năm nay"),
        CUSTOM("Tùy chọn");

        private final String label;

        Preset(String label) {
            this.label = label;
        }

        @Override
        public String toString() {
            return label;               // JComboBox hiện chữ này
        }

        /** Khoảng ứng với lựa chọn nhanh tính từ "today"; null với CUSTOM (không có khoảng cố định). */
        DateRange rangeFor(LocalDate today) {
            return switch (this) {
                case LAST_3 -> DateRange.lastMonths(today, 3);
                case LAST_6 -> DateRange.lastMonths(today, 6);
                case LAST_12 -> DateRange.lastMonths(today, 12);
                case THIS_YEAR -> DateRange.thisYear(today);
                case CUSTOM -> null;
            };
        }
    }

    private final JComboBox<Preset> presetBox = new JComboBox<>(Preset.values());
    private final DatePicker fromPicker = DatePickers.date();
    private final DatePicker toPicker = DatePickers.date();
    private final Consumer<DateRange> onChange;
    private boolean updating;           // true khi chương trình tự điền ô, để các sự kiện đó không bị coi là người dùng sửa

    public DateRangeBar(Consumer<DateRange> onChange) {
        super(new FlowLayout(FlowLayout.LEFT, 8, 0));
        this.onChange = onChange;
        add(new JLabel("Khoảng thời gian:"));
        add(presetBox);
        add(new JLabel("Từ"));
        add(fromPicker);
        add(new JLabel("đến"));
        add(toPicker);

        applyPreset(Preset.LAST_12, false);                         // mặc định 12 tháng gần nhất, chưa báo ai
        presetBox.addActionListener(e -> {
            Preset preset = (Preset) presetBox.getSelectedItem();
            if (!updating && preset != Preset.CUSTOM) {
                applyPreset(preset, true);
            }
        });
        fromPicker.addDateChangeListener(e -> onPickerEdited());
        toPicker.addDateChangeListener(e -> onPickerEdited());
    }

    /** Khoảng đang hiện trên hai ô ngày. Ném BusinessException nếu thiếu ngày hoặc ngày đầu sau ngày cuối. */
    public DateRange getRange() {
        return new DateRange(fromPicker.getDate(), toPicker.getDate());
    }

    private void applyPreset(Preset preset, boolean notify) {
        DateRange range = preset.rangeFor(LocalDate.now());
        updating = true;
        try {
            presetBox.setSelectedItem(preset);
            fromPicker.setDate(range.from());
            toPicker.setDate(range.to());
        } finally {
            updating = false;
        }
        if (notify) {
            onChange.accept(range);
        }
    }

    /** Người dùng tự sửa một ô ngày: chuyển ô chọn nhanh sang "Tùy chọn" rồi báo khoảng mới nếu hợp lệ. */
    private void onPickerEdited() {
        if (updating) {
            return;
        }
        updating = true;
        try {
            presetBox.setSelectedItem(Preset.CUSTOM);
        } finally {
            updating = false;
        }
        try {
            onChange.accept(getRange());
        } catch (BusinessException ex) {
            Dialogs.warn(this, ex.getMessage());                  // khoảng chưa hợp lệ: nhắc người dùng, không nạp lại
        }
    }
}
