
package com.gym.facility.ui.common;

import com.gym.core.exception.BusinessException;
import com.gym.core.model.DbEnum;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;
import java.awt.Color;
import java.awt.Component;
import java.math.BigDecimal;

/**
 * Các hàm tiện ích dùng chung cho giao diện Facility.
 */
public final class FacilityUiHelper {

    private FacilityUiHelper() {
        // Không cho khởi tạo đối tượng tiện ích.
    }

    /**
     * Tạo JButton thông thường.
     */
    public static JButton button(String title) {

        JButton button = new JButton(title);

        button.setFocusPainted(false);

        return button;
    }

    /**
     * Tạo JButton dùng cho thao tác nguy hiểm như Xóa.
     */
    public static JButton dangerButton(String title) {

        JButton button = button(title);

        button.setForeground(
                new Color(0xB91C1C)
        );

        return button;
    }

    /**
     * Chuyển chuỗi rỗng hoặc chỉ chứa khoảng trắng thành null.
     */
    public static String blankToNull(String value) {

        return value == null || value.isBlank()
                ? null
                : value.trim();
    }

    /**
     * Chuyển chuỗi tiền thành BigDecimal.
     *
     * Ví dụ:
     * "500000" -> 500000
     * "500,000" -> 500000
     *
     * Không sử dụng double để tránh sai số tiền tệ.
     */
    public static BigDecimal parseMoney(
            String value,
            String label
    ) {

        if (value == null || value.isBlank()) {

            throw new BusinessException(
                    label + " không được để trống."
            );
        }

        try {

            return new BigDecimal(
                    value.trim()
                            .replace(",", "")
                            .replace(" ", "")
            );

        } catch (NumberFormatException ex) {

            throw new BusinessException(
                    label + " phải là số hợp lệ.",
                    ex
            );
        }
    }

    /**
     * Renderer hiển thị label của các enum triển khai DbEnum.
     */
    public static ListCellRenderer<Object> enumRenderer() {

        return new DefaultListCellRenderer() {

            @Override
            public Component getListCellRendererComponent(
                    JList<?> list,
                    Object value,
                    int index,
                    boolean selected,
                    boolean focus
            ) {

                Object displayValue =
                        value instanceof DbEnum dbEnum
                                ? dbEnum.getLabel()
                                : value;

                return super.getListCellRendererComponent(
                        list,
                        displayValue,
                        index,
                        selected,
                        focus
                );
            }
        };
    }

    /**
     * Tạo JLabel có nền và màu chữ theo trạng thái chọn.
     */
    public static JLabel rendererLabel(
            JList<?> list,
            String text,
            boolean selected
    ) {

        JLabel label = new JLabel(text);

        if (selected) {

            label.setOpaque(true);

            label.setBackground(
                    list.getSelectionBackground()
            );

            label.setForeground(
                    list.getSelectionForeground()
            );
        }

        label.setBorder(
                BorderFactory.createEmptyBorder(
                        3, 5, 3, 5
                )
        );

        return label;
    }
}
