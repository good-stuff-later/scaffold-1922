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
import org.testcontainers.containers.Db2Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.junit.jupiter.Container;


@DisplayName("Db2 LUW Integration Test") 
@Testcontainers
public class DatabaseWithTestContainersDb2IT extends DatabaseWithTestContainers {
    
    @BeforeAll
    public static void setUpBeforeAll() {
        System.out.println("----------------------------------------------");
        System.out.println("  Db2 for Linux, Unix and Windows             ");
        System.out.println("----------------------------------------------");
    }

    // will be shared between test methods
    @Container
    private static final Db2Container DB2_CONTAINER = new Db2Container("ibmcom/db2:11.5.0.0a")
            .acceptLicense();

    @Override
    public JdbcDatabaseContainer getJdbcDatabaseContainer() {
        return DB2_CONTAINER;
    }

    @Override
    public DatabaseEngine getDatabaseEngineType() {
        return DatabaseEngine.DB2_LUW;
    }
    
    @Override
    public LeaderElectorConfiguration getLeaderElectorConfiguration(String schemaName, String tableName) {
        return LeaderElectorConfiguration.builder()
                .withDatabaseEngine(DatabaseEngine.DB2_LUW)
                .withTableName(tableName)
                .withSchemaName(schemaName).build();
    }

    @Override
    public String getDriverClassName() {
        return "com.ibm.db2.jcc.DB2Driver";
    }
}
