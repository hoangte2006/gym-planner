package com.gym.core.ui;

import com.github.lgooddatepicker.components.DatePicker;
import com.github.lgooddatepicker.components.DatePickerSettings;
import com.github.lgooddatepicker.components.DateTimePicker;
import com.github.lgooddatepicker.components.TimePickerSettings;

import java.time.LocalTime;

/** Tạo ô chọn ngày / ngày giờ đã cấu hình sẵn: dd/MM/yyyy, giờ HH:mm bước 15 phút, 06:00–22:00. */
public final class DatePickers {

    private DatePickers() {
    }

    public static DatePicker date() {
        DatePickerSettings settings = new DatePickerSettings();
        settings.setFormatForDatesCommonEra("dd/MM/yyyy");
        settings.setAllowEmptyDates(true);
        return new DatePicker(settings);
    }

    public static DateTimePicker dateTime() {
        DatePickerSettings dateSettings = new DatePickerSettings();
        dateSettings.setFormatForDatesCommonEra("dd/MM/yyyy");
        TimePickerSettings timeSettings = new TimePickerSettings();
        timeSettings.setFormatForDisplayTime("HH:mm");
        timeSettings.setFormatForMenuTimes("HH:mm");
        timeSettings.generatePotentialMenuTimes(TimePickerSettings.TimeIncrement.FifteenMinutes,
                LocalTime.of(6, 0), LocalTime.of(22, 0));
        return new DateTimePicker(dateSettings, timeSettings);
    }
}
