package com.zhaoxinms.contract.tools.aicomponent.config;

import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class FlywayRepairConfig {

    @Bean
    public FlywayMigrationStrategy flywayMigrationStrategy() {
        return new FlywayMigrationStrategy() {
            @Override
            public void migrate(Flyway flyway) {
                try {
                    log.info("Executing Flyway repair before migrate...");
                    flyway.repair();
                } catch (Exception e) {
                    log.warn("Flyway repair failed (will continue to migrate): {}", e.getMessage());
                }
                flyway.migrate();
            }
        };
    }
}



