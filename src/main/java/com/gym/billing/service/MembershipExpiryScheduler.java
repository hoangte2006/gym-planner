package com.gym.billing.service;

import java.time.LocalDate;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Chạy một lần lúc module được nạp và lặp lại mỗi 60 phút trên luồng nền daemon. */
public final class MembershipExpiryScheduler {
    private static final Logger LOGGER = Logger.getLogger(MembershipExpiryScheduler.class.getName());
    private static final AtomicBoolean STARTED = new AtomicBoolean(false);
    private static ScheduledExecutorService executor;

    private MembershipExpiryScheduler() { }

    public static void start(BillingService service) {
        if (!STARTED.compareAndSet(false, true)) return;
        executor = Executors.newSingleThreadScheduledExecutor(task -> {
            Thread t = new Thread(task, "gym-membership-expiry");
            t.setDaemon(true);
            return t;
        });
        executor.scheduleWithFixedDelay(() -> {
            try {
                int count = service.expirePastMemberships(LocalDate.now());
                if (count > 0) LOGGER.info("Đã tự chuyển " + count + " gói tập sang Hết hạn.");
            } catch (RuntimeException ex) {
                // Đừng để một lỗi kết nối làm ScheduledExecutor ngừng chạy vĩnh viễn.
                LOGGER.log(Level.WARNING, "Tác vụ tự hết hạn gói tập thất bại; sẽ thử lại sau 60 phút.", ex);
            }
        }, 0, 60, TimeUnit.MINUTES);
    }
}
