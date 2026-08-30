package com.community.healthcare.inventory.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** 不可变库存流水；记录数量及操作完成后的批次结余，便于审计和对账。 */
public record InventoryTransaction(
        UUID id,
        UUID batchId,
        InventoryTransactionType type,
        int quantity,
        int balanceAfter,
        Instant occurredAt) {
    public InventoryTransaction {
        Objects.requireNonNull(id, "库存流水号不能为空");
        Objects.requireNonNull(batchId, "药品批次号不能为空");
        Objects.requireNonNull(type, "库存流水类型不能为空");
        Objects.requireNonNull(occurredAt, "流水时间不能为空");
        if (quantity <= 0) {
            throw new IllegalArgumentException("库存流水数量必须大于零");
        }
        if (balanceAfter < 0) {
            throw new IllegalArgumentException("库存流水结余不能为负数");
        }
    }
}
