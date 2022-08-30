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

import net.lbruun.dbleaderelect.exception.LeaderElectorConfigurationException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLTimeoutException;
import java.util.EnumSet;
import javax.sql.DataSource;
import net.lbruun.dbleaderelect.utils.NodeIdUtils;

/**
 * Configuration for Leader Elector.
 * 
 * <p>
 * Use {@link #builder()} to create an instance of this class.
 * 
 * <p>
 * IMPORTANT: All candidates contending for leadership for the same role <i>must</i> use
 * the same configuration values, except for the value of {@code candidateId}.
 */
public class LeaderElectorConfiguration {

    /**
     * Maximum character length for the {@code roleId} value.
     */
    public static final int ROLEID_MAX_LENGTH = 20;
    /**
     * Maximum character length for the {@code candidateId} value.
     */
    public static final int CANDIDATEID_MAX_LENGTH = 256;
    
    /**
     * Default value for {@code candidateId}. 
     * This value is the result of calling {@link NodeIdUtils#getPidAndComputerName()}.
     */
    public static final String DEFAULT_CANDIDATEID = truncateStr(NodeIdUtils.getPidAndComputerName(), CANDIDATEID_MAX_LENGTH);
    
    /**
     * Default value for {@code roleId}. 
     */
    public static final String DEFAULT_ROLEID = "DEFAULT";

    /**
     * Default value for {@code leaderElectorLogger}. This is 
     * {@link LeaderElectorLogger#STDOUT_LOGGER STDOUT_LOGGER}.
     */
    public static final LeaderElectorLogger DEFAULT_LOGGER = LeaderElectorLogger.STDOUT_LOGGER;
    
    /**
     * Default value for {@code tableName}.
     */
    public static final String DEFAULT_TABLENAME = "db_leader_elect";
    
    /**
     * Default value for {@code intervalMs}
     */
    public static final long DEFAULT_INTERVAL_MS = 20 * 1000L;

    /**
     * Default value for {@code createTable}
     */
    public static final boolean DEFAULT_CREATE_TABLE = false;
    
    
    /**
     * Default value for {@code listenerSubscription}. 
     * The subscription contains all event types, except:
     * <ul>
     *   <li>{@link LeaderElectorListener.EventType#LEADERSHIP_CONFIRMED LEADERSHIP_CONFIRMED}</li>
     *   <li>{@link LeaderElectorListener.EventType#LEADERSHIP_NOOP LEADERSHIP_NOOP}</li>
     * </ul>
     */
    public static final EnumSet<LeaderElectorListener.EventType> DEFAULT_SUBSCRIPTION = 
            EnumSet.complementOf(EnumSet.of(
                    LeaderElectorListener.EventType.LEADERSHIP_CONFIRMED,
                    LeaderElectorListener.EventType.LEADERSHIP_NOOP
            ));



    /**
     * Event types which <i>must</i> be part of a subscription.
     * This is:
     * <ul>
     *   <li>{@link LeaderElectorListener.EventType#LEADERSHIP_ASSUMED LEADERSHIP_ASSUMED}</li>
     *   <li>{@link LeaderElectorListener.EventType#LEADERSHIP_LOST LEADERSHIP_LOST}</li>
     * </ul>
     */
    public static final EnumSet<LeaderElectorListener.EventType> MANDATORY_SUBSCRIPTIONS = 
            EnumSet.of(
                    LeaderElectorListener.EventType.LEADERSHIP_ASSUMED,
                    LeaderElectorListener.EventType.LEADERSHIP_LOST);
    

    /**
     * Configuration with all values set to their default value and a listener
     * which outputs to {@code stdout}. Mainly useful when testing.
     */
    public static final LeaderElectorConfiguration DEFAULT = builder().build();

    private final String roleId;
    private final String candidateId;
    private final LeaderElectorLogger leaderElectorLogger;
    private final DatabaseEngine databaseEngine;
    private final String schemaName;
    private final String tableName;
    private final long intervalMs;
    private final long assumeDeadMs;
    private final LeaderElectorListener listener;
    private final EnumSet<LeaderElectorListener.EventType> listenerSubscription;
    private final int queryTimeoutSecs;
    private final boolean createTable;

