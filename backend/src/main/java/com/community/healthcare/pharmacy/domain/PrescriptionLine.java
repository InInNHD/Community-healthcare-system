package com.community.healthcare.pharmacy.domain;

import java.util.Objects;
import java.util.UUID;

/** 不可变处方明细，保存药品标识、开立时名称快照和数量。 */
public record PrescriptionLine(UUID medicineId, String medicineName, int quantity) {
    public PrescriptionLine {
        Objects.requireNonNull(medicineId, "药品号不能为空");
        if (medicineName == null || medicineName.isBlank()) {
            throw new IllegalArgumentException("药品名称不能为空");
        }
        medicineName = medicineName.trim();
        if (quantity <= 0) {
            throw new IllegalArgumentException("处方数量必须大于零");
        }
    }
}
