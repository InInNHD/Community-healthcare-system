package com.community.healthcare.inventory.domain;

import java.time.Clock;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** 药品批次选择策略，只会从尚未过期且有库存的批次中选择。 */
public enum BatchSelectionPolicy {
    /** 先进先出：优先使用最早入库的批次。 */
    FIFO(Comparator.comparing(MedicineBatch::receivedAt).thenComparing(MedicineBatch::lotNumber)),
    /** 近效期先出：优先使用最早到期的批次，降低报损风险。 */
    FEFO(Comparator.comparing(MedicineBatch::expiresOn)
            .thenComparing(MedicineBatch::receivedAt)
            .thenComparing(MedicineBatch::lotNumber));

    private final Comparator<MedicineBatch> comparator;

    BatchSelectionPolicy(Comparator<MedicineBatch> comparator) {
        this.comparator = comparator;
    }

    /** 根据策略从候选集合选出一个可发药批次。 */
    public Optional<MedicineBatch> select(List<MedicineBatch> batches, Clock clock) {
        Objects.requireNonNull(batches, "候选批次不能为空");
        Clock checkedClock = Objects.requireNonNull(clock, "时钟不能为空");
        return batches.stream()
                .filter(Objects::nonNull)
                .filter(batch -> batch.isUsable(checkedClock))
                .min(comparator);
    }
}
