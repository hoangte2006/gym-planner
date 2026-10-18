package com.gym.billing.ui;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

final class BillingFormat {
    private BillingFormat() { }
    static String money(BigDecimal amount) {
        NumberFormat f = NumberFormat.getNumberInstance(Locale.forLanguageTag("vi-VN"));
        f.setMinimumFractionDigits(0);
        f.setMaximumFractionDigits(2);
        return f.format(amount == null ? BigDecimal.ZERO : amount) + " đ";
    }
}
