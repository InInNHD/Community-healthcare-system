package com.community.healthcare;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class R4FlywayMigrationTests {
    @Test
    void v10CreatesFamilyDoctorAndPublicHealthSchemaAfterR3Baseline() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:r4-flyway;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1", "sa", "");
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        createReferencedTables(jdbc);
        Flyway flyway = Flyway.configure().dataSource(dataSource)
                .baselineOnMigrate(true).baselineVersion("9").target("10").load();

        assertThat(flyway.migrate().migrationsExecuted).isGreaterThanOrEqualTo(1);
        List<String> tables = jdbc.queryForList(
                "select table_name from information_schema.tables where table_schema='public'", String.class);
        assertThat(tables).contains("care_team", "care_team_member", "service_package", "service_package_item",
                "fd_contract", "fd_contract_history", "fd_service_task", "fd_service_task_history",
                "fd_service_fulfillment", "ph_registry", "ph_follow_up_plan", "ph_follow_up_visit",
                "ph_risk_assessment", "ph_health_alert", "ph_rule_version");
    }

    private void createReferencedTables(JdbcTemplate jdbc) {
        jdbc.execute("create table organization (id bigint primary key)");
        jdbc.execute("create table site (id bigint primary key)");
        jdbc.execute("create table staff_profile (id bigint primary key)");
        jdbc.execute("create table patient (id bigint primary key)");
    }
}
