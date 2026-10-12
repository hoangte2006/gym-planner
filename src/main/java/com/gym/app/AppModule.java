package com.gym.app;

import com.gym.core.model.StaffRole;

import javax.swing.JPanel;
import java.util.Set;

/**
 * Một module chức năng của ứng dụng. Mỗi module một lớp, được đăng ký trong ModuleRegistry
 * và hiện thành một mục trên menu bên trái.
 */
public interface AppModule {

    /** Mã ngắn, duy nhất (dùng làm khóa CardLayout). */
    String getId();

    /** Tên hiển thị trên menu. */
    String getTitle();

    /** Các vai trò được vào module này. */
    Set<StaffRole> getAllowedRoles();

    /** Tạo màn hình chính của module. Gọi một lần khi người dùng mở module lần đầu. */
    JPanel createPanel();
}
