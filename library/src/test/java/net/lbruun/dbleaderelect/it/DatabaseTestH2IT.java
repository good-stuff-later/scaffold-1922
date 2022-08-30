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

import javax.sql.DataSource;
import net.lbruun.dbleaderelect.DatabaseEngine;
import net.lbruun.dbleaderelect.LeaderElectorConfiguration;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.containers.JdbcDatabaseContainer;


@DisplayName("H2 Integration Test") 
@Testcontainers
public class DatabaseTestH2IT extends DatabaseWithTestContainers {
    
    @BeforeAll
    public static void setUpBeforeAll() {
        System.out.println("----------------------------------------------");
        System.out.println("  H2                                 ");
        System.out.println("----------------------------------------------");
    }

    @Override
    public DataSource getSpecialDataSource() {
        org.h2.jdbcx.JdbcDataSource ds = new JdbcDataSource();
        ds.setURL("jdbc:h2:mem:/test");
        ds.setUser("sa");
        ds.setPassword("sa");
        return ds;
    }
    
    @Override
    public JdbcDatabaseContainer getJdbcDatabaseContainer() {
        throw new UnsupportedOperationException("Not supported"); 
    }

    @Override
    public DatabaseEngine getDatabaseEngineType() {
        return DatabaseEngine.H2;
    }
    
    @Override
    public LeaderElectorConfiguration getLeaderElectorConfiguration(String schemaName, String tableName) {
        return LeaderElectorConfiguration.builder()
                .withDatabaseEngine(DatabaseEngine.H2)
                .withTableName(tableName)
                .withSchemaName(schemaName).build();
    }
}
