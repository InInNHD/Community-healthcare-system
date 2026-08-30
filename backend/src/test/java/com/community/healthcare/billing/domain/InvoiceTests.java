package com.community.healthcare.billing.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InvoiceTests {
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-08-22T03:00:00Z"), ZoneOffset.UTC);

    @Test
    void conservesMoneyAcrossPartialAndFullPaymentsAndRefunds() {
        Invoice invoice = Invoice.draft(UUID.randomUUID(), new BigDecimal("100.00"), CLOCK);
        invoice.issue(CLOCK);

        Payment first = invoice.pay(UUID.randomUUID(), new BigDecimal("40.00"), CLOCK);
        Payment second = invoice.pay(UUID.randomUUID(), new BigDecimal("60.00"), CLOCK);
        Refund partialRefund = invoice.refund(UUID.randomUUID(), new BigDecimal("30.00"), CLOCK);

        assertThat(first.amount()).isEqualByComparingTo("40.00");
        assertThat(second.amount()).isEqualByComparingTo("60.00");
        assertThat(partialRefund.amount()).isEqualByComparingTo("30.00");
        assertThat(invoice.status()).isEqualTo(InvoiceStatus.PAID);
        assertThat(invoice.paidAmount()).isEqualByComparingTo("100.00");
        assertThat(invoice.refundedAmount()).isEqualByComparingTo("30.00");
        assertThat(invoice.netPaidAmount()).isEqualByComparingTo("70.00");

        invoice.refund(UUID.randomUUID(), new BigDecimal("70.00"), CLOCK);
        assertThat(invoice.status()).isEqualTo(InvoiceStatus.REFUNDED);
        assertThat(invoice.netPaidAmount()).isEqualByComparingTo("0.00");
    }

    @Test
    void paymentAndRefundCannotExceedConservedAmounts() {
        Invoice invoice = Invoice.draft(UUID.randomUUID(), new BigDecimal("100.00"), CLOCK);
        invoice.issue(CLOCK);

        assertThatThrownBy(() -> invoice.pay(UUID.randomUUID(), new BigDecimal("100.01"), CLOCK))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("超额");
        invoice.pay(UUID.randomUUID(), new BigDecimal("100.00"), CLOCK);
        assertThatThrownBy(() -> invoice.refund(UUID.randomUUID(), new BigDecimal("100.01"), CLOCK))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("超额");
    }

    @Test
    void enforcesInvoiceLifecycleAndPositiveMoney() {
        Invoice invoice = Invoice.draft(UUID.randomUUID(), new BigDecimal("20.00"), CLOCK);

        assertThatThrownBy(() -> invoice.pay(UUID.randomUUID(), BigDecimal.ONE, CLOCK))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> invoice.refund(UUID.randomUUID(), BigDecimal.ONE, CLOCK))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> Invoice.draft(UUID.randomUUID(), BigDecimal.ZERO, CLOCK))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
