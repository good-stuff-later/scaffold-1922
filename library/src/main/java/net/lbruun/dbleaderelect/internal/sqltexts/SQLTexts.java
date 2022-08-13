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
package net.lbruun.dbleaderelect.internal.sqltexts;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import net.lbruun.dbleaderelect.LeaderElectorConfiguration;
import net.lbruun.dbleaderelect.DatabaseEngine;
import net.lbruun.dbleaderelect.LeaderElector;
import static net.lbruun.dbleaderelect.LeaderElector.NO_LEADER_CANDIDATE_ID;

/**
 *
 */
public abstract class SQLTexts {
    
    private static final String SQL_SELECT_TEMPLATE
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
            + "     last_seen_timestamp = 0"
            + " WHERE role_id = ?"
            + " AND candidate_id = ?";

    private final String selectSQL;
    private final String affirmLeadershipSQL;
    private final String assumeLeadershipSQL;
    private final String relinquishLeadershipSQL;
    private final String tabName;

    public SQLTexts(LeaderElectorConfiguration configuration) {
        this.tabName = (configuration.getSchemaName() != null) 
                ? configuration.getSchemaName() + "." + configuration.getTableName() 
                : configuration.getTableName();
        this.selectSQL = String.format(SQL_SELECT_TEMPLATE, currentUtcMsExpression(),this.tabName);
        this.affirmLeadershipSQL = String.format(SQL_AFFIRM_LEADERSHIP_TEMPLATE, this.tabName, currentUtcMsExpression());
        this.assumeLeadershipSQL = String.format(SQL_ASSUME_LEADERSHIP_TEMPLATE, this.tabName, currentUtcMsExpression());
        this.relinquishLeadershipSQL = String.format(SQL_RELINQUISH_LEADERSHIP_TEMPLATE, this.tabName);
    }

    
    public static SQLTexts getSQL(LeaderElectorConfiguration configuration) {
        DatabaseEngine databaseEngine = configuration.getDatabaseEngine();
        switch (databaseEngine) {
            case POSTGRESQL:
                return new SQLTextsPostgreSQL(configuration);
            case MYSQL:    
                return new SQLTextsMySQL(configuration);
            default:
                throw new RuntimeException("Support for " + databaseEngine + " not yet implemented");
        }
    }

    
    /**
     * Database expression which returns the current number of milliseconds from
     * the 1970-01-01 epoch UTC. This is the equivalent of Java's
     * System.currentTimeMillis() but by letting the database decide this we
     * effectively use a single clock across all candidates.
     */
    protected abstract String currentUtcMsExpression();
    
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
    
    
    /**
     * Get statement which will insert-if-not-exist the given {@code roleId}.
     * Methods {@link #getTabName()}, {@link #getInsertColumnList()} and
     * {@link #getInsertValuesListWithDefaults()} are useful helpers when
     * building the SQL text.
     *
     * <p>
     * The statement must not throw errors if two processes at the same time try
     * to insert the same row. Note that the expected outcome is not what is
     * sometimes called an an "UPSERT" as in this case were are not interested
     * in doing an update. It would be more correctly described as a
     * <i>conditional insert</i>, meaning if-not-exist-then-insert-else-do-nothing. 
     * Some database where build-in constructs for this, while for others it 
     * will be necessary to catch-and-ignore an error. Solutions such as using 
     * the {@code MERGE} keyword, using {@code EXCEPT} or similar constructs 
     * are <i>not</i> safe to use under concurrent load.
     *
     * @param connection
     * @param roleId
     * @return
     * @throws SQLException 
     */
    public abstract PreparedStatement getInsertRoleSQL(Connection connection, String roleId) throws SQLException;

    

    
    public final String getInsertColumnList() {
        return "(role_id, candidate_id, last_seen_timestamp, lease_counter)";
    }
    
    /**
     * 
     * @return 
     */
    public final String getInsertValuesListWithDefaults() {
        return "( ?, '" + LeaderElector.NO_LEADER_CANDIDATE_ID + "'," + LeaderElector.NO_LEADER_LASTSEENTIMESTAMP_MS + ", 0)";
    }
    
    /**
     * Tablename as it should be used in SQL strings. If relevant it will 
     * include a schema prefix.
     */
    public final String getTabName() {
        return tabName;
    }
    
    
}
