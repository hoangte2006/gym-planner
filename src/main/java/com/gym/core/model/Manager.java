package com.gym.core.model;

/** Quản lý. Không có bảng riêng: là STAFF có Role = 'Manager'. */
public class Manager extends Staff {


    @Override
    public String getRoleName() {
        return StaffRole.MANAGER.getLabel();
    }
}
