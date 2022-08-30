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
package net.lbruun.dbleaderelect.internal.utils;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.JDBCType;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import javax.sql.DataSource;

public class SQLUtils {

    private SQLUtils() {
    }
    
    
    /**
     * Verifies if table exists by lookup in the database's internal dictionary.
     * This is normally very fast.
     * @param dataSource
     * @param schemaName schema name, optional, current schema is used if not specified
     * @param tableName table name, mandatory, not qualified
     * @return true if the table exists
     * @throws SQLException 
     * @see #tableExists(java.sql.Connection, java.lang.String, java.lang.String) 
     */
    public static boolean tableExists(DataSource dataSource, String schemaName, String tableName) throws SQLException {
        Objects.requireNonNull(dataSource, "dataSource cannot be null");
        try (Connection connection = dataSource.getConnection()) {
            return tableExists(connection, schemaName, tableName);
        }
    }
    
    /**
     * Verifies if table exists by lookup in the database's internal dictionary.
     * This is normally very fast.
     * 
     * @param connection
     * @param schemaName schema name, optional, current schema is used if not specified
     * @param tableName table name, mandatory, not qualified
     * @return true if the table exists
     * @throws SQLException 
     * @see #tableExists(javax.sql.DataSource, java.lang.String, java.lang.String) 
     */
    public static boolean tableExists(Connection connection, String schemaName, String tableName) throws SQLException {
        Objects.requireNonNull(connection, "connection cannot be null");
        Objects.requireNonNull(tableName, "tableName cannot be null");
        
        DatabaseMetaData meta = connection.getMetaData();
        
        String s = objectNameNormalisation(meta, schemaName);
        String t = objectNameNormalisation(meta, tableName);
        try ( ResultSet resultSet = meta.getTables(null, s, t, new String[]{"TABLE"})) {
            return resultSet.next();
        }
    }
    
    
    public static void tableColumnVerification(DataSource dataSource, String schemaName, String tableName, TableColumn[] expectedColumns) throws SQLException {
        Objects.requireNonNull(dataSource, "dataSource cannot be null");
        try ( Connection connection = dataSource.getConnection()) {
            tableColumnVerification(connection, schemaName, tableName, expectedColumns);
        }
    }
    
