package com.gym.core.dao;

import com.gym.core.DBConnection;
import com.gym.core.Tx;
import com.gym.core.exception.DataAccessException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Phần JDBC dùng chung cho mọi DAO: mở kết nối, chạy SQL, đóng kết nối, và đổi SQLException
 * thành DataAccessException ở MỘT chỗ duy nhất. DAO con chỉ cần khai báo tên bảng, khóa chính,
 * cách đổi một dòng thành model, và câu INSERT/UPDATE của riêng bảng đó.
 *
 * Mọi hàm ở đây đều "biết" giao dịch {@link Tx}: nếu đang chạy bên trong Tx.run thì dùng kết nối của
 * giao dịch (không tự mở, không tự đóng, không tự chốt); nếu không thì tự mở kết nối riêng như trước.
 *
 * @param <T>  kiểu model
 * @param <ID> kiểu khóa chính
 */
public abstract class AbstractJdbcDao<T, ID> implements BaseDao<T, ID> {

    /** Một đoạn việc chạy trên một kết nối (dùng trong transaction). */
    @FunctionalInterface
    protected interface SqlWork<R> {
        R run(Connection cn) throws SQLException;
    }

    /**
     * Kết nối "mượn" cho một lệnh: dùng kết nối của Tx nếu có (và không đóng nó), nếu không thì mở kết nối
     * mới (và đóng khi xong). Dùng với try-with-resources.
     */
    protected static final class Lease implements AutoCloseable {
        private final Connection connection;
        private final boolean owned;

        private Lease(Connection connection, boolean owned) {
            this.connection = connection;
            this.owned = owned;
        }

        static Lease open() {
            Connection ambient = Tx.current(); // nếu đang trong Tx thì dùng kết nối của Tx, không đóng; nếu không thì mở kết nối mới và đóng khi xong
            return ambient != null ? new Lease(ambient, false) : new Lease(DBConnection.getConnection(), true);
        }

        public Connection connection() {
            return connection;
        }

        @Override
        public void close() throws SQLException {
            if (owned) {
                connection.close();
            }
        }
    }

    // ---------- 3 thứ DAO con PHẢI khai báo ----------

    /** Tên bảng, ví dụ "ROOM" hoặc "[SESSION]". Dùng cho thông báo lỗi và DELETE mặc định. */
    protected abstract String tableName();

    /**
     * Cột khóa chính, ví dụ "Room_ID". Dùng để lọc theo khóa và sắp xếp.
     * Bảng khóa ghép: trả danh sách cột để sắp xếp, ví dụ "Session_ID, Visitor_ID".
     */
    protected abstract String idColumn();

    /** Đổi dòng hiện tại của ResultSet thành đối tượng model. */
    protected abstract T mapRow(ResultSet rs) throws SQLException;

    // ---------- 2 thứ DAO con CÓ THỂ đổi (bảng kế thừa cần JOIN) ----------

    /** Phần sau FROM. Mặc định là tên bảng; DAO kế thừa trả về chuỗi JOIN. */
    protected String fromClause() {
        return tableName();
    }

    /** Danh sách cột của câu SELECT. Mặc định là *. */
    protected String selectColumns() {
        return "*";
    }

    /** SELECT ... FROM ... (chưa có WHERE). DAO con dùng để viết thêm truy vấn riêng. */
    protected String selectSql() {
        return "SELECT " + selectColumns() + " FROM " + fromClause();
    }

    // ---------- Các hàm của BaseDao đã viết sẵn ----------

    @Override
    public Optional<T> findById(ID id) {
        return queryOne(selectSql() + " WHERE " + idColumn() + " = ?", ps -> ps.setObject(1, id));
    }

    @Override
    public List<T> findAll() {
        return queryList(selectSql() + " ORDER BY " + idColumn(), StatementBinder.NONE);
    }

    @Override
    public PageResult<T> findPage(int page, int pageSize) {
        if (page < 1 || pageSize < 1) {
            throw new IllegalArgumentException("Trang và kích thước trang phải từ 1 trở lên.");
        }
        long total = count("SELECT COUNT(*) FROM " + fromClause(), StatementBinder.NONE);
        List<T> items = queryList(selectSql() + " ORDER BY " + idColumn()
                        + " OFFSET ? ROWS FETCH NEXT ? ROWS ONLY",
                ps -> {
                    ps.setInt(1, (page - 1) * pageSize);
                    ps.setInt(2, pageSize);
                });
        return new PageResult<>(items, page, pageSize, total);
    }

