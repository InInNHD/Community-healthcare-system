package com.community.healthcare;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class R2FlywayMigrationTests {
    @Test
    void v8CreatesSchedulingEncounterAndIdempotencySchemaAfterR1Baseline() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:r2-flyway;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1", "sa", "");
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        createR1ReferencedTables(jdbc);
        Flyway flyway = Flyway.configure().dataSource(dataSource)
                .baselineOnMigrate(true).baselineVersion("7").load();

        assertThat(flyway.migrate().migrationsExecuted).isGreaterThanOrEqualTo(1);

        List<String> tables = jdbc.queryForList(
                "select table_name from information_schema.tables where table_schema = 'public'", String.class);
        assertThat(tables).contains("sched_session", "sched_slot", "sched_appointment", "sched_check_in",
                "sched_queue_entry", "sched_event", "clinical_encounter", "clinical_note",
                "clinical_diagnosis", "clinical_document_version", "idempotency_record");
        List<String> appointmentColumns = jdbc.queryForList("select column_name from information_schema.columns "
                + "where table_schema = 'public' and table_name = 'sched_appointment'", String.class);
        assertThat(appointmentColumns).contains("patient_id", "slot_id", "status", "version");
    }

    private void createR1ReferencedTables(JdbcTemplate jdbc) {
        jdbc.execute("create table organization (id bigint primary key)");
        jdbc.execute("create table site (id bigint primary key)");
        jdbc.execute("create table department (id bigint primary key)");
        jdbc.execute("create table staff_profile (id bigint primary key)");
        jdbc.execute("create table patient (id bigint primary key)");
        jdbc.execute("create table audit_event (id bigint primary key)");
    }
}