    /**
     * Verifies if expected columns exist on the table. Throws {@code SQLException} is not.
     * @param connection
     * @param schemaName name of schema, may be null for the default schema for the connection
     * @param tableName name of table
     * @param expectedColumns
     * @throws SQLException 
     */
    public static void tableColumnVerification(Connection connection, String schemaName, String tableName, TableColumn[] expectedColumns) throws SQLException {
        Objects.requireNonNull(connection, "connection cannot be null");
        Objects.requireNonNull(tableName, "tableName cannot be null");
        Objects.requireNonNull(expectedColumns, "expectedColumns cannot be null");

        DatabaseMetaData meta = connection.getMetaData();
        ColumnComparison columnComparison = getColumnComparison(meta);
        String tName = (schemaName != null) ? schemaName + "." + tableName : tableName;
        String s = objectNameNormalisation(meta, schemaName);
        String t = objectNameNormalisation(meta, tableName);
        Map<String, TableColumn> columnsInTable = new HashMap<>();
        try (ResultSet resultSet = meta.getColumns(null, s, t, null)) {
            while (resultSet.next()) {
                String columnName = resultSet.getString(4); 
                if (columnComparison == ColumnComparison.LOWERCASE) {
                    columnName = columnName.toLowerCase(Locale.US);
                } else if (columnComparison == ColumnComparison.UPPERCASE) {
                    columnName = columnName.toUpperCase(Locale.US);
                }
                int sqlType = resultSet.getInt(5);
                TableColumn column;
                if (sqlType == java.sql.Types.VARCHAR) {
                    column = new TableColumn(columnName, sqlType, resultSet.getInt(7));
                } else {
                    column = new TableColumn(columnName, sqlType);
                }
                columnsInTable.put(columnName, column);
            }
        }
        
        for(TableColumn expectedColumn : expectedColumns) {
            String c = objectNameNormalisation(meta, expectedColumn.getColumnName());
            TableColumn columnInTable = columnsInTable.get(c);
            if (columnInTable == null) {
                throw new SQLException("Verification error for table " + tName + " : column + " + expectedColumn.getColumnName() + " not found");
            }
            try {
                columnInTable.compareTo(expectedColumn);
            } catch (SQLException ex) {
                throw new SQLException("Verification error for table " + tName, ex);
            }
        }
    }
    
    
    /**
     * Gets row count for table.
     * For very large tables this may be a lengthy operation.
     * @param dataSource
     * @param schemaName schema name, optional, current schema is used if not specified
     * @param tableName table name, mandatory, not qualified
     * @return row count
     * @throws SQLException 
     * @see #getRowCount(java.sql.Connection, java.lang.String, java.lang.String) 
     */
    public static long getRowCount(DataSource dataSource, String schemaName, String tableName) throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            return getRowCount(connection, schemaName, tableName);
        }
    }

    /**
     * Gets row count for table.
     * For very large tables this may be a lengthy operation.
     * @param connection
     * @param schemaName schema name, optional, current schema is used if not specified
     * @param tableName table name, mandatory, not qualified
     * @return row count
     * @throws SQLException 
     * @see #getRowCount(javax.sql.DataSource, java.lang.String, java.lang.String) 
     */
    public static long getRowCount(Connection connection, String schemaName, String tableName) throws SQLException {
        String schemaPrefix = (schemaName == null)? "" : schemaName + ".";
        String sql = "SELECT COUNT(*) AS no_of_rows FROM " + schemaPrefix + tableName;
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            try (ResultSet resultSet = pstmt.executeQuery()) {
                resultSet.next();
                return resultSet.getLong("no_of_rows");
            }
        }
    }

    

    private static String objectNameNormalisation(DatabaseMetaData meta, String dbObjectName) throws SQLException {
        Objects.requireNonNull(meta, "meta cannot be null");
        if (dbObjectName == null) {
            return null;
        }
        if (meta.storesLowerCaseIdentifiers()) {
            return dbObjectName.toLowerCase(Locale.US);
        } else if (meta.storesUpperCaseIdentifiers()) {
            return dbObjectName.toUpperCase(Locale.US);
        }
        return dbObjectName;
    }

    /**
     * Gets the rule by which two column names can be compared for identity.
     * <ul>
     *   <li>{@code LOWERCASE}. The identifiers must converted to lowercase before they
     *            can be compared.</li>
     *   <li>{@code UPPERCASE}. The identifiers must converted to lowercase before they
     *            can be compared.</li>
     *   <li>{@code EXACT}. The identifiers must compared as-is.</li>
     * </ul>
     * 
     * @param meta
     * @return
     * @throws SQLException 
     */
    private static ColumnComparison getColumnComparison(DatabaseMetaData meta) throws SQLException {
        if (meta.storesLowerCaseIdentifiers()) {
            return ColumnComparison.LOWERCASE;
        } else if (meta.storesUpperCaseIdentifiers()) {
            return ColumnComparison.UPPERCASE;
        } else if (meta.supportsMixedCaseIdentifiers()) {
            return ColumnComparison.EXACT;
        }
        throw new RuntimeException("Unexpected error. Column case rules for database cannot be determined.");
    }
    
    public static class TableColumn {
        private final String columnName;
        private final int sqlType;
        private final int columnSize;

        public TableColumn(String columnName, int sqlType, int columnSize) {
            this.columnName = columnName;
            this.sqlType = sqlType;
            this.columnSize = columnSize;
        }
        public TableColumn(String columnName, int sqlType) {
            this(columnName, sqlType, -1);
        }

        public String getColumnName() {
            return columnName;
        }

        public int getSqlType() {
            return sqlType;
        }

        public int getColumnSize() {
            return columnSize;
        }
        
        public void compareTo(TableColumn expected) throws SQLException {
            String errPrefix = "Column definition for " + this.getColumnName() + " is incorrect : ";
            if (this.getSqlType() != expected.getSqlType()) {
                JDBCType thisType = JDBCType.valueOf(this.getSqlType());
                JDBCType expectedType = JDBCType.valueOf(expected.getSqlType());
                if (isIntegerType()) {
                    if (!isIntegerTypeCompatible(expected.getSqlType())) {
                        throw new SQLException(errPrefix + " is an integer type but isn't large enough. Must be at least of JDBC Type " + expectedType);
                    }
                } else {
                    throw new SQLException(errPrefix + "is of JDBC Type " + thisType + " but is expected to be of JDBC Type " + expectedType);
                }
            }
            
            if (this.getSqlType() == java.sql.Types.VARCHAR) {
                if (this.getColumnSize() < expected.getColumnSize()) {
                    throw new SQLException(errPrefix + "is expected to be able to store at least " + expected.getColumnSize() + " characters");
                }
            }
        }
        
        public boolean isIntegerTypeCompatible(int expected) {
            if (!isIntegerType()) {
                return false;
            }
            if (expected == java.sql.Types.TINYINT) {
                return true;
            }
            if (expected == java.sql.Types.SMALLINT) {
                return (this.getSqlType() != java.sql.Types.TINYINT);
            }
            if (expected == java.sql.Types.INTEGER) {
                return (this.getSqlType() == java.sql.Types.INTEGER) || (this.getSqlType() == java.sql.Types.BIGINT);
            }
            if (expected == java.sql.Types.BIGINT) {
                return (this.getSqlType() == java.sql.Types.BIGINT);
            }
            return false;
        }
        
        public boolean isIntegerType() {
            return ((this.getSqlType() == java.sql.Types.TINYINT) ||
                    (this.getSqlType() == java.sql.Types.SMALLINT) ||
                    (this.getSqlType() == java.sql.Types.INTEGER) ||
                    (this.getSqlType() == java.sql.Types.BIGINT));
        }

        @Override
        public int hashCode() {
            int hash = 5;
            hash = 89 * hash + Objects.hashCode(this.columnName);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final TableColumn other = (TableColumn) obj;
            if (!Objects.equals(this.columnName, other.columnName)) {
                return false;
            }
            return true;
        }
        
        
    }
    
    private static enum ColumnComparison {
        LOWERCASE,
        UPPERCASE,
        EXACT
    }
}