    @Override
    public void deleteById(ID id) {
        execute("DELETE FROM " + tableName() + " WHERE " + idColumn() + " = ?", ps -> ps.setObject(1, id));
    }

    // ---------- Công cụ cho DAO con (tự mở/đóng kết nối, tự bọc lỗi) ----------

    /** Chạy SELECT, trả về nhiều dòng. */
    protected List<T> queryList(String sql, StatementBinder binder) {
        try (Lease lease = Lease.open();
             PreparedStatement ps = lease.connection().prepareStatement(sql)) {
            binder.bind(ps);
            try (ResultSet rs = ps.executeQuery()) {
                List<T> result = new ArrayList<>();
                while (rs.next()) {
                    result.add(mapRow(rs));
                }
                return result;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Không đọc được dữ liệu từ bảng " + tableName(), e);
        }
    }

    /** Chạy SELECT, trả về tối đa một dòng (không có thì Optional rỗng). */
    protected Optional<T> queryOne(String sql, StatementBinder binder) {
        List<T> rows = queryList(sql, binder);
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
    }

    /** Chạy câu cho đúng một số (thường là COUNT). */
    protected long count(String sql, StatementBinder binder) {
        try (Lease lease = Lease.open();
             PreparedStatement ps = lease.connection().prepareStatement(sql)) {
            binder.bind(ps);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getLong(1) : 0;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Không đếm được dữ liệu của bảng " + tableName(), e);
        }
    }

    /** Chạy INSERT/UPDATE/DELETE, trả về số dòng bị ảnh hưởng. */
    protected int execute(String sql, StatementBinder binder) {
        try (Lease lease = Lease.open()) {
            return exec(lease.connection(), sql, binder);
        } catch (SQLException e) {
            throw new DataAccessException("Không ghi được dữ liệu vào bảng " + tableName(), e);
        }
    }

    /** Chạy INSERT cho bảng có khóa IDENTITY, trả về khóa mà DB vừa tự sinh. */
    protected int insertAndGetKey(String sql, StatementBinder binder) {
        try (Lease lease = Lease.open()) {
            return insertKey(lease.connection(), sql, binder);
        } catch (SQLException e) {
            throw new DataAccessException("Không thêm được dữ liệu vào bảng " + tableName(), e);
        }
    }

    // ---------- Transaction: nhiều lệnh, hoặc cùng thành công, hoặc cùng bị hủy ----------

    /**
     * Chạy nhiều lệnh trên CÙNG một kết nối. Có lỗi ở bất kỳ lệnh nào thì hủy tất cả (rollback).
     * Dùng cho bảng kế thừa: ghi vào bảng cha rồi bảng con.
     * Nếu đang bên trong {@link Tx} thì chỉ chạy trên kết nối của Tx; việc chốt hay hủy do Tx quyết định.
     */
    protected <R> R inTransaction(SqlWork<R> work) {
        Connection ambient = Tx.current();
        if (ambient != null) {
            try {
                return work.run(ambient);
            } catch (SQLException e) {
                throw new DataAccessException("Không ghi được dữ liệu vào bảng " + tableName(), e);
            }
        }
        try (Connection cn = DBConnection.getConnection()) {
            cn.setAutoCommit(false);
            try {
                R result = work.run(cn);
                cn.commit();
                return result;
            } catch (SQLException | RuntimeException e) {
                cn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Không ghi được dữ liệu vào bảng " + tableName(), e);
        }
    }

    /** Như execute nhưng chạy trên kết nối có sẵn (dùng bên trong inTransaction). */
    protected static int exec(Connection cn, String sql, StatementBinder binder) throws SQLException {
        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            binder.bind(ps);
            return ps.executeUpdate();
        }
    }

    /** Như insertAndGetKey nhưng chạy trên kết nối có sẵn. */
    protected static int insertKey(Connection cn, String sql, StatementBinder binder) throws SQLException {
        try (PreparedStatement ps = cn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            binder.bind(ps);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
                throw new SQLException("DB không trả về khóa vừa tạo.");
            }
        }
    }

    /** Lấy một số nguyên từ câu SELECT trên kết nối có sẵn (ví dụ MAX + 1). */
    protected static int queryInt(Connection cn, String sql, StatementBinder binder) throws SQLException {
        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            binder.bind(ps);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }
}