    private LeaderElectorConfiguration(
            String roleId,
            String candidateId,
            LeaderElectorLogger leaderElectorLogger,
            DatabaseEngine databaseEngine,
            String schemaName,
            String tableName, 
            long intervalMs, 
            long assumeDeadMs,
            LeaderElectorListener listener,
            EnumSet<LeaderElectorListener.EventType> listenerSubscription,
            int queryTimeoutSecs,
            boolean createTable
    ) {
        this.roleId = roleId;
        this.candidateId = candidateId;
        this.leaderElectorLogger = leaderElectorLogger;
        this.databaseEngine = databaseEngine;
        this.schemaName = schemaName;
        this.tableName = tableName;
        this.assumeDeadMs = assumeDeadMs;
        this.intervalMs = intervalMs;
        this.listener = listener;
        this.listenerSubscription  = listenerSubscription;
        this.queryTimeoutSecs = queryTimeoutSecs;
        this.createTable = createTable;
    }

    public String getRoleId() {
        return roleId;
    }

    public String getCandidateId() {
        return candidateId;
    }

    public LeaderElectorLogger getLeaderElectorLogger() {
        return leaderElectorLogger;
    }

    public DatabaseEngine getDatabaseEngine() {
        return databaseEngine;
    }

    public String getSchemaName() {
        return schemaName;
    }

    public String getTableName() {
        return tableName;
    }

    public long getIntervalMs() {
        return intervalMs;
    }

    public long getAssumeDeadMs() {
        return assumeDeadMs;
    }

    public LeaderElectorListener getListener() {
        return listener;
    }

    public EnumSet<LeaderElectorListener.EventType> getListenerSubscription() {
        return listenerSubscription;
    }
    
    public int getQueryTimeoutSecs() {
        return queryTimeoutSecs;
    }

    public boolean getCreateTable() {
        return createTable;
    }

    @Override
    public String toString() {
        String dbE = (databaseEngine == null) ? "<auto-detect>" : databaseEngine.toString();
        return this.getClass().getSimpleName() 
                + "{" 
                + "roleId=" + roleId 
                + ", candidateId=" + candidateId 
                + ", databaseEngine=" + dbE 
                + ", schemaName=" + schemaName 
                + ", tableName=" + tableName 
                + ", intervalMs=" + intervalMs 
                + ", assumeDeadMs=" + assumeDeadMs 
                + ", queryTimeoutSecs=" + queryTimeoutSecs 
                + ", createTable=" + createTable
                + ", listener=" + listener.getClass().getSimpleName()
                + ", listenerSubscription=" + listenerSubscription.toString()
                + ", leaderElectorLogger=" + leaderElectorLogger.getClass().getSimpleName()
                + '}';
    }
    
    
    /**
     * Gets a new configuration based on an existing one but with with values
     * which should be auto-detected set to their actual value based on what can
     * be obtained from the provided {@code DataSource}.
     * 
     * <p>
     * Note: There is rarely any reason to explicitly call this method. It is
     * used by the library in the constructor for {@link LeaderElector}. It is 
     * mainly exposed for testing purpose.
     *
     * @param configuration existing configuration object
     * @param dataSource from where to auto-detect
     * @throws LeaderElectorConfigurationException if missing values cannot be 
     *   auto-detected
     * @return new configuration object
     */
    public static LeaderElectorConfiguration getRuntimeConfiguration(LeaderElectorConfiguration configuration, DataSource dataSource) 
            throws LeaderElectorConfigurationException {
        if (configuration.getDatabaseEngine() == null) {
            try (Connection connection = dataSource.getConnection()) {
                DatabaseEngine databaseEngine = DatabaseEngine.getDatabaseEngineFromConnection(connection);
                configuration.getLeaderElectorLogger().logInfo(
                        LeaderElectorConfiguration.class, "Leader Elector using " + databaseEngine + " database engine (auto-detected)");
                return LeaderElectorConfiguration.builder(configuration)
                        .withDatabaseEngine(databaseEngine)
                        .build();
            } catch (SQLTimeoutException ex) {
                throw new LeaderElectorConfigurationException("Cannot connect to database in order to auto-detect database engine type", ex);
            } catch (SQLException ex) {
                throw new LeaderElectorConfigurationException("Cannot determine database engine type. You may have to set it explicitly in the configuration", ex);
            }
        }
        return configuration;
    }

    
    
    
    /** 
     * Creates builder.
     * @return builder for new configuration
     */
    public static Builder builder() {
        return new Builder();
    }
    
