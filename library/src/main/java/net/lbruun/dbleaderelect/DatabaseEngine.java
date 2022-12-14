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
package net.lbruun.dbleaderelect;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.Locale;

/**
 * Type of database.
 */
public enum DatabaseEngine {
    
    /**
     * PostgreSQL
     */
    POSTGRESQL,
    
    /**
     * MySQL
     */
    MYSQL,
    
    /**
     * MariaDB
     */
    MARIADB,
    
    /**
     * Oracle Database
     */
    ORACLE,
    
    /**
     * Microsoft SQL Server
     */
    MSSQL,
    
    /**
     * H2 Database Engine
     */
    H2,
    
    /**
     * IBM Db2 for Linux, Unix and Windows
     */
    DB2_LUW;
    
    /**
     * Determines the database engine type from a database connection.
     * @param connection
     * @return database engine type
     * @throws SQLException if the engine type cannot be determined or
     *     if there was some other problem with the connection.
     */
    public static DatabaseEngine getDatabaseEngineFromConnection(Connection connection) throws SQLException {
        DatabaseMetaData metaData = connection.getMetaData();

        String databaseProductName = metaData.getDatabaseProductName();
        String databaseProductNameLower = databaseProductName.toLowerCase(Locale.US);
        String databaseProductVersion = metaData.getDatabaseProductVersion();

        // Be careful about the order below. Use most specific first.
        if (databaseProductName.startsWith("Microsoft SQL Server")) {
            return MSSQL;
        } else if (databaseProductName.startsWith("PostgreSQL")) {
            return POSTGRESQL;
        } else if (databaseProductName.startsWith("MySQL")) {
            return MYSQL;
        } else if (databaseProductName.startsWith("MariaDB")) {
            return MARIADB;
        } else if (databaseProductName.startsWith("Oracle")) {
            return ORACLE;
        } else if (databaseProductName.startsWith("H2")) {
            return H2;
        } else if (databaseProductNameLower.equals("db2")
                || databaseProductNameLower.matches("db2[.-/ ].*")) {
            if (databaseProductVersion != null && databaseProductVersion.length() > 3) {
                String ppp = databaseProductVersion.substring(0, 3);
                if (ppp.equals("SQL")) {
                    return DB2_LUW;  // Db2 on Linux, UNIX, and Windows systems
                }
            }
        }
        throw new SQLException("Unknown database engine: " + databaseProductName);
    }
}
