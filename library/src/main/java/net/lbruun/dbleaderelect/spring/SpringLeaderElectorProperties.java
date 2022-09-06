/*
 * Copyright 2022 lbruun.net
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.lbruun.dbleaderelect.spring;

import java.util.EnumSet;
import net.lbruun.dbleaderelect.DatabaseEngine;
import net.lbruun.dbleaderelect.LeaderElectorConfiguration;
import net.lbruun.dbleaderelect.LeaderElectorListener;
import net.lbruun.dbleaderelect.LeaderElectorLogger;
import net.lbruun.dbleaderelect.exception.LeaderElectorConfigurationException;

/**
 * Java Bean version of {@link LeaderElectorConfiguration}.
 */
public class SpringLeaderElectorProperties {

    private String roleId;
    private String candidateId;
    private DatabaseEngine databaseEngine;
    private String schemaName;
    private String tableName;
    private Long intervalMs;
    private Long assumeDeadMs;
    private String listenerSubscription;
    private Integer queryTimeoutSecs;
    private Boolean createTable;

    /**
     * Get property {@code roleId}.
     * 
     * <p>
     * See {@link LeaderElectorConfiguration.Builder#withRoleId(java.lang.String) this}
     * for more information about this property.
     */
    public String getRoleId() {
        return roleId;
    }

    /**
     * Set property {@code roleId}.
     * 
     * <p>
     * See {@link LeaderElectorConfiguration.Builder#withRoleId(java.lang.String) this}
     * for more information about this property.
     */
    public void setRoleId(String roleId) {
        this.roleId = roleId;
    }

    /**
     * Get property {@code candidateId}.
     * 
     * <p>
     * See {@link LeaderElectorConfiguration.Builder#withCandidateId(java.lang.String) this}
     * for more information about this property.
     */
    public String getCandidateId() {
        return candidateId;
    }

    /**
     * Set property {@code candidateId}.
     * 
     * <p>
     * See {@link LeaderElectorConfiguration.Builder#withCandidateId(java.lang.String) this}
     * for more information about this property.
     */
    public void setCandidateId(String candidateId) {
        this.candidateId = candidateId;
    }

    /**
     * Get property {@code databaseEngine}.
     * 
     * <p>
     * See {@link LeaderElectorConfiguration.Builder#withDatabaseEngine(net.lbruun.dbleaderelect.DatabaseEngine) this}
     * for more information about this property.
     */
    public DatabaseEngine getDatabaseEngine() {
        return databaseEngine;
    }

    /**
     * Set property {@code databaseEngine}. Note, that there is rarely a good
     * reason to set this value explicitly as the type of database engine
     * will be auto-detected if this property is not set.
     * 
     * <p>
     * See {@link LeaderElectorConfiguration.Builder#withDatabaseEngine(net.lbruun.dbleaderelect.DatabaseEngine) this}
     * for more information about this property.
     */
    public void setDatabaseEngine(DatabaseEngine databaseEngine) {
        this.databaseEngine = databaseEngine;
    }

    /**
     * Get property {@code schemaName}.
     * 
     * <p>
     * See {@link LeaderElectorConfiguration.Builder#withSchemaName(java.lang.String) this}
     * for more information about this property.
     */
    public String getSchemaName() {
        return schemaName;
    }

    /**
     * Set property {@code schemaName}.
     * 
     * <p>
     * See {@link LeaderElectorConfiguration.Builder#withSchemaName(java.lang.String) this}
     * for more information about this property.
     */
    public void setSchemaName(String schemaName) {
        this.schemaName = schemaName;
    }

    /**
     * Get property {@code tableName}.
     * 
     * <p>
     * See {@link LeaderElectorConfiguration.Builder#withTableName(java.lang.String) this}
     * for more information about this property.
     */
    public String getTableName() {
        return tableName;
    }

    /**
     * Set property {@code tableName}.
     * 
     * <p>
     * See {@link LeaderElectorConfiguration.Builder#withTableName(java.lang.String) this}
     * for more information about this property.
     */
    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    /**
     * Get property {@code intervalMs}.
     * 
     * <p>
     * See {@link LeaderElectorConfiguration.Builder#withIntervalMs(long) this}
     * for more information about this property.
     */
    public Long getIntervalMs() {
        return intervalMs;
    }

    /**
     * Set property {@code intervalMs}.
     * 
     * <p>
     * See {@link LeaderElectorConfiguration.Builder#withIntervalMs(long) this}
     * for more information about this property.
     */
    public void setIntervalMs(long intervalMs) {
        this.intervalMs = intervalMs;
    }

    /**
     * Get property {@code assumeDeadMs}.
     * 
     * <p>
     * See {@link LeaderElectorConfiguration.Builder#withAssumeDeadMs(long) this}
     * for more information about this property.
     */
    public Long getAssumeDeadMs() {
        return assumeDeadMs;
    }