    /** 
     * Creates builder with values preset from existing configuration.
     * This provides a way to clone an existing configuration.
     * @param configuration existing configuration
     * @return builder for new configuration
     */
    public static Builder builder(LeaderElectorConfiguration configuration) {
        return new Builder(configuration);
    }
    
    private static String truncateStr(String str, int len) {
        if (str == null) {
            return null;
        }
        if (str.length() > len) {
            return str.substring(0, len);
        }
        return str;
    }

    /**
     * Builder for {@link LeaderElectorConfiguration}.
     * 
     * <p>
     * The builder provides reasonable defaults for most values.
     * When tuning the time-related values it is recommended only
     * to set the {@link Builder#withIntervalMs(long) intervalMs}
     * value explicitly. Other time-related values will be then be derived
     * from this with reasonable values.
     */
    public static class Builder {

        private String roleId;
        private String candidateId;
        private LeaderElectorLogger leaderElectorLogger;
        private DatabaseEngine databaseEngine;
        private String schemaName;
        private String tableName;
        private Long intervalMs;
        private Long assumeDeadMs;
        private LeaderElectorListener listener;
        private EnumSet<LeaderElectorListener.EventType> listenerSubscription;
        private Integer queryTimeoutSecs;
        private Boolean createTable;

        private Builder() {
        }
        
        private Builder(LeaderElectorConfiguration configuration) {
            withAssumeDeadMs(configuration.getAssumeDeadMs());
            withDatabaseEngine(configuration.getDatabaseEngine());
            withIntervalMs(configuration.getIntervalMs());
            withLogger(configuration.getLeaderElectorLogger());
            withListener(configuration.getListener());
            withListenerSubscription(configuration.getListenerSubscription());
            withCandidateId(configuration.getCandidateId());
            withQueryTimeoutSecs(configuration.getQueryTimeoutSecs());
            withSchemaName(configuration.getSchemaName());
            withTableName(configuration.getTableName());
            withCreateTable(configuration.getCreateTable());
        }

        /**
         * Role to compete for. Candidates compete for a particular role. Each
         * role is an election of its own. Examples of roles to compete for
         * <ul>
         *   <li>UniverseLeader</li>
         *   <li>PlanetLeader</li>
         *   <li>PackLeader</li>
         * </ul>
         *
         * <p>
         * The value must be no longer than
         * {@link #ROLEID_MAX_LENGTH ROLEID_MAX_LENGTH}. Most applications only
         * need a single leader role to compete for and therefore do not need to
         * set this value.
           * 
         * <p>
         * Defaults to {@link #DEFAULT_ROLEID} if not set.
         * 
         * @throws LeaderElectorConfigurationException if argument is {@code null},
         *         empty string or more than {@link #ROLEID_MAX_LENGTH ROLEID_MAX_LENGTH}
         *         characters long
         * @param roleId id for the role
         * @return 
         */
        public final Builder withRoleId(String roleId) {
            if (roleId == null || roleId.isEmpty()) {
                throw new LeaderElectorConfigurationException("roleId must have a value, cannot be empty or null");
            }
            if (roleId.length() > ROLEID_MAX_LENGTH) {
                throw new LeaderElectorConfigurationException("Invalid value for roleId: \"" + roleId + "\". roleId must be no longer than " + ROLEID_MAX_LENGTH + " characters long");
            }
            this.roleId = roleId;
            return this;
        }
        

        /**
         * Defines an id for the current candidate. The value must be unique
         * between all candidates participating in a leader election for the same
         * role.
         * 
         * <p>
         * The value must be no longer than {@link #CANDIDATEID_MAX_LENGTH CANDIDATEID_MAX_LENGTH}. 
         * If longer, it will automatically be truncated to this length.
         * 
         * <p>
         * Defaults to {@link NodeIdUtils#getPidAndComputerName()} if not set.
         * 
         * <p>
         * The {@link NodeIdUtils} class defines useful methods which can act as
         * input to this method. However, the default value will be sufficient
         * for most needs.
         *
         * @see NodeIdUtils
         * @throws LeaderElectorConfigurationException if argument is {@code null} 
         *         or an empty string
         * @param candidateId unique id for candidate
         * @return 
         */
        public final Builder withCandidateId(String candidateId) {
            if (candidateId == null || candidateId.isEmpty()) {
                throw new LeaderElectorConfigurationException("candidateId must have a value, cannot be empty or null");
            }
            this.candidateId = truncateStr(candidateId, CANDIDATEID_MAX_LENGTH);
            return this;
        }
        

