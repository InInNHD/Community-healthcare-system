package com.community.healthcare;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(properties = {
        "spring.flyway.enabled=true",
        "spring.jpa.hibernate.ddl-auto=validate",
        "app.bootstrap.enabled=false",
        "app.security.bootstrap.enabled=false"
})
class MySqlMigrationIntegrationTests {
    @Container
    @ServiceConnection
    static final MySQLContainer MYSQL = new MySQLContainer("mysql:8.4")
            .withDatabaseName("community_healthcare");

    @Autowired JdbcTemplate jdbc;

    @Test
    void emptyMySqlDatabaseIsFullyCreatedByFlyway() {
        Integer successfulMigrations = jdbc.queryForObject(
                "select count(*) from flyway_schema_history where success = 1", Integer.class);
        Integer coreTables = jdbc.queryForObject("""
                select count(*) from information_schema.tables
                where table_schema = database()
                  and table_name in ('app_user', 'patient', 'doctor', 'appointment',
                                     'health_record', 'chronic_case', 'medicine',
                                     'organization', 'site', 'department', 'staff_profile',
                                     'staff_site_assignment', 'patient_identifier',
                                     'patient_site_enrollment', 'guardian_relationship',
                                     'patient_access_grant', 'audit_event')
                """, Integer.class);

        assertThat(successfulMigrations).isEqualTo(7);
        assertThat(coreTables).isEqualTo(17);
    }
}