    /**
     * Set property {@code assumeDeadMs}.
     * 
     * <p>
     * See {@link LeaderElectorConfiguration.Builder#withAssumeDeadMs(long) this}
     * for more information about this property.
     */
    public void setAssumeDeadMs(long assumeDeadMs) {
        this.assumeDeadMs = assumeDeadMs;
    }

    /**
     * Get property {@code listenerSubscription} as a comma-separated string.
     * 
     * <p>
     * See {@link LeaderElectorConfiguration.Builder#withListenerSubscription(java.util.EnumSet) this}
     * for more information about this property.
     * 
     * @see #setListenerSubscription(java.lang.String) 
     */
    public String getListenerSubscription() {
        return listenerSubscription;
    }

    /**
     * Set property {@code listenerSubscription} as a comma-separated string.
     * 
     * <p>
     * See {@link LeaderElectorConfiguration.Builder#withListenerSubscription(java.util.EnumSet) this}
     * for more information about this property.
     * 
     * <p>
     * The value must be a comma-separated string of the values of 
     * {@link LeaderElectorListener.EventType EventType}. However, two additional forms
     * are supported:
     * <ul>
     *    <li>The value {@code "#ALL#"} (on its own) means all event types.</li>
     *    <li>A minus sign in front of an enum value means exclusion. For example, the
     *        value {@code "-LEADERSHIP_CONFIRMED,-LEADERSHIP_NOOP"} means all
     *        event types <i>except</i> those two. </li>
     * </ul>
     */
    public void setListenerSubscription(String listenerSubscription) {
        this.listenerSubscription = listenerSubscription;
    }

    /**
     * Get property {@code queryTimeoutSecs}.
     * 
     * <p>
     * See {@link LeaderElectorConfiguration.Builder#withQueryTimeoutSecs(int) this}
     * for more information about this property.
     */
    public Integer getQueryTimeoutSecs() {
        return queryTimeoutSecs;
    }

    /**
     * Set property {@code queryTimeoutSecs}.
     * 
     * <p>
     * See {@link LeaderElectorConfiguration.Builder#withQueryTimeoutSecs(int) this}
     * for more information about this property.
     */
    public void setQueryTimeoutSecs(int queryTimeoutSecs) {
        this.queryTimeoutSecs = queryTimeoutSecs;
    }

    /**
     * Get property {@code createTable}.
     * 
     * <p>
     * See {@link LeaderElectorConfiguration.Builder#withCreateTable(boolean) this}
     * for more information about this property.
     */
    public Boolean getCreateTable() {
        return createTable;
    }

    /**
     * Set property {@code createTable}.
     * 
     * <p>
     * See {@link LeaderElectorConfiguration.Builder#withCreateTable(boolean) this}
     * for more information about this property.
     */
    public void setCreateTable(Boolean createTable) {
        this.createTable = createTable;
    }
    

    /**
     * Gets immutable configuration from this bean as well as the values
     * supplied as method arguments.
     * @param listener listener to set in the configuration
     * @param logger logger to set in the configuration
     * @return immutable configuration
     * @throws LeaderElectorConfigurationException if values in the
     *     configuration are inconsistent.
     */
    public LeaderElectorConfiguration configuration(
            LeaderElectorListener listener,
            LeaderElectorLogger logger) throws LeaderElectorConfigurationException {
        LeaderElectorConfiguration.Builder builder 
                = LeaderElectorConfiguration.builder();

        builder.withListener(listener);
        builder.withLogger(logger);
        
        if (roleId != null)  {
            builder.withRoleId(roleId);
        }

        if (candidateId != null) {
            builder.withCandidateId(candidateId);
        }

        if (databaseEngine != null) {
            builder.withDatabaseEngine(databaseEngine);
        }

        if (schemaName != null) {
            builder.withSchemaName(schemaName);
        }

        if (tableName != null) {
            builder.withTableName(tableName);
        }

        if (intervalMs != null) {
            builder.withIntervalMs(intervalMs);
        }

        if (assumeDeadMs != null) {
            builder.withAssumeDeadMs(assumeDeadMs);
        }

        if (listenerSubscription != null) {
            EnumSet<LeaderElectorListener.EventType> eventTypeSubs
                    = EventTypesListConversion.convert(listenerSubscription);
            builder.withListenerSubscription(eventTypeSubs);
        }

        if (queryTimeoutSecs != null) {
            builder.withQueryTimeoutSecs(queryTimeoutSecs);
        }
        
        if (createTable != null) {
            builder.withCreateTable(createTable);
        }
        
        return builder.build();
    }
    
    /**
     * Gets immutable configuration from this bean as well as the value
     * supplied as method argument. Sets the logger to be an instance
     * of {@link SpringLeaderElectorLogger}.
     * 
     * @param listener listener to set in the configuration
     * @return immutable configuration
     * @throws LeaderElectorConfigurationException if values in the
     *     configuration are inconsistent.
     */
    public LeaderElectorConfiguration configuration(
            LeaderElectorListener listener) throws LeaderElectorConfigurationException {
        return configuration(listener, new SpringLeaderElectorLogger());
    }
}
