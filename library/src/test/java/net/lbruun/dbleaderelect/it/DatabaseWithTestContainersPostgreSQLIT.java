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
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;


@DisplayName("PostgreSQL Integration Test") 
@Testcontainers
public class DatabaseWithTestContainersPostgreSQLIT extends DatabaseWithTestContainers {
    
    @BeforeAll
    public static void setUpBeforeAll() {
        System.out.println("----------------------------------------------");
        System.out.println("  PostgreSQL                                 ");
        System.out.println("----------------------------------------------");
    }

    // will be shared between test methods
    @Container
    private static final PostgreSQLContainer POSTGRESQL_CONTAINER = new PostgreSQLContainer("postgres:14.1")
        .withDatabaseName("testdb")
        .withUsername("sa")
        .withPassword("sa");

    @Override
    public JdbcDatabaseContainer getJdbcDatabaseContainer() {
        return POSTGRESQL_CONTAINER;
    }

    @Override
    public DatabaseEngine getDatabaseEngineType() {
        return DatabaseEngine.POSTGRESQL;
    }
    
    @Override
    public LeaderElectorConfiguration getLeaderElectorConfiguration(String schemaName, String tableName) {
        return LeaderElectorConfiguration.builder()
                .withDatabaseEngine(DatabaseEngine.POSTGRESQL)
                .withTableName(tableName)
                .withSchemaName(schemaName).build();
    }
}
