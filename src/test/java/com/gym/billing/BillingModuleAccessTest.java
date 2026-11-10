package com.gym.billing;

import com.gym.app.AppModule;
import com.gym.app.ModuleRegistry;
import com.gym.core.model.StaffRole;
import com.gym.core.ui.Async;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.GraphicsEnvironment;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** TC28: Trainer không vào module billing; thao tác nền không khóa EDT. */
class BillingModuleAccessTest {

    @Test
    void tc28_trainerCannotAccessBillingModule() {
        BillingModule module = new BillingModule();
        Set<StaffRole> roles = module.getAllowedRoles();

        assertEquals(Set.of(StaffRole.MANAGER, StaffRole.RECEPTIONIST), roles);
        assertFalse(roles.contains(StaffRole.TRAINER));

        List<AppModule> trainerModules = ModuleRegistry.forRole(StaffRole.TRAINER);
        assertTrue(trainerModules.stream().noneMatch(m -> "billing".equals(m.getId())));
    }

    @Test
    void tc28_asyncWorkDoesNotRunOnEdt() throws Exception {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "Cần display cho kiểm tra Async/EDT");

        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch finished = new CountDownLatch(1);
        AtomicBoolean workedOnEdt = new AtomicBoolean(true);
        JPanel owner = new JPanel();

        SwingUtilities.invokeAndWait(() ->
                Async.run(owner, () -> {
                    workedOnEdt.set(SwingUtilities.isEventDispatchThread());
                    started.countDown();
                    return "ok";
                }, result -> finished.countDown())
        );

        assertTrue(started.await(5, TimeUnit.SECONDS), "Background work did not start");
        assertTrue(finished.await(5, TimeUnit.SECONDS), "Async callback did not finish");
        assertFalse(workedOnEdt.get(), "DB/work must not run on EDT");
    }
}
