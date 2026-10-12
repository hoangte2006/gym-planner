package com.gym.core.ui;

import javax.swing.JRootPane;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import java.awt.Component;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

/**
 * Chạy việc nặng (gọi Service, truy vấn DB) ở luồng nền để giao diện không bị đứng,
 * trong lúc đó hiện lớp phủ "Đang xử lý...". Kết quả và lỗi được trả về trên luồng giao diện.
 *
 * <pre>
 * Async.run(this, () -> memberService.search(keyword),
 *           page -> table.setItems(page.items()));        // lỗi tự hiện hộp thoại tiếng Việt
 * </pre>
 */
public final class Async {

    private Async() {
    }

    /** Chạy work ở nền; thành công thì gọi onSuccess; lỗi thì hiện hộp thoại tiếng Việt. */
    public static <T> void run(Component owner, Callable<T> work, Consumer<T> onSuccess) {
        run(owner, work, onSuccess, error -> Dialogs.error(owner, error));
    }

    public static <T> void run(Component owner, Callable<T> work, Consumer<T> onSuccess, Consumer<Throwable> onError) {
        JRootPane root = owner == null ? null : SwingUtilities.getRootPane(owner);
        Component oldGlass = root == null ? null : root.getGlassPane();
        if (root != null) {
            LoadingOverlay overlay = new LoadingOverlay("Đang xử lý...");
            root.setGlassPane(overlay);
            overlay.setVisible(true);
        }

        new SwingWorker<T, Void>() {
            @Override
            protected T doInBackground() throws Exception {
                return work.call();
            }

            @Override
            protected void done() {
                if (root != null) {
                    root.getGlassPane().setVisible(false);
                    root.setGlassPane(oldGlass);
                }
                try {
                    onSuccess.accept(get());
                } catch (ExecutionException e) {
                    onError.accept(e.getCause() == null ? e : e.getCause());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    onError.accept(e);
                }
            }
        }.execute();
    }
}
