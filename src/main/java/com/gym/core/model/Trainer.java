package com.gym.core.model;

import com.gym.core.util.Check;

/** Huấn luyện viên (bảng TRAINER, con của STAFF). */
public class Trainer extends Staff {

    private String specialty;
    private String certification;

    /** Chuyên môn. */
    public String getSpecialty() {
        return specialty;
    }

    public void setSpecialty(String specialty) {
        this.specialty = Check.text(specialty, "Chuyên môn");
    }

    /** Chứng chỉ. */
    public String getCertification() {
        return certification;
    }

    public void setCertification(String certification) {
        this.certification = certification;
    }

    @Override
    public String getRoleName() {
        return StaffRole.TRAINER.getLabel();
    }
}