        /**
         * Defines a logger implementation to use for messages from the
         * Leader Elector. This includes informational messages during startup
         * and shutdown as well as error messages from the event propagation
         * mechanism. As such, this logger is not the way to be alerted to
         * errors from the core of the leader election process; 
         * {@link LeaderElectorListener.Event#hasErrors() events} are used
         * for this.
         * 
         * <p>
         * Defaults to {@link #DEFAULT_LOGGER DEFAULT_LOGGER} if not set. 
         * Set this value to {@link LeaderElectorLogger#NULL_LOGGER NULL_LOGGER} if 
         * such messages should be suppressed entirely. This can be justified
         * if there is no need for informational startup/shutdown messages
         * and if you are confident that your {@link LeaderElectorListener#onLeaderElectionEvent(net.lbruun.dbleaderelect.LeaderElectorListener.Event)
         * LeaderElectorListener#onLeaderElectionEvent()} implementation never 
         * throws exceptions.
         * 
         * @param leaderElectorLogger logger implementation
         * @return 
         */
        public final Builder withLogger(LeaderElectorLogger leaderElectorLogger) {
            if (leaderElectorLogger == null ) {
                throw new LeaderElectorConfigurationException("infoLogger must have a value, cannot be null");
            }
            this.leaderElectorLogger = leaderElectorLogger;
            return this;
        }

        
        /**
         * Defines the type of database in use. The Leader Elector needs 
         * to know which type of database engine is in use as there are slight
         * variations of the SQL grammar between database engines.
         * 
         * <p>
         * Defaults to {@code null} is not set. A value of {@code null} means
         * the database type will be auto-detected at startup.
         * 
         * @param databaseEngine
         */
        public final Builder withDatabaseEngine(DatabaseEngine databaseEngine) {
            this.databaseEngine = databaseEngine;
            return this;
        }
        
        /**
         * Defines the schema of the table which is used to keep track
         * of current leadership. The schema must already exist. 
         * 
         * <p>
         * Defaults to {@code null} if not set. The {@code null} value
         * means to use the default schema of the database session.
         * 
         * <p>
         * WARNING: For MySQL/MariaDB the concept of "schema" is effectively
         * synonymous with a database. Therefore, if this setting is used with
         * MySQL/MariaDB it means in which <i>database</i> the table is located.
         *
         * @see #withTableName(java.lang.String) 
         * @throws LeaderElectorConfigurationException if argument 
         *    contains a dot character ('.').
         * @param schemaName unqualified schema name or {@code null}
         */
        public final Builder withSchemaName(String schemaName) {
            if (schemaName != null) {
                if (schemaName.indexOf('.') != -1) {
                    throw new LeaderElectorConfigurationException("schemaName must not contain '.' character");
                }
            }
            if (schemaName != null && schemaName.isEmpty()) {
                this.schemaName = null;
            } else {
                this.schemaName = schemaName;
            }
            return this;
        }
        
        /**
         * Defines the name of the table which is used to keep track
         * of current leadership. The table must already exist. 
         * 
         * <p>
         * Defaults to {@link LeaderElectorConfiguration#DEFAULT_TABLENAME DEFAULT_TABLENAME} 
         * if not set.
         * 
         * @see #withSchemaName(java.lang.String) 
         * @throws LeaderElectorConfigurationException if argument is {@code null}
         *    empty string, or if argument contains a '.' character.
         * @param tableName, not {@code null}
         */
        public final Builder withTableName(String tableName) {
            if (tableName == null || tableName.isEmpty()) {
                throw new LeaderElectorConfigurationException("tableName must have a value, cannot be empty or null");
            }
            if (tableName.indexOf('.') != -1) {
                throw new LeaderElectorConfigurationException("Invalid value for tableName  \"" + tableName + "\". The value must be non-qualified (without any '.')");
            }
            this.tableName = tableName;
            return this;
        }

