package com.gym.core.model;

import com.gym.core.util.Check;
import java.math.BigDecimal;
import java.time.LocalDate;

/** Vé ngày (bảng DAILY_PASS_TICKET, con của VISITOR). */
public class DailyPassTicket extends Visitor {

    private LocalDate visitDate;
    private BigDecimal dailyRate;

    /** Ngày đến. */
    public LocalDate getVisitDate() {
        return visitDate;
    }

    public void setVisitDate(LocalDate visitDate) {
        this.visitDate = visitDate;
    }

    /** Giá vé ngày. */
    public BigDecimal getDailyRate() {
        return dailyRate;
    }

    public void setDailyRate(BigDecimal dailyRate) {
        this.dailyRate = Check.positive(dailyRate, "Giá vé ngày");
    }

    @Override
    public String getDisplayName() {
        return getFullName() + " (Vé ngày)";
    }
}
