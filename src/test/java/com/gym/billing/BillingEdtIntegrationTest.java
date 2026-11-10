package com.gym.billing;

import com.gym.app.ModuleRegistry;
import com.gym.app.Session;
import com.gym.billing.ui.PlanPanel;
import com.gym.core.DBConnection;
import com.gym.core.model.Manager;
import com.gym.core.model.StaffRole;
import com.gym.core.model.Trainer;
import com.gym.core.ui.Async;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.GraphicsEnvironment;
import java.math.BigDecimal;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * TC28 (sâu): Trainer không có module billing; thao tác tải DB của billing chạy ngoài EDT.
 */
class BillingEdtIntegrationTest {

    @BeforeAll
    static void requireDatabaseAndDisplay() {
        assumeFalse(GraphicsEnvironment.isHeadless(), "Cần màn hình cho kiểm thử EDT/Swing.");
        boolean up;
        try {
            DBConnection.test();
            up = true;
        } catch (RuntimeException e) {
            up = false;
        }
        assumeTrue(up, "Không kết nối được DB, bỏ qua TC28 integration.");
    }

    @AfterEach
    void logout() {
        Session.logout();
    }

    @Test
    void tc28_trainerHasNoBillingModule_andDbLoadStaysOffEdt() throws Exception {
        Trainer trainer = new Trainer();
        trainer.setFullName("TV3 Trainer");
        trainer.setPhone("0900000099");
        trainer.setSalary(new BigDecimal("100"));
        trainer.setRole(StaffRole.TRAINER);
        Session.login(trainer);
        assertTrue(ModuleRegistry.forRole(StaffRole.TRAINER).stream()
                .noneMatch(m -> "billing".equals(m.getId())));
        Session.logout();

        Manager manager = new Manager();
        manager.setFullName("TV3 Manager");
        manager.setPhone("0900000098");
        manager.setSalary(new BigDecimal("100"));
        manager.setRole(StaffRole.MANAGER);
        Session.login(manager);

        CountDownLatch workStarted = new CountDownLatch(1);
        CountDownLatch workFinished = new CountDownLatch(1);
        AtomicBoolean workedOnEdt = new AtomicBoolean(true);
        JPanel owner = new JPanel();

        SwingUtilities.invokeAndWait(() ->
                Async.run(owner, () -> {
                    workedOnEdt.set(SwingUtilities.isEventDispatchThread());
                    workStarted.countDown();
                    DBConnection.test();
                    Thread.sleep(150);
                    return Boolean.TRUE;
                }, ignored -> workFinished.countDown())
        );

        assertTrue(workStarted.await(5, TimeUnit.SECONDS), "Background work không khởi động");
        assertFalse(workedOnEdt.get(), "Tác vụ DB phải chạy ngoài EDT");

        // Trong lúc background còn chạy, EDT vẫn nhận được invokeLater từ luồng test.
        CountDownLatch edtPulse = new CountDownLatch(1);
        long t0 = System.nanoTime();
        SwingUtilities.invokeLater(edtPulse::countDown);
        assertTrue(edtPulse.await(2, TimeUnit.SECONDS), "EDT bị khóa khi tải dữ liệu");
        long edtResponseMs = (System.nanoTime() - t0) / 1_000_000L;
        assertTrue(edtResponseMs < 1000, "EDT phản hồi chậm: " + edtResponseMs + "ms");

        AtomicReference<PlanPanel> panelRef = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> panelRef.set(new PlanPanel()));
        assertNotNull(panelRef.get());

        assertTrue(workFinished.await(5, TimeUnit.SECONDS), "Background work không kết thúc");
    }
}
