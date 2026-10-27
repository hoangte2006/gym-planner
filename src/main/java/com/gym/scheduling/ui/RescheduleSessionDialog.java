package com.gym.scheduling.ui;

import com.gym.core.model.ClassSession;
import com.gym.core.model.PtSession;
import com.gym.core.model.Session;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerDateModel;
import java.awt.GridLayout;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.Optional;

final class RescheduleSessionDialog {

    private RescheduleSessionDialog() {
    }

    static Optional<Session> show(JComponent parent, Session original) {
        JSpinner date = new JSpinner(new SpinnerDateModel());
        date.setEditor(new JSpinner.DateEditor(date, "dd/MM/yyyy"));
        date.setValue(toDate(original.getStartTime().toLocalDate()));

        JComboBox<String> start = new JComboBox<>(times());
        JComboBox<String> end = new JComboBox<>(times());
        start.setSelectedItem(original.getStartTime().toLocalTime().toString());
        end.setSelectedItem(original.getEndTime().toLocalTime().toString());

        JPanel form = new JPanel(new GridLayout(0, 2, 8, 8));
        form.add(new JLabel("Buổi tập:"));
        form.add(new JLabel(original.getSessionTitle()));
        form.add(new JLabel("Ngày mới:"));
        form.add(date);
        form.add(new JLabel("Bắt đầu:"));
        form.add(start);
        form.add(new JLabel("Kết thúc:"));
        form.add(end);

        int answer = JOptionPane.showConfirmDialog(
                parent, form, "Dời lịch buổi tập", JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE);
        if (answer != JOptionPane.OK_OPTION) {
            return Optional.empty();
        }

        LocalDate day = toLocalDate((Date) date.getValue());
        LocalDateTime from = LocalDateTime.of(day, LocalTime.parse((String) start.getSelectedItem()));
        LocalDateTime to = LocalDateTime.of(day, LocalTime.parse((String) end.getSelectedItem()));
        Session changed;
        if (original instanceof ClassSession classSession) {
            ClassSession copy = new ClassSession();
            copy.setClassName(classSession.getClassName());
            copy.setMaxCapacity(classSession.getMaxCapacity());
            changed = copy;
        } else if (original instanceof PtSession ptSession) {
            PtSession copy = new PtSession();
            copy.setMemberId(ptSession.getMemberId());
            copy.setBookingStatus(ptSession.getBookingStatus());
            changed = copy;
        } else {
            throw new IllegalArgumentException("Không hỗ trợ loại buổi tập này.");
        }
        changed.setSessionId(original.getSessionId());
        changed.setSessionType(original.getSessionType());
        changed.setRoomId(original.getRoomId());
        changed.setTrainerId(original.getTrainerId());
        changed.setStartTime(from);
        changed.setEndTime(to);
        return Optional.of(changed);
    }

    private static String[] times() {
        String[] result = new String[33];
        for (int i = 0; i < result.length; i++) {
            result[i] = String.format("%02d:%02d", (i * 30) / 60 + 6, (i * 30) % 60);
        }
        result[result.length - 1] = "22:00";
        return result;
    }

    private static Date toDate(LocalDate date) {
        return Date.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    private static LocalDate toLocalDate(Date date) {
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }
}
