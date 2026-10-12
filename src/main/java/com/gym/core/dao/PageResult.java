package com.gym.core.dao;

import java.util.List;

/** Một trang kết quả: dữ liệu của trang hiện tại kèm tổng số dòng. */
public record PageResult<T>(List<T> items, int page, int pageSize, long totalItems) {

    public int totalPages() {
        return pageSize <= 0 ? 0 : (int) Math.ceil((double) totalItems / pageSize);
    }
}
