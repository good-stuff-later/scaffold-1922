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
package net.lbruun.dbleaderelect.utils;

import net.lbruun.dbleaderelect.internal.utils.SQLUtils;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import javax.sql.DataSource;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class SQLUtilsTest {
    
    private static final String SCHEMA_NAME = "myschema";
    public static DataSource dataSource;
    
    @BeforeAll
    public static void setUpClass() {
        JdbcDataSource ds = new JdbcDataSource();
        // In-memory DB with an additional schema. The default schema is 'PUBLIC'
        ds.setURL("jdbc:h2:mem:testdb;INIT=CREATE SCHEMA IF NOT EXISTS " + SCHEMA_NAME + "\\;");
        ds.setUser("sa");
        ds.setPassword("");
        dataSource = ds;
    }
    

    @Test
    public void testTableExists() throws Exception {
        
        String tableName = "mytable";
        try ( Connection connection = dataSource.getConnection()) {
            try ( Statement stmt = connection.createStatement()) {
                stmt.executeUpdate("CREATE TABLE " + tableName + " (mycolumn VARCHAR(2))");
            }
            boolean result = SQLUtils.tableExists(connection, null, tableName);            
            assertEquals(true, result);

            try ( Statement stmt = connection.createStatement()) {
                stmt.executeUpdate("CREATE TABLE "+ SCHEMA_NAME + "." + tableName + " (mycolumn VARCHAR(2))");
            }
            result = SQLUtils.tableExists(connection, SCHEMA_NAME, tableName);            
            assertEquals(true, result);
        }
    }


    @Test
    public void testGetRowCount() throws Exception {
        
        String tableName = "mytable";
        try ( Connection connection = dataSource.getConnection()) {
            try ( Statement stmt = connection.createStatement()) {
                stmt.executeUpdate("CREATE TABLE " + tableName + " (mycolumn VARCHAR(3))");
                stmt.executeUpdate("CREATE TABLE " + SCHEMA_NAME + "." + tableName + " (mycolumn VARCHAR(3))");
                
                stmt.executeUpdate("INSERT INTO " + tableName + " VALUES ('foo')");
                stmt.executeUpdate("INSERT INTO " + tableName + " VALUES ('bar')");
            }
            long rowCount = SQLUtils.getRowCount(connection, null, tableName);
            assertEquals(2, rowCount);

            // Empty the table and test again
            try ( Statement stmt = connection.createStatement()) {
                stmt.executeUpdate("DELETE FROM " + tableName);
            }

            rowCount = SQLUtils.getRowCount(connection, null, tableName);
            assertEquals(0, rowCount);

            rowCount = SQLUtils.getRowCount(connection, SCHEMA_NAME, tableName);
            assertEquals(0, rowCount);
        }
    }

    @Test
    public void testTableColumnVerification() throws Exception {
        
        String tableName = "mytable";
        try ( Connection connection = dataSource.getConnection()) {
            try ( Statement stmt = connection.createStatement()) {
                stmt.executeUpdate("CREATE TABLE " + tableName + " (my_char_column VARCHAR(10), my_int_column INTEGER)");
                stmt.executeUpdate("CREATE TABLE " + SCHEMA_NAME + "." + tableName + " (my_char_column VARCHAR(10), my_int_column INTEGER)");
            }

            // Expected column exists in table (no schema)
            Assertions.assertDoesNotThrow(() -> {
                SQLUtils.tableColumnVerification(connection, null, tableName,
                        new SQLUtils.TableColumn[]{
                            new SQLUtils.TableColumn("my_char_column", java.sql.Types.VARCHAR, 5)
                        });
            });

            // Expected column exists in table (with schema)
            Assertions.assertDoesNotThrow(() -> {
                SQLUtils.tableColumnVerification(connection, SCHEMA_NAME, tableName,
                        new SQLUtils.TableColumn[]{
                            new SQLUtils.TableColumn("my_char_column", java.sql.Types.VARCHAR, 5)
                        });
            });
            
            // Expected column does not exist in table
            Assertions.assertThrows(SQLException.class, () -> {
                SQLUtils.tableColumnVerification(connection, null, tableName,
                        new SQLUtils.TableColumn[]{
                            new SQLUtils.TableColumn("my_char_column", java.sql.Types.VARCHAR, 5),
                            new SQLUtils.TableColumn("notexist", java.sql.Types.VARCHAR, 10)
                        });
            });
            
            // Expected char column exists and is of correct type but is not wide enough
            Assertions.assertThrows(SQLException.class, () -> {
                SQLUtils.tableColumnVerification(connection, null, tableName,
                        new SQLUtils.TableColumn[]{
                            new SQLUtils.TableColumn("my_char_column", java.sql.Types.VARCHAR, 12)
                        });
            });

            // Expected int column exists and is of int type but is not wide enough
            Assertions.assertThrows(SQLException.class, () -> {
                SQLUtils.tableColumnVerification(connection, null, tableName,
                        new SQLUtils.TableColumn[]{
                            new SQLUtils.TableColumn("my_int_column", java.sql.Types.BIGINT)
                        });
            });

        }
    }
    
    
}
