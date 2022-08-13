/*
 * Copyright 2021 lbruun.net
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
package net.lbruun.dbleaderelect;

import net.lbruun.dbleaderelect.helpers.LiquibaseRunner;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.AfterAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 *
 */
public class PostgresPwdChangeTest {

    private static DataSource DS;

    public PostgresPwdChangeTest() {
    }

    @BeforeAll
    public static void setUpClass() throws SQLException {
        System.out.println("BeforeAll: Setting up DataSource, creating temp table and populating it.");
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:postgresql://localhost:5432/leaderelect");
        config.setUsername("lbhtmp");
        config.setPassword("lbhtmp");
        config.setMinimumIdle(1);
        config.setMaximumPoolSize(2);

        DS = new HikariDataSource(config);

    }

    @AfterAll
    public static void tearDownClass() throws SQLException {

        System.out.println("AfterAll: Closing connection pool.");
        ((HikariDataSource) DS).close();
    }

    @BeforeEach
    public void setUp() {
    }

    @AfterEach
    public void tearDown() {
    }

    @Test
    public void testPwdUpdate() throws InterruptedException, SQLException {

        while (true) {
            try ( Connection connection = DS.getConnection()) {

                DatabaseMetaData metaData = connection.getMetaData();

                try ( ResultSet rs = metaData.getTables(null, null, null, new String[]{"TABLE"})) {
                    while (rs.next()) {
                        String schema = rs.getString(2);
                        String tableName = rs.getString(3);
                        System.out.println(schema + "." + tableName);
                    }
                }

            } catch (SQLException ex) {
                int x = 5;
            }
            Thread.sleep(30000);
        }
    }

}
