package com.gym.core.ui;

import com.github.lgooddatepicker.components.DatePicker;
import com.github.lgooddatepicker.components.DateTimePicker;
import com.gym.core.model.DbEnum;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Biểu mẫu nhập liệu: mỗi dòng là nhãn bên trái và ô nhập bên phải. Mỗi ô có một "khóa" (key)
 * để đọc giá trị ra sau này. Ô bắt buộc có dấu * sau nhãn.
 *
 * <pre>
 * FormPanel form = new FormPanel();
 * form.addText("name", "Tên phòng", true);
 * form.addText("capacity", "Sức chứa", true);
 * form.addDate("date", "Ngày mua", false);
 * ...
 * String name = form.getText("name");        // đã cắt khoảng trắng; null nếu để trống
 * </pre>
 */
public class FormPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    private final Map<String, JComponent> fields = new LinkedHashMap<>();
    private final Map<String, String> labels = new LinkedHashMap<>();
    private int row;

    public FormPanel() {
        super(new GridBagLayout());
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
    }

    // ---------- Thêm ô nhập ----------

    public JTextField addText(String key, String label, boolean required) {
        JTextField field = new JTextField(24);
        return register(key, label, required, field);
    }

    public JTextArea addTextArea(String key, String label, boolean required, int rows) {
        JTextArea area = new JTextArea(rows, 24);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        fields.put(key, area);
        labels.put(key, label);
        addRow(label, required, new JScrollPane(area));
        return area;
    }

    public <E> JComboBox<E> addCombo(String key, String label, boolean required, E[] items) {
        JComboBox<E> combo = new JComboBox<>(items);
        combo.setRenderer(new DefaultListCellRenderer() {
            private static final long serialVersionUID = 1L;

            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean selected, boolean focus) {
                Object shown = value instanceof DbEnum e ? e.getLabel() : value;
                return super.getListCellRendererComponent(list, shown, index, selected, focus);
            }
        });
        return register(key, label, required, combo);
    }

    /** Thêm thành phần tùy biến vào form nhưng vẫn dùng chung quy tắc nhãn/layout. */
    public <C extends JComponent> C addComponent(String key, String label, boolean required, C component) {
        return register(key, label, required, component);
    }

    public DatePicker addDate(String key, String label, boolean required) {
        return register(key, label, required, DatePickers.date());
    }

    public DateTimePicker addDateTime(String key, String label, boolean required) {
        return register(key, label, required, DatePickers.dateTime());
    }

    // ---------- Đọc giá trị ----------

    /** Văn bản đã cắt khoảng trắng hai đầu; null nếu để trống. */
    public String getText(String key) {
        JComponent c = field(key);
        String text = c instanceof JTextField tf ? tf.getText()
                : c instanceof JTextArea ta ? ta.getText() : null;
        return text == null || text.isBlank() ? null : text.trim();
    }

    public LocalDate getDate(String key) {
        return ((DatePicker) field(key)).getDate();
    }

    public LocalDateTime getDateTime(String key) {
        return ((DateTimePicker) field(key)).getDateTimePermissive();
    }

    @SuppressWarnings("unchecked")
    public <E> E getSelected(String key) {
        return (E) ((JComboBox<?>) field(key)).getSelectedItem();
    }

    /** Nhãn của ô (không kèm dấu *), dùng làm tên trường khi báo lỗi với Validator. */
    public String label(String key) {
        return labels.get(key);
    }

    // ---------- Gán giá trị (khi mở form sửa) ----------

    public void setText(String key, String value) {
        JComponent c = field(key);
        String text = value == null ? "" : value;
        if (c instanceof JTextField tf) {
            tf.setText(text);
        } else if (c instanceof JTextArea ta) {
            ta.setText(text);
        }
    }

    public void setDate(String key, LocalDate value) {
        ((DatePicker) field(key)).setDate(value);
    }

    public void setDateTime(String key, LocalDateTime value) {
        ((DateTimePicker) field(key)).setDateTimePermissive(value);
    }

    public void setSelected(String key, Object value) {
        ((JComboBox<?>) field(key)).setSelectedItem(value);
    }

    /** Xóa trắng mọi ô. */
    public void clear() {
        for (Map.Entry<String, JComponent> e : fields.entrySet()) {
            JComponent c = e.getValue();
            if (c instanceof JTextField || c instanceof JTextArea) {
                setText(e.getKey(), "");
            } else if (c instanceof DatePicker dp) {
                dp.clear();
            } else if (c instanceof DateTimePicker dtp) {
                dtp.clear();
            } else if (c instanceof JComboBox<?> cb && cb.getItemCount() > 0) {
                cb.setSelectedIndex(0);
            }
        }
    }

    // ---------- Nội bộ ----------

    private <C extends JComponent> C register(String key, String label, boolean required, C component) {
        fields.put(key, component);
        labels.put(key, label);
        addRow(label, required, component);
        return component;
    }

    private JComponent field(String key) {
        JComponent c = fields.get(key);
        if (c == null) {
            throw new IllegalArgumentException("Biểu mẫu không có ô '" + key + "'.");
        }
        return c;
    }

    private void addRow(String label, boolean required, JComponent input) {
        GridBagConstraints lc = new GridBagConstraints();
        lc.gridx = 0;
        lc.gridy = row;
        lc.anchor = GridBagConstraints.NORTHWEST;
        lc.insets = new Insets(6, 0, 6, 12);
        add(new JLabel(required ? label + " *" : label), lc);

        GridBagConstraints ic = new GridBagConstraints();
        ic.gridx = 1;
        ic.gridy = row;
        ic.weightx = 1;
        ic.fill = GridBagConstraints.HORIZONTAL;
        ic.insets = new Insets(6, 0, 6, 0);
        add(input, ic);
        row++;
    }
}