        /**
         * Defines the number of milliseconds after which the Leader Elector
         * will decide that a leader is dead if it hasn't renewed its lease within
         * this time. Therefore, if a lease is older than {@code assumeDeadMs}
         * then the Leader Elector will assume the leader it dead - as the candidate hasn't
         * properly renewed its own lease - and the lease is now up for grabs by
         * another candidate. Meaning another candidate may now assume leader role.
         * 
         * <p>
         * If set, the value must be at least 3 seconds larger than
         * {@link #withIntervalMs(long) intervalMs}. It is suggested to use a
         * value 2x {@code intervalMs}. The difference between
         * {@code assumeDeadMs} and {@code intervalMs} is the allowance for how
         * late in renewing its lease a leader can be without being dethroned.
         * The difference must allow for network latencies, garbage collection,
         * temporary CPU starvation or any reason which will cause the renewal
         * process to be late.
         *
         * <p>
         * If not set: Defaults to 2x {@code intervalMs}, however at least 3 seconds.
         * ({@code MAX(intervalMs+3000, intervalMs*2)})
         * 
         * @param assumeDeadMs milliseconds value after which time a leader will
         *        be considered dead if it hasn't renewed its lease.
         * @throws LeaderElectorConfigurationException if input is &lt;= 0 (zero).
         */
        public final Builder withAssumeDeadMs(long assumeDeadMs) {
            if (assumeDeadMs <= 0) {
                throw new LeaderElectorConfigurationException("assumeDeadMs must be > 0");
            }
            this.assumeDeadMs = assumeDeadMs;
            return this;
        }

        /**
         * Defines how often the lease is checked and/or renewed. Every
         * {@code intervalMs} a background thread in the Leader Elector
         * will go to the database and either renew its current lease (if
         * the candidate is currently the leader) or check if other leader has
         * died and a new one should be promoted. The value is the
         * interval <i>between</i> the checks (not including the the check 
         * itself).
         * 
         * <p>
         * The lower this value is the smaller the amount of time which 
         * may pass without a leader. If leadership gaps are generally
         * undesirable then the value should be lowered. However, the lower
         * the value the more strain on the database, especially with many
         * candidates.
         * 
         * <p>
         * Defaults to {@link LeaderElectorConfiguration#DEFAULT_INTERVAL_MS DEFAULT_INTERVAL_MS}
         * if not set.
         * 
         * @throws LeaderElectorConfigurationException if input is &lt;= 0 (zero).
         * @param intervalMs interval length in milliseconds.
         */
        public final Builder withIntervalMs(long intervalMs) {
            if (intervalMs <= 0) {
                throw new LeaderElectorConfigurationException("intervalMs must be > 0");
            }
            this.intervalMs = intervalMs;
            return this;
        }
        
        /**
         * Listener which receives events from the Leader Election process.
         *
         * <p>
         * Defaults to {@link LeaderElectorListener.NoOpListener} if not set.
         *
         * @throws LeaderElectorConfigurationException if argument is {@code null}
         * @param listener listener
         */
        public final Builder withListener(LeaderElectorListener listener) {
            if (listener == null) {
                throw new LeaderElectorConfigurationException("listener cannot be null");
            }
            this.listener = listener;
            return this;
        }
        
        /**
         * Subscription for listener. This defines which
         * {@link LeaderElectorListener.EventType types of events} the listener
         * will receive.
         *
         * <p>
         * Defaults to
         * {@link LeaderElectorConfiguration#DEFAULT_SUBSCRIPTION DEFAULT_SUBSCRIPTION}
         * if not set. A value of
         * {@link LeaderElectorListener#ALL_EVENT_TYPES ALL_EVENT_TYPES} can be
         * used to receive any type of event. This may be useful for debugging
         * purpose.
         *
         * <p>
         * The set must as a minimum include the types listed in
         * {@link MANDATORY_SUBSCRIPTIONS MANDATORY_SUBSCRIPTIONS}.
         *
         * @throws LeaderElectorConfigurationException if input is {@code null}
         *    or if input does not contain all of the event types listed
         *    in {@link MANDATORY_SUBSCRIPTIONS MANDATORY_SUBSCRIPTIONS}.
         * @param eventSubscription subscription
         */
        public final Builder withListenerSubscription(EnumSet<LeaderElectorListener.EventType> eventSubscription) {
            if (eventSubscription == null) {
                throw new LeaderElectorConfigurationException("eventSubscription cannot be null");
            }
            if (!eventSubscription.containsAll(MANDATORY_SUBSCRIPTIONS)) {
                throw new LeaderElectorConfigurationException("eventSubscription must contain the mandatory types : " + MANDATORY_SUBSCRIPTIONS);
            }
            this.listenerSubscription = eventSubscription;
            return this;
        }

