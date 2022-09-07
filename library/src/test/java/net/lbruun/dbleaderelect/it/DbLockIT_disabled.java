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

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import net.lbruun.dbleaderelect.LeaderElectorConfiguration;
import net.lbruun.dbleaderelect.helpers.LiquibaseRunner;
import org.junit.jupiter.api.Test;

/**
 *
 * @author lars
 */

public class DbLockIT_disabled {
    
    private static final String SQL = 
            "SELECT "
            + "    candidate_id,"
            + "    last_seen_timestamp,"
            + "    lease_counter"
            + " FROM " + LeaderElectorConfiguration.DEFAULT_TABLENAME 
            + " WHERE role_id = ?" 
            + " FOR UPDATE";


    @Test
    public void testPostgresSQLLock() throws InterruptedException, SQLException {
        System.out.println("Test : testPostgresSQLLock");
        
        // Use a test container dedicated to this test alone
//        try (final PostgreSQLContainer testContainer = new PostgreSQLContainer("postgres:14.1")
//                .withDatabaseName("testdb")
//                .withUsername("sa")
//                .withPassword("sa");) {

            //testContainer.start();

            HikariConfig hikariConfig = new HikariConfig();
//            hikariConfig.setJdbcUrl(testContainer.getJdbcUrl());
//            hikariConfig.setUsername(testContainer.getUsername());
//            hikariConfig.setPassword(testContainer.getPassword());
            hikariConfig.setJdbcUrl("jdbc:postgresql://localhost:5432/leaderelect");
            hikariConfig.setUsername("postgres");
            hikariConfig.setPassword("postgres");
            hikariConfig.setMaximumPoolSize(2);
            hikariConfig.setMinimumIdle(1);
            hikariConfig.setConnectionTimeout(3000);  // value is in milliseconds

            HikariDataSource hikariDataSource = new HikariDataSource(hikariConfig);
            hikariDataSource.setLoginTimeout(3);  // value is seconds
            LiquibaseRunner liquibaseRunner = new LiquibaseRunner(hikariDataSource, LeaderElectorConfiguration.DEFAULT_TABLENAME);
            liquibaseRunner.execStart();
            
            try ( Connection connection = hikariDataSource.getConnection()) {
//                connection.setAutoCommit(false);
                try ( PreparedStatement preparedStatement = connection.prepareStatement(SQL)) {
                    // Set a timeout. We do not wish to wait for the database lock forever.
                    preparedStatement.setQueryTimeout(3);
                    
                    preparedStatement.setString(1, LeaderElectorConfiguration.DEFAULT_ROLEID);

                    try ( ResultSet rs = preparedStatement.executeQuery()) {
                        rs.next();
                        Thread.sleep(40000);
                    }
//                    connection.commit();
                }
            }

//        }
    }

    
    
}
