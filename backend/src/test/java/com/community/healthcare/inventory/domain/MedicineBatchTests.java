package com.community.healthcare.inventory.domain;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MedicineBatchTests {
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-08-22T03:00:00Z"), ZoneOffset.UTC);

    @Test
    void issuingCreatesImmutableLedgerEntryAndCannotMakeStockNegative() {
        MedicineBatch batch = batch("B001", LocalDate.parse("2027-01-01"), 10,
                Instant.parse("2026-08-01T00:00:00Z"));

        InventoryTransaction transaction = batch.issue(UUID.randomUUID(), 4, CLOCK);

        assertThat(batch.onHand()).isEqualTo(6);
        assertThat(transaction.type()).isEqualTo(InventoryTransactionType.ISSUE);
        assertThat(transaction.quantity()).isEqualTo(4);
        assertThat(transaction.balanceAfter()).isEqualTo(6);
        assertThat(transaction.occurredAt()).isEqualTo(CLOCK.instant());
        assertThatThrownBy(() -> batch.issue(UUID.randomUUID(), 7, CLOCK))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("库存");
    }

    @Test
    void expiredOrExpiringTodayBatchCannotBeIssued() {
        MedicineBatch expired = batch("OLD", LocalDate.parse("2026-08-21"), 10,
                Instant.parse("2026-07-01T00:00:00Z"));
        MedicineBatch expiresToday = batch("TODAY", LocalDate.parse("2026-08-22"), 10,
                Instant.parse("2026-07-02T00:00:00Z"));

        assertThatThrownBy(() -> expired.issue(UUID.randomUUID(), 1, CLOCK))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("过期");
        assertThatThrownBy(() -> expiresToday.issue(UUID.randomUUID(), 1, CLOCK))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("过期");
    }

    @Test
    void fifoAndFefoChooseOnlyUsableBatches() {
        UUID medicineId = UUID.randomUUID();
        MedicineBatch oldest = batch(medicineId, "OLD", LocalDate.parse("2027-12-01"), 10,
                Instant.parse("2026-06-01T00:00:00Z"));
        MedicineBatch earliestExpiry = batch(medicineId, "EARLY", LocalDate.parse("2026-12-01"), 10,
                Instant.parse("2026-07-01T00:00:00Z"));
        MedicineBatch empty = batch(medicineId, "EMPTY", LocalDate.parse("2026-09-01"), 0,
                Instant.parse("2026-05-01T00:00:00Z"));

        assertThat(BatchSelectionPolicy.FIFO.select(List.of(earliestExpiry, empty, oldest), CLOCK))
                .contains(oldest);
        assertThat(BatchSelectionPolicy.FEFO.select(List.of(oldest, empty, earliestExpiry), CLOCK))
                .contains(earliestExpiry);
    }

    private static MedicineBatch batch(String lot, LocalDate expiry, int quantity, Instant receivedAt) {
        return batch(UUID.randomUUID(), lot, expiry, quantity, receivedAt);
    }

    private static MedicineBatch batch(UUID medicineId, String lot, LocalDate expiry, int quantity, Instant receivedAt) {
        return MedicineBatch.received(UUID.randomUUID(), medicineId, lot, expiry, quantity,
                Clock.fixed(receivedAt, ZoneOffset.UTC));
    }
}