        /**
         * Database query timeout in seconds. This defines how long to wait for
         * the central lock (the {@link #withTableName(java.lang.String) table}
         * in the database) to become available for reading.
         *
         * <p>
         * This value should be long enough to allow for temporary hickups with
         * the database (for example if database has a temporary lag in accessing
         * disk) but short enough to reveal true problems with the database. If
         * the value is exceeded then it will result in an event of type
         * {@link LeaderElectorListener.EventType#LEADERSHIP_LOST LEADERSHIP_LOST} 
         * with a non-null {@link LeaderElectorListener.Event}.
         * 
         * <p>
         * If not set, defaults to half of seconds of {@code intervalMs},
         * however no more than 5 seconds. ({@code MIN(5, (intervalMs*1000)/2)})
         * 
         * @throws LeaderElectorConfigurationException if input is less than zero.
         * @param queryTimeoutSecs
         * @return 
         */
        public final Builder withQueryTimeoutSecs(int queryTimeoutSecs) {
            if (queryTimeoutSecs <= 0) {
                throw new LeaderElectorConfigurationException("queryTimeoutSecs must be larger than zero");
            }
            this.queryTimeoutSecs = queryTimeoutSecs;
            return this;
        }


        /**
         * If the leader election table should be created if it does not
         * already exist?. If {@code true}, then at every instantiation
         * of {@link LeaderElector} class a check will be made to see if the
         * table already exists. If not, it will be created.
         * 
         * <p>
         * The table will be created like this (generic form):
         * <pre>
         * CREATE TABLE [&lt;configuration.schemaName&gt;.]&lt;configuration.tableName&gt;
         *  (
         *     role_id               VARCHAR(20)    NOT NULL,
         *     candidate_id          VARCHAR(256)   NOT NULL,
         *     last_seen_timestamp   BIGINT         NOT NULL,
         *     lease_counter         BIGINT         NOT NULL,
         *     PRIMARY KEY(role_id)
         *  )
         * </pre>
         * (with column types replaced as appropriate for the given database 
         * engine)
         * 
         * <p>
         * If not set, defaults to {@code false}.
         * 
         */
        public final Builder withCreateTable(boolean createTable) {
            this.createTable = createTable;
            return this;
        }

        /**
         * Creates a new configuration.
         * 
         * @throws LeaderElectorConfigurationException if values in the
         *         configuration are inconsistent.
         * @return configuration
         */
        public LeaderElectorConfiguration build() throws LeaderElectorConfigurationException {

            if (intervalMs == null) {
                intervalMs = DEFAULT_INTERVAL_MS;
            }
            if (assumeDeadMs == null) {
                assumeDeadMs = Math.max(intervalMs + 3000, intervalMs * 2);
            }
            if (queryTimeoutSecs == null) {
                queryTimeoutSecs = (int) Math.min(5, (intervalMs*1000)/2);
            }            
            if (roleId == null) {
                roleId = DEFAULT_ROLEID;
            }
            if (candidateId == null) {
                candidateId = DEFAULT_CANDIDATEID;
            }
            if (leaderElectorLogger == null) {
                leaderElectorLogger = DEFAULT_LOGGER;
            }
            if (tableName == null) {
                tableName = DEFAULT_TABLENAME;
            }
            if (listener == null) {
                listener = new LeaderElectorListener.NoOpListener();
            }
            if (listenerSubscription == null) {
                listenerSubscription = DEFAULT_SUBSCRIPTION;
            }
            if (createTable == null) {
                createTable = DEFAULT_CREATE_TABLE;
            }
            
            // Validation
            if (intervalMs >= (assumeDeadMs - 3000)) {
                throw new LeaderElectorConfigurationException("assumeDeadMs must be at least 3 seconds larger than intervalMs");
            }           
            
            


            return new LeaderElectorConfiguration(
                    roleId,
                    candidateId, 
                    leaderElectorLogger,
                    databaseEngine, 
                    schemaName, 
                    tableName, 
                    intervalMs, 
                    assumeDeadMs, 
                    listener, 
                    listenerSubscription, 
                    queryTimeoutSecs,
                    createTable
            );
        }
    }
}
