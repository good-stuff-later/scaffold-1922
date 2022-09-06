/*
 * Copyright 2022 lars.
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
package net.lbruun.dbleaderelect.internal.sql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import net.lbruun.dbleaderelect.LeaderElectorConfiguration;
import net.lbruun.dbleaderelect.DatabaseEngine;
import net.lbruun.dbleaderelect.LeaderElector;
import static net.lbruun.dbleaderelect.LeaderElector.NO_LEADER_CANDIDATE_ID;
import net.lbruun.dbleaderelect.exception.LeaderElectorPreFlightException;

/**
 * Database engine abstraction.
 */
public abstract class SQLCmds {
    
    // This works with most databases, but not for example MS SQL Server
    private static final String DEFAULT_SQL_SELECT_TEMPLATE
            = "SELECT "
            + "    candidate_id," // #1
            + "    last_seen_timestamp," // #2
            + "    %s as now_utc_ms," // #3
            + "    lease_counter" // #4
            + " FROM %s"
            + " WHERE role_id = ?"
            + " FOR UPDATE";
    private static final String SQL_AFFIRM_LEADERSHIP_TEMPLATE
            = "UPDATE %s"
            + " SET  last_seen_timestamp = %s"
            + " WHERE role_id = ?"
            + " AND candidate_id = ?";
    private static final String SQL_ASSUME_LEADERSHIP_TEMPLATE
            = "UPDATE %s"
            + " SET candidate_id = ? ,"
            + "     last_seen_timestamp = %s ,"
            + "     lease_counter = ?"
            + " WHERE role_id = ?";
    private static final String SQL_RELINQUISH_LEADERSHIP_TEMPLATE
            = "UPDATE %s"
            + " SET candidate_id = '" + NO_LEADER_CANDIDATE_ID + "'"
            + "    ,last_seen_timestamp = 0"
            + " WHERE role_id = ?"
            + " AND candidate_id = ?";

    private static final String SQL_CREATE_TABLE_TEMPLATE
            = "CREATE TABLE %1$s"
            + " ("
            + "   role_id               %2$s(20)    NOT NULL," 
            + "   candidate_id          %2$s(256)   NOT NULL," 
            + "   last_seen_timestamp   %3$s        NOT NULL," 
            + "   lease_counter         %3$s        NOT NULL," 
            + "   PRIMARY KEY (role_id)" 
            + "  )";
    
    public static final String COLUMN_LIST_FOR_INSERT
            = "(role_id, candidate_id, last_seen_timestamp, lease_counter)";
    public static final String VALUES_LIST_FOR_INSERT
            = "( ?, '" + LeaderElector.NO_LEADER_CANDIDATE_ID + "'," + LeaderElector.NO_LEADER_LASTSEENTIMESTAMP_MS + ", 0)";

    
    private final String createTableSQL;
    private final String selectSQL;
    private final String affirmLeadershipSQL;
    private final String assumeLeadershipSQL;
    private final String relinquishLeadershipSQL;
    private final String tabName;
    private final String tabNamePlain;

    public SQLCmds(LeaderElectorConfiguration configuration) {
        this.tabName = (configuration.getSchemaName() != null) 
                ? configuration.getSchemaName() + "." + configuration.getTableName() 
                : configuration.getTableName();
        this.tabNamePlain = configuration.getTableName();
        this.createTableSQL = String.format(SQL_CREATE_TABLE_TEMPLATE, this.tabName, this.getVarcharStr(), this.getBigIntStr());
        this.selectSQL = String.format(DEFAULT_SQL_SELECT_TEMPLATE, currentUtcMsExpression(),this.tabName);
        this.affirmLeadershipSQL = String.format(SQL_AFFIRM_LEADERSHIP_TEMPLATE, this.tabName, currentUtcMsExpression());
        this.assumeLeadershipSQL = String.format(SQL_ASSUME_LEADERSHIP_TEMPLATE, this.tabName, currentUtcMsExpression());
        this.relinquishLeadershipSQL = String.format(SQL_RELINQUISH_LEADERSHIP_TEMPLATE, this.tabName);
    }

    
    public static SQLCmds getSQL(LeaderElectorConfiguration configuration) {
        DatabaseEngine databaseEngine = configuration.getDatabaseEngine();
        switch (databaseEngine) {
            case POSTGRESQL:
                return new SQLCmdsPostgreSQL(configuration);
            case MYSQL:
            case MARIADB:    
                return new SQLCmdsMySQL(configuration);
            case ORACLE:    
                return new SQLCmdsOracle(configuration);
            case MSSQL:    
                return new SQLCmdsMSSQL(configuration);
            case H2:    
                return new SQLCmdsH2(configuration);
            case DB2_LUW:
                return new SQLCmdsDb2(configuration);
            default:
                throw new LeaderElectorPreFlightException("Support for " + databaseEngine + " not yet implemented");
        }
    }

    
    /**
     * Database expression which returns the current number of milliseconds from
     * the 1970-01-01 epoch UTC where each day is exactly 86400000 milliseconds and
     * leap seconds are not taken into account. This is the equivalent of Java's
     * System.currentTimeMillis() but by letting the database decide this we
     * effectively use a single clock across all candidates.
     */
    protected abstract String currentUtcMsExpression();
    
