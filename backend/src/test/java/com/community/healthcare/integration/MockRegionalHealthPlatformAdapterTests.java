package com.community.healthcare.integration;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MockRegionalHealthPlatformAdapterTests {
    @Test
    void returnsTheSameReceiptForTheSameIdempotencyKey() {
        RegionalHealthPlatformPort adapter = new MockRegionalHealthPlatformAdapter();

        ExchangeReceipt first = adapter.submit("r5-key", "{\"referralId\":1}");
        ExchangeReceipt replay = adapter.submit("r5-key", "{\"referralId\":1}");

        assertThat(replay).isEqualTo(first);
        assertThat(first.simulation()).isTrue();
    }
}
