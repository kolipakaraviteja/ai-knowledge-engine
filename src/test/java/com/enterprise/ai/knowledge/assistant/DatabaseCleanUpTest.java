package com.enterprise.ai.knowledge.assistant;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

@Disabled("This test requires PostgreSQL database. Use SQL scripts for cleanup instead.")
public class DatabaseCleanUpTest {

    @Test
    public void cleanUpAllTables() {
        // This test is disabled because it requires PostgreSQL database
        // For PostgreSQL cleanup to fix Flyway migration issues, use the following SQL script:
        // 
        // -- Clean Flyway schema history
        // DELETE FROM flyway_schema_history;
        //
        // -- Drop all tables (optional - if you want to start completely fresh)
        // DROP SCHEMA public CASCADE;
        // CREATE SCHEMA public;
        // GRANT ALL ON SCHEMA public TO workspace;
        // GRANT ALL ON SCHEMA public TO public;
        //
        // Then restart the application to apply the V4 migration.
        //
        // Alternative: Run Flyway repair command
        // mvn flyway:repair
    }
}