    /**
     * Text to append to append to a SELECT expression when the goal 
     * is simply to retrieve a value from the database's internal functions,
     * rather than from a table. For Oracle this will be "FROM DUAL" whereas
     * for most databases it will be an empty string;
     */
    public String fromNothingString() {
        return "";
    }
    
    public String getCreateTableSQL() {
        return createTableSQL;
    }

    public String getAffirmLeadershipSQL() {
        return affirmLeadershipSQL;
    }

    public String getAssumeLeadershipSQL() {
        return assumeLeadershipSQL;
    }

    public String getRelinquishLeadershipSQL() {
        return relinquishLeadershipSQL;
    }

    public String getSelectSQL() {
        return selectSQL;
    }
    
    public String getBigIntStr() {
        return "bigint";
    }

    public String getVarcharStr() {
        return "varchar";
    }
   
    public boolean isTableAlreadyExistException(SQLException ex) {
        return false;
    }
    /**
     * Statement which returns a single row with a single column with value
     * which is the milliseconds since epoch as derived from the time in the
     * database server.
     */
    public PreparedStatement getDbTimeUTCMillisStmt(Connection connection) throws SQLException {
        return  connection.prepareStatement("SELECT " + currentUtcMsExpression() + " " + fromNothingString());
    }

    public PreparedStatement getCreateTableStmt(Connection connection) throws SQLException {
        return connection.prepareStatement(getCreateTableSQL());
    }
    
    public PreparedStatement getSelectStmt(Connection connection, String roleId) throws SQLException {
        PreparedStatement pstmt = connection.prepareStatement(getSelectSQL());
        pstmt.setString(1, roleId);
        return pstmt;
    }
    
    public PreparedStatement getAffirmLeadershipStmt(Connection connection, String roleId, String candidateId)
            throws SQLException {
        PreparedStatement pstmt = connection.prepareStatement(getAffirmLeadershipSQL());
        pstmt.setString(1, roleId);
        pstmt.setString(2, candidateId);
        return pstmt;
    }

    public PreparedStatement getAssumeLeadershipStmt(Connection connection, String roleId, String candidateId, long newLeaseCounter)
            throws SQLException {
        PreparedStatement pstmt = connection.prepareStatement(getAssumeLeadershipSQL());
        pstmt.setString(1, candidateId);
        pstmt.setLong(2, newLeaseCounter);
        pstmt.setString(3, roleId);
        return pstmt;
    }

    public PreparedStatement getRelinquishLeadershipStmt(Connection connection, String roleId, String candidateId)
            throws SQLException {
        PreparedStatement pstmt = connection.prepareStatement(getRelinquishLeadershipSQL());
        pstmt.setString(1, roleId);
        pstmt.setString(2, candidateId);
        return pstmt;
    }

    
    
    /**
     * Get statement which will insert-if-not-exist the given {@code roleId}.
     * Methods {@link #getTabName()}, {@link #getInsertColumnList()} and
     * {@link #getInsertValuesListWithDefaults()} are useful helpers when
     * building the SQL text.
     *
     * <p>
     * The statement must not throw errors if two processes at the same time try
     * to insert the same row. Note that the expected outcome is not what is
     * sometimes called an an "UPSERT" as in this case we are not interested
     * in doing an update. It would be more correctly described as a
     * <i>conditional insert</i>, meaning if-not-exist-then-insert-else-do-nothing. 
     * Some databases have build-in constructs for this, while for others it 
     * will be necessary to catch-and-ignore an error. Solutions such as using 
     * the {@code MERGE} keyword, using {@code EXCEPT} or similar constructs 
     * are <i>not</i> safe to use under concurrent load.
     *
     * @param connection
     * @param roleId
     * @return
     * @throws SQLException 
     */
    public abstract PreparedStatement getInsertRoleStmt(Connection connection, String roleId) throws SQLException;

    
    /**
     * Tablename for leader election table, as it should be used in SQL strings. If relevant it will 
     * include a schema prefix.
     */
    public final String getTabName() {
        return tabName;
    }

    /**
     * Tablename for leader election table. Never with a schema prefix.
     */
    public String getTabNamePlain() {
        return tabNamePlain;
    }
    
    
}
