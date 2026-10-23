package com.gym.dashboard.ui;

import com.github.lgooddatepicker.components.DatePicker;
import com.gym.dashboard.model.DateRange;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.swing.JComboBox;
import java.awt.Component;
import java.awt.Container;
import java.awt.GraphicsEnvironment;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

/** Thanh chọn khoảng thời gian: chọn nhanh điền ngày, tự sửa ngày thì chuyển sang Tùy chọn, và mỗi thay đổi chỉ báo một lần. */
class DateRangeBarTest {

    @BeforeAll
    static void requireDisplay() {
        assumeFalse(GraphicsEnvironment.isHeadless(), "Cần môi trường có màn hình để dựng thành phần Swing.");
    }

    private final List<DateRange> notified = new ArrayList<>();
    private final DateRangeBar bar = new DateRangeBar(notified::add);

    @SuppressWarnings("unchecked")
    private JComboBox<DateRangeBar.Preset> presetBox() {
        return (JComboBox<DateRangeBar.Preset>) find(bar, JComboBox.class).get(0);
    }

    private List<DatePicker> pickers() {
        return find(bar, DatePicker.class);
    }

    private static <T> List<T> find(Container root, Class<T> type) {
        List<T> found = new ArrayList<>();
        for (Component c : root.getComponents()) {
            if (type.isInstance(c)) {
                found.add(type.cast(c));
            }
            if (c instanceof Container inner) {
                found.addAll(find(inner, type));
            }
        }
        return found;
    }

    @Test
    void startsWithTwelveMonthsAndDoesNotNotifyOnCreation() {
        assertEquals(DateRangeBar.Preset.LAST_12, presetBox().getSelectedItem());
        assertEquals(DateRange.lastMonths(LocalDate.now(), 12), bar.getRange());
        assertEquals(0, notified.size(), "Vừa tạo thì chưa báo ai");
    }

    @Test
    void choosingAPresetFillsTheDatesAndNotifiesExactlyOnce() {
        presetBox().setSelectedItem(DateRangeBar.Preset.LAST_3);
        DateRange expected = DateRange.lastMonths(LocalDate.now(), 3);
        assertEquals(expected, bar.getRange());
        assertEquals(List.of(expected), notified, "Chỉ một thông báo, không bị gọi lặp khi tự điền hai ô ngày");
    }

    @Test
    void editingADateSwitchesToCustomAndNotifies() {
        DatePicker from = pickers().get(0);
        DatePicker to = pickers().get(1);
        LocalDate keptTo = to.getDate();

        from.setDate(LocalDate.of(2026, 1, 1));

        assertEquals(DateRangeBar.Preset.CUSTOM, presetBox().getSelectedItem());
        assertEquals(List.of(new DateRange(LocalDate.of(2026, 1, 1), keptTo)), notified);
    }

    @Test
    void thisYearPresetStartsOnJanuaryFirst() {
        presetBox().setSelectedItem(DateRangeBar.Preset.THIS_YEAR);
        assertEquals(LocalDate.now().withDayOfYear(1), bar.getRange().from());
        assertEquals(LocalDate.now(), bar.getRange().to());
    }
}
