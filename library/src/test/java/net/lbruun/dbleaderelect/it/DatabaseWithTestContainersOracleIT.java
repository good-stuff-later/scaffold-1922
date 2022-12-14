/*
 * Copyright 2022 lbruun.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.lbruun.dbleaderelect.it;

import net.lbruun.dbleaderelect.DatabaseEngine;
import net.lbruun.dbleaderelect.LeaderElectorConfiguration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.OracleContainer;
import org.testcontainers.junit.jupiter.Container;

/**
 *
 * @author lars
 */

@DisplayName("Oracle Integration Test") 
@Testcontainers
public class DatabaseWithTestContainersOracleIT extends DatabaseWithTestContainers {
    
    @BeforeAll
    public static void setUpBeforeAll() {
        System.out.println("----------------------------------------------");
        System.out.println("  Oracle                                      ");
        System.out.println("----------------------------------------------");
    }

    // will be shared between test methods
    @Container
    private static final OracleContainer ORACLE_CONTAINER = new OracleContainer("gvenzl/oracle-xe:18.4.0-slim")
            .withDatabaseName("testDB")
            .withUsername("testUser")
            .withPassword("testPassword");

    @Override
    public JdbcDatabaseContainer getJdbcDatabaseContainer() {
        return ORACLE_CONTAINER;
    }

    @Override
    public DatabaseEngine getDatabaseEngineType() {
        return DatabaseEngine.ORACLE;
    }
    
    @Override
    public LeaderElectorConfiguration getLeaderElectorConfiguration(String schemaName, String tableName) {
        return LeaderElectorConfiguration.builder()
                .withDatabaseEngine(DatabaseEngine.ORACLE)
                .withTableName(tableName)
                .withSchemaName(schemaName)
                .build();
    }

    @Override
    public String getDriverClassName() {
        return "oracle.jdbc.driver.OracleDriver";
    }
}
