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
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;
import javax.sql.DataSource;
import net.lbruun.dbleaderelect.DatabaseEngine;
import net.lbruun.dbleaderelect.LeaderElectorConfiguration;
import net.lbruun.dbleaderelect.helpers.LiquibaseRunner;
import net.lbruun.dbleaderelect.helpers.SQLUtilsTestHelper;
import net.lbruun.dbleaderelect.internal.core.RowInLeaderElectionTable;
import net.lbruun.dbleaderelect.internal.sql.SQLCmds;
import net.lbruun.dbleaderelect.internal.utils.SQLUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.containers.JdbcDatabaseContainer;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.TestMethodOrder;
/**
 *
 */

@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public abstract class DatabaseWithTestContainers {
    
    private LiquibaseRunner liquibaseRunner;
    private HikariDataSource dataSource;
    private LeaderElectorConfiguration leaderElectorConfiguration;
    private String tmpTable;

    
    @BeforeEach
    public void setUp() throws SQLException {
        System.out.println("Executing @BeforeEach setUp()");
        HikariConfig config = new HikariConfig();
        DataSource specialDataSource = getSpecialDataSource();
        if (specialDataSource != null) {
            config.setDataSource(specialDataSource);
        } else {
            config.setJdbcUrl(getJdbcDatabaseContainer().getJdbcUrl());
            config.setUsername(getJdbcDatabaseContainer().getUsername());
            config.setPassword(getJdbcDatabaseContainer().getPassword());
            if (getDriverClassName() != null) {
                config.setDriverClassName(getDriverClassName());
            }
        }
        config.setConnectionTimeout(3000);

        dataSource = new HikariDataSource(config);
        dataSource.setLoginTimeout(3);
        liquibaseRunner = new LiquibaseRunner(dataSource);
        tmpTable = liquibaseRunner.getLeaderElectTableName();
        liquibaseRunner.execStart();
    }

    @AfterEach
    public void tearDown() {
        System.out.println("Executing @AfterEach tearDown()");
        if (liquibaseRunner != null) {
            liquibaseRunner.execEnd();
        }
        if (dataSource != null) {
            dataSource.close();
        }
    }

    public DataSource getSpecialDataSource() {
        return null;
    }

    public abstract JdbcDatabaseContainer getJdbcDatabaseContainer();
    
    public abstract DatabaseEngine getDatabaseEngineType();
    
    public DataSource getDataSource() {
        return dataSource;
    }
    
    public String getDriverClassName() {
        return null;
    }
    
    public abstract LeaderElectorConfiguration getLeaderElectorConfiguration(String schemaName, String tableName);
    
    @Test
    @Order(0)
    void testCapabilities() throws SQLException {
        System.out.println("Test: Database and JDBC Driver capabilities");
        
        try (Connection connection = getDataSource().getConnection()) {
            DatabaseMetaData meta = connection.getMetaData();
            
            System.out.println("JDBC Driver info");
            System.out.println("    DatabaseMetaData.getDriverName()                       : " + meta.getDriverName());
            System.out.println("    DatabaseMetaData.getDriverVersion()                    : " + meta.getDriverVersion());
            System.out.println("    DatabaseMetaData.getDatabaseMajorVersion()             : " + meta.getDatabaseMajorVersion());
            System.out.println("    DatabaseMetaData.getDatabaseMinorVersion()             : " + meta.getDatabaseMinorVersion());
            System.out.println("    DatabaseMetaData.getDatabaseProductName()              : " + meta.getDatabaseProductName());
            System.out.println("    DatabaseMetaData.getDatabaseProductVersion()           : " + meta.getDatabaseProductVersion());
            System.out.println("    DatabaseMetaData.getDriverMajorVersion()               : " + meta.getDriverMajorVersion());
            System.out.println("    DatabaseMetaData.getDriverMinorVersion()               : " + meta.getDriverMinorVersion());
            System.out.println("    DatabaseMetaData.getJDBCMajorVersion()                 : " + meta.getJDBCMajorVersion());
            System.out.println("    DatabaseMetaData.getJDBCMinorVersion()                 : " + meta.getJDBCMinorVersion());

            
            System.out.println("Schema and Catalog");
            System.out.println("    Connection.getCatalog()                                : " + connection.getCatalog());
            System.out.println("    Connection.getSchema()                                 : " + connection.getSchema());
            System.out.println("    DatabaseMetaData.getCatalogs()                         : " + meta.getCatalogs());
            System.out.println("    DatabaseMetaData.getCatalogSeparator()                 : " + meta.getCatalogSeparator());
            System.out.println("    DatabaseMetaData.getCatalogTerm()                      : " + meta.getCatalogTerm());
            System.out.println("    DatabaseMetaData.getSchemas()                          : " + meta.getSchemas());
            System.out.println("    DatabaseMetaData.getSchemaTerm()                       : " + meta.getSchemaTerm());
            System.out.println("    DatabaseMetaData.isCatalogAtStart()                    : " + meta.isCatalogAtStart());
            System.out.println("    DatabaseMetaData.supportsCatalogsInDataManipulation()  : " + meta.supportsCatalogsInDataManipulation());
            System.out.println("    DatabaseMetaData.supportsCatalogsInTableDefinitions()  : " + meta.supportsCatalogsInTableDefinitions());
            System.out.println("    DatabaseMetaData.supportsSchemasInDataManipulation()   : " + meta.supportsSchemasInDataManipulation());
            System.out.println("    DatabaseMetaData.supportsSchemasInTableDefinitions()   : " + meta.supportsSchemasInTableDefinitions());

            System.out.println("Lower-case vs upper-case");
            System.out.println("    DatabaseMetaData.storesLowerCaseIdentifiers()          : " + meta.storesLowerCaseIdentifiers());
            System.out.println("    DatabaseMetaData.storesLowerCaseQuotedIdentifiers()    : " + meta.storesLowerCaseQuotedIdentifiers());
            System.out.println("    DatabaseMetaData.storesMixedCaseIdentifiers()          : " + meta.storesMixedCaseIdentifiers());
            System.out.println("    DatabaseMetaData.storesMixedCaseQuotedIdentifiers()    : " + meta.storesMixedCaseQuotedIdentifiers());
            System.out.println("    DatabaseMetaData.storesUpperCaseIdentifiers()          : " + meta.storesUpperCaseIdentifiers());
            System.out.println("    DatabaseMetaData.storesUpperCaseQuotedIdentifiers()    : " + meta.storesUpperCaseQuotedIdentifiers());
            System.out.println("    DatabaseMetaData.supportsMixedCaseIdentifiers()        : " + meta.supportsMixedCaseIdentifiers());
            System.out.println("    DatabaseMetaData.supportsMixedCaseQuotedIdentifiers()  : " + meta.supportsMixedCaseQuotedIdentifiers());
            System.out.println("    DatabaseMetaData.getIdentifierQuoteString()            : " + meta.getIdentifierQuoteString());
                        
            System.out.println("Capabilities");
            System.out.println("    DatabaseMetaData.getDefaultTransactionIsolation()      : " + meta.getDefaultTransactionIsolation());
            System.out.println("    DatabaseMetaData.getSQLStateType()                     : " + meta.getSQLStateType());
            System.out.println("    DatabaseMetaData.supportsANSI92EntryLevelSQL()         : " + meta.supportsANSI92EntryLevelSQL());
            System.out.println("    DatabaseMetaData.supportsANSI92FullSQL()               : " + meta.supportsANSI92FullSQL());
            System.out.println("    DatabaseMetaData.supportsANSI92IntermediateSQL()       : " + meta.supportsANSI92IntermediateSQL());
            System.out.println("    DatabaseMetaData.supportsColumnAliasing()              : " + meta.supportsColumnAliasing());
            System.out.println("    DatabaseMetaData.supportsSelectForUpdate()             : " + meta.supportsSelectForUpdate());
            System.out.println("    DatabaseMetaData.supportsStatementPooling()            : " + meta.supportsStatementPooling());
            System.out.println("    DatabaseMetaData.supportsTransactions()                : " + meta.supportsTransactions());
        }
    }
    
    /**
     * Tests INSERT statement syntax and if it correctly handles the
     * 'duplicate key' scenario. It should simply ignore the INSERT request
     * and if a row with such key already exist.
     */
    @Test
    @Order(1)
    void testInsertRoleSQL() throws SQLException {
        System.out.println("Test: InsertRoleSQL");
        LeaderElectorConfiguration config = getLeaderElectorConfiguration(null, tmpTable);
        SQLCmds sqlTexts = SQLCmds.getSQL(config);
        
        try (Connection connection = getDataSource().getConnection()) {
            try (PreparedStatement pstmt = sqlTexts.getInsertRoleStmt(connection, config.getRoleId())) {
                // First INSERT must succeed
                pstmt.execute();

                // Second INSERT (with exact same values) must be ignored
                // (not throw errors)
                pstmt.execute();
            }
        }
    }


    /**
     * Tests database expression for 'milliseconds after epoch'. Test
     * is done by comparing to System.currentTimeMillis. This will work as long
     * as the test and the database server take their time from the same source.
     * @throws SQLException 
     */
    @Test
    @Order(2)
    void testDbTime() throws SQLException {
        System.out.println("Test: dbTime");
        LeaderElectorConfiguration config = getLeaderElectorConfiguration(null, tmpTable);
        SQLCmds sqlTexts = SQLCmds.getSQL(config);
        
        long dbNow;
        
        try (Connection connection = getDataSource().getConnection()) {
            try ( PreparedStatement pstmt = sqlTexts.getDbTimeUTCMillisStmt(connection)) {
                // Repeat 3x to make sure that statement is 'warmed up' in the db
                dbNow = getLongFromRs(pstmt.executeQuery());
                dbNow = getLongFromRs(pstmt.executeQuery());
                dbNow = getLongFromRs(pstmt.executeQuery());
            }
        }
        
        long now = System.currentTimeMillis();
        System.out.println("   Now (java time) : " + now);
        System.out.println("   Now (db time)   : " + dbNow);
        long timeDiff = Math.abs(now - dbNow);
        System.out.println("   Diff            : " + timeDiff);
        
        // When running on Windows using Docker Desktop for Windows you may
        // experience the clock inside Docker to be out-of-sync with the clock
        // on your workstation. This happens in particular after sleep and hibernate
        // of your workstation. It is a problem of the WSL2 which is used by Docker Desktop
        // rather then the Docker Desktop itself. 
        // The fix is to restart the WSL, doing 'wsl.exe --shutdown' from command line (CMD.EXE) 
        // followed by a complete restart of the Docker Desktop application.
        assertTrue(timeDiff < 100);
    }
    
    /**
     * Tests SELECT statement syntax.
     */
    @Test
    @Order(3)
    void testSelectSQL() throws SQLException {
        System.out.println("Test: SelectSQL");
        LeaderElectorConfiguration config = getLeaderElectorConfiguration(null, tmpTable);
        SQLCmds sqlTexts = SQLCmds.getSQL(config);
        
        RowInLeaderElectionTable row = null;
        int noOfRows = -1;
        try (Connection connection = getDataSource().getConnection()) {

            // Insert row so that following SELECT can proceeed            
            try ( PreparedStatement pstmt = sqlTexts.getInsertRoleStmt(connection, config.getRoleId())) {
                pstmt.executeUpdate();
            }
            
            try (PreparedStatement pstmt = sqlTexts.getSelectStmt(connection, config.getRoleId())) {
                if (pstmt.execute()) {
                    noOfRows = 0;
                    ResultSet resultSet = pstmt.getResultSet();
                    while(resultSet.next()) {
                        noOfRows++;
                        row = new RowInLeaderElectionTable(resultSet, config.getCandidateId());
                    }
                }
            }
        }
        
        assertEquals(1, noOfRows);
        assertNotNull(row);
        assertEquals(RowInLeaderElectionTable.CurrentLeaderDbStatus.NOBODY, row.getCurrentLeaderDbStatus());
                
    }


    /**
     * Tests if the database type is correctly discovered.
     */
    @Test
    @Order(4)
    public void testDatabaseTypeDetection() throws SQLException   {
        System.out.println("Test: DatabaseTypeDetection");
        try ( Connection connection = getDataSource().getConnection()) {
            DatabaseEngine databaseEngineType = DatabaseEngine.getDatabaseEngineFromConnection(connection);
            assertNotNull(databaseEngineType);
            assertEquals(getDatabaseEngineType(), databaseEngineType);
        }
    }


    /**
     * Tests if the database type is correctly discovered.
     */
    @Test
    @Order(5)
    public void testCreateTable() throws SQLException   {
        System.out.println("Test: CreateTable");
        LeaderElectorConfiguration config = LeaderElectorConfiguration.builder()
                .withTableName("tab_create_test_table")
                .withDatabaseEngine(getDatabaseEngineType())
                .withCreateTable(true)
                .build();

        SQLCmds sqlCmds = SQLCmds.getSQL(config);
        try ( Connection connection = getDataSource().getConnection()) {
            if (!SQLUtils.tableExists(connection, config.getSchemaName(), config.getTableName())) {
                System.out.println("Creating table " + sqlCmds.getTabName());
                try (PreparedStatement stmt = sqlCmds.getCreateTableStmt(connection)) {
                    stmt.execute();
                }
                System.out.println("Table " + sqlCmds.getTabName() + " created.");
                
                SQLUtilsTestHelper.dropTable(connection, config.getSchemaName(), config.getTableName());
            }
        }
    }

    
    @Test
    @Order(7)
    public void testTableAlreadyExistIgnore() throws SQLException  {
        System.out.println("Test: TableAlreadyExistIgnore");
        LeaderElectorConfiguration config = LeaderElectorConfiguration.builder()
                .withTableName("tab_create_test_table_x")
                .withDatabaseEngine(getDatabaseEngineType())
                .withCreateTable(true)
                .build();

        SQLCmds sqlCmds = SQLCmds.getSQL(config);
        try ( Connection connection = getDataSource().getConnection()) {
            if (!SQLUtils.tableExists(connection, config.getSchemaName(), config.getTableName())) {
                System.out.println("Creating table " + sqlCmds.getTabName());
                try (PreparedStatement stmt = sqlCmds.getCreateTableStmt(connection)) {
                    stmt.execute();
                }
                System.out.println("Table " + sqlCmds.getTabName() + " created.");
                System.out.println("Creating table " + sqlCmds.getTabName());

                try (PreparedStatement stmt = sqlCmds.getCreateTableStmt(connection)) {
                    stmt.execute();
                } catch (SQLException ex) {
                    assertTrue(sqlCmds.isTableAlreadyExistException(ex));
                }                
                SQLUtilsTestHelper.dropTable(connection, config.getSchemaName(), config.getTableName());
            }
        }
    }
    
    /**
     * Tests if the "SELECT FOR UPDATE" (or similar) is truly only letting one database
     * session in at a time. This is meant to test if the locking mechanism in
     * the database behaves as expected.
     *
     * <p>
     * This is not a perfect test. But close enough.
     */
    @Test
    @Order(80)
    public void testDatabaseExclusiveRowLevelLock() throws SQLException  {
        System.out.println("testSelectForUpdateMany");
        final LeaderElectorConfiguration config = getLeaderElectorConfiguration(null, tmpTable);
        final SQLCmds sqlTexts = SQLCmds.getSQL(config);
        final AtomicLong counter = new AtomicLong(0);


        // Insert row so that following SELECT can proceeed. Without a row
        // we have nothing to lock upon.
        try ( Connection connection = getDataSource().getConnection()) {
            try ( PreparedStatement pstmt = sqlTexts.getInsertRoleStmt(connection, config.getRoleId())) {
                pstmt.executeUpdate();
            }
        }

        assertAll(() -> {
            final ReentrantLock lock = new ReentrantLock();
            int noOfThreads = 6;
            int noOfIterationsPerThread = 40;
            Thread[] threads = new Thread[noOfThreads];
            for (int i = 0; i < noOfThreads; i++) {
                Thread thread = new Thread(runSelectForUpdateIter(i + 1, counter, getDataSource(), config, sqlTexts, lock, noOfIterationsPerThread));
                thread.setUncaughtExceptionHandler((Thread th, Throwable ex) -> {
                    ex.printStackTrace();
                    throw new RuntimeException(ex);
                });
                thread.start();
                threads[i] = thread;
            }
            // Wait for all threads to finish
            for (Thread t : threads) {
                try {
                    t.join();
                } catch (InterruptedException ex) {
                    throw new RuntimeException(ex);
                }
            }
        });
    }
    
    
    private Runnable runSelectForUpdateIter(
            int threadNo, 
            AtomicLong counter,
            final DataSource dataSource, 
            final LeaderElectorConfiguration config,
            final SQLCmds sqlCmds,
            final ReentrantLock lock, 
            final int iterations) {
        return () -> {
            System.out.println("Thread-" + threadNo + " starting.");
            String candidateId = "candidate-" + threadNo;
            try ( Connection connection = dataSource.getConnection()) {
                boolean originalAutoCommit = connection.getAutoCommit();
                connection.setAutoCommit(false);
                try (
                         PreparedStatement selectStmt = sqlCmds.getSelectStmt(connection, config.getRoleId());  
                         PreparedStatement updateStmt = sqlCmds.getAssumeLeadershipStmt(connection, config.getRoleId(), candidateId, 0);
                        ) {
                    for (int i = 0; i < iterations; i++) {
                        updateStmt.setLong(2, counter.incrementAndGet());
                        System.out.println("Thread-" + threadNo + " . Execution no: " + (i + 1));
                        executeSelectWithExclusiveRowLock(connection, candidateId, selectStmt, updateStmt, lock);
                    }
                }
                connection.setAutoCommit(originalAutoCommit);
            } catch (SQLException ex) {
                throw new RuntimeException(ex);
            }
        };
    }

    private void executeSelectWithExclusiveRowLock(Connection connection, String candidateId, PreparedStatement selectStmt, PreparedStatement updateStmt, ReentrantLock lock) throws SQLException {
        boolean wasLocked = false;
        try ( ResultSet rs = selectStmt.executeQuery()) {
            // Is someone else already in here?
            // We should be the only ones here!!
            if (lock.isLocked()) {
                throw new RuntimeException("Lock is already held. This is unexpected. It means some other thread is also here.");
            } else {
                lock.lock();
                wasLocked = true;
            }
            if (!rs.next()) {
                throw new RuntimeException("Table is empty. This is unexpected.");
            } else {
                RowInLeaderElectionTable rowInLeaderElectionTable = new RowInLeaderElectionTable(rs, candidateId);
                System.out.println("Row read : " + rowInLeaderElectionTable);
                // Execute an UPDATE while holding the row lock. This must succeed.
                int rowsAffected = updateStmt.executeUpdate();
                if (rowsAffected != 1) {
                    throw new RuntimeException("Rows affected by update different than 1 (one).");
                }
            }
        } catch (SQLException ex) {
            int errorCode = ex.getErrorCode();
            String sqlState = ex.getSQLState();
            ex.printStackTrace();
            throw ex;
        } finally {
            if (wasLocked) {
                lock.unlock();
            }
            connection.commit();
        }
    }

    
    
    
    
    private long getLongFromRs(ResultSet rs) throws SQLException {
        while (rs.next()) {
            return rs.getLong(1);
        }
        return -1;
    }
    
}
