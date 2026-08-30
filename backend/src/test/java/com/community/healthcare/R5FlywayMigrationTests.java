package com.community.healthcare;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class R5FlywayMigrationTests {
    @Test
    void v11CreatesReferralIntegrationNotificationAndQualitySchema() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:r5-flyway;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1", "sa", "");
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        jdbc.execute("create table patient(id bigint primary key)");
        jdbc.execute("create table staff_profile(id bigint primary key)");
        jdbc.execute("create table clinical_encounter(id bigint primary key)");
        Flyway flyway = Flyway.configure().dataSource(dataSource)
                .baselineOnMigrate(true).baselineVersion("10").load();

        assertThat(flyway.migrate().migrationsExecuted).isEqualTo(1);
        List<String> tables = jdbc.queryForList(
                "select table_name from information_schema.tables where table_schema='public'", String.class);
        assertThat(tables).contains("referral_case", "referral_history", "referral_document",
                "referral_feedback", "referral_exchange_link", "outbox_event", "integration_exchange",
                "integration_dead_letter", "record_release", "notification_message", "notification_delivery",
                "service_feedback", "quality_snapshot");
    }
}
