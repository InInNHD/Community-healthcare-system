package com.community.healthcare.inventory.domain;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/**
 * 药品批次聚合，维护批号、有效期和实时结余。
 *
 * <p>所有出库都由该对象校验有效期和可用数量，确保业务层无法产生负库存。</p>
 */
public final class MedicineBatch {
    private final UUID id;
    private final UUID medicineId;
    private final String lotNumber;
    private final LocalDate expiresOn;
    private final Instant receivedAt;
    private int onHand;

    private MedicineBatch(UUID id,
                          UUID medicineId,
                          String lotNumber,
                          LocalDate expiresOn,
                          int onHand,
                          Instant receivedAt) {
        this.id = Objects.requireNonNull(id, "药品批次号不能为空");
        this.medicineId = Objects.requireNonNull(medicineId, "药品号不能为空");
        if (lotNumber == null || lotNumber.isBlank()) {
            throw new IllegalArgumentException("批号不能为空");
        }
        if (onHand < 0) {
            throw new IllegalArgumentException("批次库存不能为负数");
        }
        this.lotNumber = lotNumber.trim();
        this.expiresOn = Objects.requireNonNull(expiresOn, "有效期不能为空");
        this.onHand = onHand;
        this.receivedAt = Objects.requireNonNull(receivedAt, "入库时间不能为空");
    }

    /** 创建已验收入库的药品批次。 */
    public static MedicineBatch received(UUID id,
                                         UUID medicineId,
                                         String lotNumber,
                                         LocalDate expiresOn,
                                         int quantity,
                                         Clock clock) {
        return new MedicineBatch(id, medicineId, lotNumber, expiresOn, quantity, now(clock));
    }

    /** 扣减可用库存并返回带扣减后结余的出库流水。 */
    public InventoryTransaction issue(UUID transactionId, int quantity, Clock clock) {
        Objects.requireNonNull(transactionId, "库存流水号不能为空");
        if (quantity <= 0) {
            throw new IllegalArgumentException("发药数量必须大于零");
        }
        if (!isUsable(clock)) {
            throw new IllegalStateException("过期批次不可发药");
        }
        if (quantity > onHand) {
            throw new IllegalStateException("库存不足，批次扣减不能为负数");
        }
        onHand -= quantity;
        return new InventoryTransaction(
                transactionId, id, InventoryTransactionType.ISSUE, quantity, onHand, now(clock));
    }

    /** 判断批次是否同时满足有库存且尚未到期。 */
    public boolean isUsable(Clock clock) {
        Clock checkedClock = Objects.requireNonNull(clock, "时钟不能为空");
        return onHand > 0 && expiresOn.isAfter(LocalDate.now(checkedClock));
    }

    private static Instant now(Clock clock) {
        return Objects.requireNonNull(clock, "时钟不能为空").instant();
    }

    public UUID id() { return id; }
    public UUID medicineId() { return medicineId; }
    public String lotNumber() { return lotNumber; }
    public LocalDate expiresOn() { return expiresOn; }
    public int onHand() { return onHand; }
    public Instant receivedAt() { return receivedAt; }
}
