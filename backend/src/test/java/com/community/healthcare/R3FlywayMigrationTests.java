package com.community.healthcare;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class R3FlywayMigrationTests {
    @Test
    void v9CreatesPharmacyInventoryBillingAndInsuranceSchemaAfterR2Baseline() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:r3-flyway;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1", "sa", "");
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        createReferencedTables(jdbc);
        Flyway flyway = Flyway.configure().dataSource(dataSource)
                .baselineOnMigrate(true).baselineVersion("8").target(MigrationVersion.fromVersion("9")).load();

        assertThat(flyway.migrate().migrationsExecuted).isEqualTo(1);
        List<String> tables = jdbc.queryForList(
                "select table_name from information_schema.tables where table_schema='public'", String.class);
        assertThat(tables).contains("rx_medicine_sku", "inventory_warehouse", "inventory_batch",
                "inventory_transaction", "rx_prescription", "rx_prescription_item", "rx_review",
                "rx_dispense", "rx_dispense_item", "billing_charge_item", "billing_invoice",
                "billing_invoice_line", "billing_payment", "billing_refund", "billing_daily_settlement",
                "insurance_claim", "insurance_claim_event");
    }

    private void createReferencedTables(JdbcTemplate jdbc) {
        jdbc.execute("create table organization(id bigint primary key)");
        jdbc.execute("create table site(id bigint primary key)");
        jdbc.execute("create table department(id bigint primary key)");
        jdbc.execute("create table staff_profile(id bigint primary key)");
        jdbc.execute("create table patient(id bigint primary key)");
        jdbc.execute("create table clinical_encounter(id bigint primary key)");
    }
}
