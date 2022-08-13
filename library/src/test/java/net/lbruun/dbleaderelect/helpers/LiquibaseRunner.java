/*
 * Copyright 2021 lars.
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
import java.util.Random;
import javax.sql.DataSource;
import liquibase.Contexts;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.LiquibaseException;
import liquibase.resource.ClassLoaderResourceAccessor;

/**
 *
 */
public class LiquibaseRunner {

    private static final String LIQUIBASE_CHGLOG_DEFAULT_LOCATION = "net/lbruun/dbleaderelect/liquibase/db.changelog_db_leader_elect.yaml";
    private final String changeLogClasspathLocation;
    private final DataSource dataSource;
    private Database database;
    private final String leaderElectTableName;
            

    public LiquibaseRunner(String changeLogClasspathLocation, DataSource dataSource, String tableName) {
        this.changeLogClasspathLocation = changeLogClasspathLocation;
        this.dataSource = dataSource;
        this.leaderElectTableName = tableName;
    }
    
    public LiquibaseRunner(DataSource dataSource, String tableName) {
        this(LIQUIBASE_CHGLOG_DEFAULT_LOCATION, dataSource, tableName);
    }

    public LiquibaseRunner(DataSource dataSource) {
        this(LIQUIBASE_CHGLOG_DEFAULT_LOCATION, dataSource, getTmpTableName());
    }

    public String getLeaderElectTableName() {
        return leaderElectTableName;
    }
    
    
    public synchronized void  execStart() {
        
        try ( Connection connection = dataSource.getConnection()) {
            database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(connection));
            database.setDatabaseChangeLogLockTableName(getTmpTableName());
            database.setDatabaseChangeLogTableName(getTmpTableName());
            Liquibase liquibase = new liquibase.Liquibase(changeLogClasspathLocation, new ClassLoaderResourceAccessor(), database);
            liquibase.setChangeLogParameter("dbleaderelect.tablename", leaderElectTableName);
            liquibase.setChangeLogParameter("dbleaderelect.schemaname", connection.getSchema());
            liquibase.update((Contexts) null);
        } catch (SQLException | LiquibaseException ex) {
            throw new RuntimeException(ex);
        }
    }


    public synchronized void execEnd() {
        if (database == null) {
            throw new RuntimeException("execStart() hasn't been executed");
        }
        String databaseChangeLogLockTableName = database.getDatabaseChangeLogLockTableName();
        String databaseChangeLogTableName = database.getDatabaseChangeLogTableName();
        String liquibaseSchemaName = database.getLiquibaseSchemaName();
        try ( Connection connection = dataSource.getConnection()) {
            SQLUtils.dropTable(connection, liquibaseSchemaName, databaseChangeLogLockTableName);
            SQLUtils.dropTable(connection, liquibaseSchemaName, databaseChangeLogTableName);
            SQLUtils.dropTable(connection, null, leaderElectTableName);
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
    }
    
    
    public static String getTmpTableName() {
        Random random = new Random();
        return "tmp_" 
                + System.currentTimeMillis() 
                + "_"
                +  Math.abs(random.nextLong());
    }

}
