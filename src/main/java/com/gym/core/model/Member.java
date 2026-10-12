package com.gym.core.model;

import com.gym.core.util.Check;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** Hội viên (bảng MEMBER, con của VISITOR). */
public class Member extends Visitor {

    private String email;
    private LocalDate birthDate;
    private byte[] photo;
    private LocalDateTime registrationDate;
    private boolean photoOnFile;

    /** Email. */
    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = Check.email(email, "Email");
    }

    /** Ngày sinh. */
    public LocalDate getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(LocalDate birthDate) {
        this.birthDate = birthDate;
    }

    /** Ảnh. */
    public byte[] getPhoto() {
        return photo;
    }

    public void setPhoto(byte[] photo) {
        this.photo = photo;
    }

    /** Ngày đăng ký. */
    public LocalDateTime getRegistrationDate() {
        return registrationDate;
    }

    public void setRegistrationDate(LocalDateTime registrationDate) {
        this.registrationDate = registrationDate;
    }

    @Override
    public String getDisplayName() {
        return getFullName() + " (Hội viên)";
    }

    /**
     * Hội viên đã có ảnh chưa. Khi đọc danh sách, DAO không tải ảnh (nặng) mà chỉ đánh dấu
     * "có ảnh trong DB"; muốn lấy ảnh dùng MemberDao.findPhoto(id).
     */
    public boolean hasPhoto() {
        return photoOnFile || (photo != null && photo.length > 0);
    }

    public void setPhotoOnFile(boolean photoOnFile) {
        this.photoOnFile = photoOnFile;
    }
}
