package com.gym.core;

import com.gym.core.exception.DataAccessException;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Cấp kết nối tới SQL Server. Mỗi lần gọi trả về một Connection mới, người gọi đóng bằng try-with-resources.
 */
public final class DBConnection {

    private static final int LOGIN_TIMEOUT_SECONDS = 5;

    private DBConnection() {
    }

    public static Connection getConnection() {
        Config c = Config.get();
        String url = "jdbc:sqlserver://" + c.get("db.host", "localhost") + ":" + c.getInt("db.port", 1433)
                + ";databaseName=" + c.get("db.name", "GymPlanner")
                + ";encrypt=" + c.getBoolean("db.encrypt", false)
                + ";trustServerCertificate=" + c.getBoolean("db.trustServerCertificate", true)
                // Các cột chữ trong DB là VARCHAR: gửi tham số dạng VARCHAR để chỉ mục được dùng (nhanh hơn)
                + ";sendStringParametersAsUnicode=false"
                + ";loginTimeout=" + LOGIN_TIMEOUT_SECONDS;
        try {
            return DriverManager.getConnection(url, c.get("db.user", ""), c.get("db.password", ""));
        } catch (SQLException e) {
            throw new DataAccessException(explain(e), e);
        }
    }

    /** Kiểm tra kết nối (dùng cho màn hình khởi động). Ném DataAccessException với thông điệp dễ hiểu nếu lỗi. */
    public static void test() {
        try (Connection cn = getConnection(); Statement st = cn.createStatement()) {
            st.execute("SELECT 1");
        } catch (SQLException e) {
            throw new DataAccessException(explain(e), e);
        }
    }

    private static String explain(SQLException e) {
        int code = e.getErrorCode();
        String msg = String.valueOf(e.getMessage());
        if (code == 18456) {
            return "Sai tên đăng nhập hoặc mật khẩu SQL Server. Kiểm tra db.user và db.password trong "
                    + Config.DEFAULT_FILE + ".";
        }
        if (code == 4060) {
            return "Không tìm thấy cơ sở dữ liệu '" + Config.get().get("db.name", "GymPlanner")
                    + "'. Hãy chạy các script trong thư mục db/ trước.";
        }
        if (msg.contains("TCP/IP") || msg.contains("Connection refused") || msg.contains("timed out")) {
            return "Không kết nối được SQL Server. Hãy kiểm tra dịch vụ SQL Server đang chạy, đã bật TCP/IP cổng "
                    + Config.get().getInt("db.port", 1433) + " (chạy db/setup_local_sqlserver.ps1).";
        }
        return "Lỗi kết nối cơ sở dữ liệu: " + msg;
    }
}
