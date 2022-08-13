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
 * All candidates contending for leadership for the same role must use
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
     * Default value for {@code assumeDeadMs}.
     */
    public static final long DEFAULT_ASSUME_DEAD_MS = 2 * DEFAULT_INTERVAL_MS;
    
    /**
     * Default value for {@code queryTimeoutSecs}.
     */
    public static final int DEFAULT_QUERY_TIMEOUT_SECS = 120;
    
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
            int queryTimeoutSecs
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

    @Override
    public String toString() {
        String dbE = (databaseEngine == null) ? "<auto-detect>" : databaseEngine.toString();
        return this.getClass().getSimpleName() + "{" + "roleId=" + roleId + ", candidateId=" + candidateId + ", databaseEngine=" + dbE + ", schemaName=" + schemaName + ", tableName=" + tableName + ", intervalMs=" + intervalMs + ", assumeDeadMs=" + assumeDeadMs + ", queryTimeoutSecs=" + queryTimeoutSecs + '}';
    }
    
    
    /**
     * Gets a new configuration based on an existing one but with with values
     * which should be auto-detected set to their actual value based on what can
     * be obtained from the provided {@code DataSource}.
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
     */
    public static class Builder {

        private String roleId = DEFAULT_ROLEID;
        private String candidateId = DEFAULT_CANDIDATEID;
        private LeaderElectorLogger leaderElectorLogger = DEFAULT_LOGGER;
        private DatabaseEngine databaseEngine = null;
        private String schemaName = null;
        private String tableName = DEFAULT_TABLENAME;
        private long intervalMs = DEFAULT_INTERVAL_MS;
        private long assumeDeadMs = DEFAULT_ASSUME_DEAD_MS;
        private LeaderElectorListener listener = new LeaderElectorListener.NoOpListener();
        private EnumSet<LeaderElectorListener.EventType> listenerSubscription  = DEFAULT_SUBSCRIPTION;
        private int queryTimeoutSecs = DEFAULT_QUERY_TIMEOUT_SECS;

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
         * The value must be no longer than {@link #ROLEID_MAX_LENGTH ROLEID_MAX_LENGTH}. 
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
         * Defines an id for the current candidate. The velue must be unique
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
         * Leader Elector.
         * 
         * <p>
         * Defaults to {@link #DEFAULT_LOGGER DEFAULT_LOGGER} if not set. 
         * Set this value to {@link LeaderElectorLogger#NULL_LOGGER NULL_LOGGER} if 
         * such messages should be suppressed entirely.
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
         * will decide that a candidate is dead if it hasn't renewed its lease within
         * this time. Therefore, if a lease is older than {@code assumeDeadMs}
         * then the Leader Elector will assume the candidate it dead - as the candidate hasn't
         * properly renewed its own lease - and the lease is now up for grabs by
         * another candidate. Meaning another candidate may now assume leader role.
         * 
         * <p>
         * The value must be larger than {@link #withIntervalMs(long) intervalMs}. 
         * It is suggested to use a value 2x {@code intervalMs}.
         * 
         * <p>
         * Defaults to {@link LeaderElectorConfiguration#DEFAULT_ASSUME_DEAD_MS DEFAULT_ASSUME_DEAD_MS}
         * if not set.
         * 
         * @param assumeDeadMs milliseconds value after which time a candidate will
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
         * died and a new one should be promoted.
         * 
         * <p>
         * The lower this value is the smaller the amount if time which 
         * may pass without a leader. 
         * 
         * <p>
         * The value must be &lt; {@link #withAssumeDeadMs(long) assumeDeadMs}.
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
         * Defaults to {@link LeaderElectorConfiguration#DEFAULT_SUBSCRIPTION DEFAULT_SUBSCRIPTION} 
         * if not set. A value of {@code EnumSet.allOf(LeaderElectorListener.EventType.class)}
         * can be used to receive any type of event which may be useful for
         * debugging purpose.
         *
         * @throws LeaderElectorConfigurationException if input is {@code null}
         * @param eventSubscription subscription
         */
        public final Builder withListenerSubscription(EnumSet<LeaderElectorListener.EventType> eventSubscription) {
            if (eventSubscription == null) {
                throw new LeaderElectorConfigurationException("eventSubscription cannot be null");
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
         * Defaults to {@link LeaderElectorConfiguration#DEFAULT_QUERY_TIMEOUT_SECS DEFAULT_QUERY_TIMEOUT_SECS} 
         * if not set. 
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
         * Creates a new configuration.
         * 
         * @throws LeaderElectorConfigurationException if values in the
         *         configuration are inconsistent.
         * @return configuration
         */
        public LeaderElectorConfiguration build() {
            if (intervalMs >= assumeDeadMs) {
                throw new LeaderElectorConfigurationException("intervalMs must be < assumeDeadMs");
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
                    queryTimeoutSecs);
        }
    }
}
