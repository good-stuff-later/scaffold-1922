/*
 * Copyright 2021 lbruun
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
package net.lbruun.dbleaderelect.helpers;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import javax.sql.DataSource;
import net.lbruun.dbleaderelect.internal.utils.SQLUtils;

public class SQLUtilsTestHelper {

    private SQLUtilsTestHelper() {
    }
    
    public static void dropTable(DataSource dataSource, String schemaName, String tableName) throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            dropTable(connection, schemaName, tableName);
        }
    }
    
    public static void dropTable(Connection connection, String schemaName, String tableName) throws SQLException {
        try ( Statement statement = connection.createStatement()) {
            if (SQLUtils.tableExists(connection, schemaName, tableName)) {
                String schemaPrefix = (schemaName != null) ? (schemaName + ".") : "";
                System.out.println("Dropping table " + schemaPrefix + tableName);
                statement.execute("DROP TABLE " + schemaPrefix + tableName);
            }
        }
    }
    
//    
//    public static boolean tableExists(DataSource dataSource, String schemaName, String tableName) throws SQLException {
//        try (Connection connection = dataSource.getConnection()) {
//            return tableExists(connection, schemaName, tableName);
//        }
//    }
//    
//    public static boolean tableExists(Connection connection, String schemaName, String tableName) throws SQLException {
//        DatabaseMetaData meta = connection.getMetaData();
//        try ( ResultSet resultSet = meta.getTables(null, schemaName, tableName, new String[]{"TABLE"})) {
//            return resultSet.next();
//        }
//    }
//    
}
