package com.gym.core.dao;

import java.util.List;
import java.util.Optional;

/**
 * Hợp đồng chung cho 17 DAO. Mỗi bảng một DAO, chỉ CRUD và truy vấn, không chứa luật nghiệp vụ.
 * Lỗi truy cập dữ liệu được ném dưới dạng DataAccessException (unchecked).
 *
 * @param <T>  kiểu model
 * @param <ID> kiểu khóa chính
 */
public interface BaseDao<T, ID> {

    Optional<T> findById(ID id); 

    List<T> findAll();

    /** Phân trang, page bắt đầu từ 1. */
    PageResult<T> findPage(int page, int pageSize);

    /** Thêm mới, trả về đối tượng đã có khóa sinh tự động. */
    T insert(T entity);

    void update(T entity);

    void deleteById(ID id);
}
