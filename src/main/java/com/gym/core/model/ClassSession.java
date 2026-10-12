package com.gym.core.model;

import com.gym.core.util.Check;

/** Lớp tập nhóm (bảng CLASS, con của SESSION). */
public class ClassSession extends Session {

    private String className;
    private int maxCapacity;

    /** Tên lớp. */
    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = Check.text(className, "Tên lớp");
    }

    /** Số chỗ tối đa. */
    public int getMaxCapacity() {
        return maxCapacity;
    }

    public void setMaxCapacity(int maxCapacity) {
        this.maxCapacity = Check.positive(maxCapacity, "Số chỗ tối đa");
    }

    @Override
    public String getSessionTitle() {
        return className;
    }
}
