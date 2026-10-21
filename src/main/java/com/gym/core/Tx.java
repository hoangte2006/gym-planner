package com.gym.core;

import com.gym.core.exception.DataAccessException;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.function.Supplier;

/**
 * Giao dịch dùng chung cho NHIỀU DAO: các thao tác bên trong cùng thành công hoặc cùng bị hủy.
 *
 * <pre>
 * // Bán vé ngày = tạo khách + vé + hóa đơn. Hóa đơn lỗi thì khách và vé cũng bị hủy.
 * Tx.run(() -> {
 *     passDao.insert(ticket);
 *     billingDao.insert(bill);
 * });
 *
 * DailyPassTicket saved = Tx.call(() -> passDao.insert(ticket));   // có giá trị trả về
 * </pre>
 *
 * Cách hoạt động: Tx mở MỘT kết nối, tắt tự chốt, cất kết nối vào "ngăn riêng của luồng hiện tại".
 * Mọi DAO gọi bên trong đều dùng kết nối đó thay vì tự mở kết nối mới. Xong việc thì chốt (commit),
 * có lỗi thì hủy (rollback).
 *
 * Quy tắc dùng:
 * <ul>
 *   <li>Gọi các DAO ngay bên trong lambda, trên CÙNG luồng. Nếu dùng Async, đặt Tx.run BÊN TRONG việc nền:
 *       {@code Async.run(this, () -> Tx.call(() -> service.sell(...)), ...)}.</li>
 *   <li>KHÔNG bắt lỗi (try/catch) bên trong lambda rồi cho chạy tiếp: lỗi phải bay ra khỏi Tx.run để được hủy.</li>
 *   <li>Tx lồng nhau thì tự gộp vào giao dịch ngoài cùng; chỉ giao dịch ngoài cùng mới chốt hoặc hủy.</li>
 *   <li>Đừng để việc lâu (chờ người dùng bấm nút) bên trong Tx: kết nối bị giữ suốt thời gian đó.</li>
 * </ul>
 */
public final class Tx {

    private static final ThreadLocal<Connection> CURRENT = new ThreadLocal<>();

    private Tx() {
    }

    /** Chạy một đoạn việc trong giao dịch. Lỗi thì hủy tất cả và ném lỗi lên tiếp. */
    public static void run(Runnable work) {
        call(() -> {
            work.run();
            return null;
        });
    }

    /** Như {@link #run(Runnable)} nhưng trả về kết quả của đoạn việc. */
    public static <R> R call(Supplier<R> work) {
        if (CURRENT.get() != null) {
            return work.get();                 // đã trong giao dịch: gộp vào, để giao dịch ngoài cùng quyết định
        }
        try (Connection cn = DBConnection.getConnection()) {
            cn.setAutoCommit(false);
            CURRENT.set(cn);
            try {
                R result = work.get();
                cn.commit();
                return result;
            } catch (RuntimeException | Error e) {
                rollbackQuietly(cn, e);
                throw e;
            } finally {
                CURRENT.remove();              // luôn dọn, kể cả khi lỗi, để không rò sang việc khác
            }
        } catch (SQLException e) {
            throw new DataAccessException("Không hoàn tất được giao dịch.", e);
        }
    }

    /** Kết nối của giao dịch đang chạy trên luồng này; null nếu không trong Tx. Chỉ dành cho lớp DAO. */
    public static Connection current() {
        return CURRENT.get(); 
    }

    public static boolean isActive() {
        return CURRENT.get() != null;
    }

    private static void rollbackQuietly(Connection cn, Throwable cause) {
        try {
            cn.rollback();
        } catch (SQLException rollbackError) {
            cause.addSuppressed(rollbackError);   // giữ lỗi gốc là lỗi chính
        }
    }
}
