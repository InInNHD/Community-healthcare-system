package com.community.healthcare;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:demo-flyway-schema;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "app.bootstrap.enabled=false",
        "app.security.bootstrap.enabled=false"
})
@ActiveProfiles("demo")
class DemoFlywaySchemaIntegrationTests {
    @Autowired JdbcTemplate jdbc;

    @Test
    void demoEmptyDatabaseIsMigratedThroughR5() {
        Integer requiredTables = jdbc.queryForObject("""
                select count(*) from information_schema.tables
                where lower(table_name) in ('flyway_schema_history','outbox_event','quality_snapshot')
                """, Integer.class);

        assertThat(requiredTables).isEqualTo(3);
    }
}
