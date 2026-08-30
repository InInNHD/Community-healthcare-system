package com.community.healthcare;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class R1FlywayMigrationTests {
    @Test
    void v6CreatesIdentityRegistryAndAppendOnlyAuditSchemaOnEmptyDatabase() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:r1-flyway;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1", "sa", "");
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        jdbc.execute("create table patient (id bigint primary key, id_card varchar(32) not null unique)");
        jdbc.update("insert into patient(id, id_card) values (?, ?)", 1L, "11010519491231002X");
        jdbc.execute("create table app_user (id bigint primary key)");
        Flyway flyway = Flyway.configure().dataSource(dataSource)
                .baselineOnMigrate(true).baselineVersion("5").load();

        assertThat(flyway.migrate().migrationsExecuted).isGreaterThanOrEqualTo(2);

        List<String> tables = jdbc.queryForList("select table_name from information_schema.tables where table_schema = 'public'", String.class);
        assertThat(tables).contains("organization", "site", "department", "staff_profile", "staff_site_assignment",
                "patient_identifier", "patient_site_enrollment", "guardian_relationship", "patient_access_grant", "audit_event");
        List<String> columns = jdbc.queryForList("select column_name from information_schema.columns "
                + "where table_schema = 'public' and table_name = 'app_user'", String.class);
        assertThat(columns).contains("account_status", "authz_version", "mfa_required", "mfa_enrolled_at",
                "mfa_secret_ciphertext", "staff_profile_id");
        assertThat(jdbc.queryForObject("select id_card from patient where id = 1", String.class))
                .doesNotContain("11010519491231002X").startsWith("****");
    }
}
