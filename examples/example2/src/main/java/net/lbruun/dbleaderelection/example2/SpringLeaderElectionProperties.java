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
package net.lbruun.dbleaderelection.example2;

import java.util.EnumSet;
import net.lbruun.dbleaderelect.DatabaseEngine;
import net.lbruun.dbleaderelect.LeaderElectorConfiguration;
import net.lbruun.dbleaderelect.LeaderElectorListener;
import net.lbruun.dbleaderelect.LeaderElectorLogger;
import net.lbruun.dbleaderelect.exception.LeaderElectorConfigurationException;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Java Bean version of {@link LeaderElectorConfiguration}.
 */
@ConfigurationProperties(prefix = "dbleaderelection")
public class SpringLeaderElectionProperties {

    private String roleId;
    private String candidateId;
    private LeaderElectorLogger leaderElectorLogger;
    private DatabaseEngine databaseEngine;
    private String schemaName;
    private String tableName;
    private Long intervalMs;
    private Long assumeDeadMs;
    private LeaderElectorListener listener;
    private String listenerSubscription;
    private Integer queryTimeoutSecs;

    public String getRoleId() {
        return roleId;
    }

    public void setRoleId(String roleId) {
        this.roleId = roleId;
    }

    public String getCandidateId() {
        return candidateId;
    }

    public void setCandidateId(String candidateId) {
        this.candidateId = candidateId;
    }

    public LeaderElectorLogger getLeaderElectorLogger() {
        return leaderElectorLogger;
    }

    public void setLeaderElectorLogger(LeaderElectorLogger leaderElectorLogger) {
        this.leaderElectorLogger = leaderElectorLogger;
    }

    public DatabaseEngine getDatabaseEngine() {
        return databaseEngine;
    }

    public void setDatabaseEngine(DatabaseEngine databaseEngine) {
        this.databaseEngine = databaseEngine;
    }

    public String getSchemaName() {
        return schemaName;
    }

    public void setSchemaName(String schemaName) {
        this.schemaName = schemaName;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public long getIntervalMs() {
        return intervalMs;
    }

    public void setIntervalMs(long intervalMs) {
        this.intervalMs = intervalMs;
    }

    public long getAssumeDeadMs() {
        return assumeDeadMs;
    }

    public void setAssumeDeadMs(long assumeDeadMs) {
        this.assumeDeadMs = assumeDeadMs;
    }

    public LeaderElectorListener getListener() {
        return listener;
    }

    public void setListener(LeaderElectorListener listener) {
        this.listener = listener;
    }

    public String getListenerSubscription() {
        return listenerSubscription;
    }

    public void setListenerSubscription(String listenerSubscription) {
        this.listenerSubscription = listenerSubscription;
    }

    public int getQueryTimeoutSecs() {
        return queryTimeoutSecs;
    }

    public void setQueryTimeoutSecs(int queryTimeoutSecs) {
        this.queryTimeoutSecs = queryTimeoutSecs;
    }

    /**
     * 
     * Gets immutable configuration from this bean.
     * @return immutable configuration
     * @throws LeaderElectorConfigurationException if values in the
     *     configuration are inconsistent.
     *
     */
    public LeaderElectorConfiguration configuration() throws LeaderElectorConfigurationException {
        LeaderElectorConfiguration.Builder builder 
                = LeaderElectorConfiguration.builder();

        if (roleId != null)  {
            builder.withRoleId(roleId);
        }

        if (candidateId != null) {
            builder.withCandidateId(candidateId);
        }

        if (leaderElectorLogger != null) {
            builder.withLogger(leaderElectorLogger);
        } else {
            builder.withLogger(new SpringLeaderElectorLogger());
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

        if (listener != null) {
            builder.withListener(listener);
        }

        if (listenerSubscription != null) {
            EnumSet<LeaderElectorListener.EventType> eventTypes
                    = EventTypesListConversion.convert(listenerSubscription);
            builder.withListenerSubscription(eventTypes);
        }

        if (queryTimeoutSecs != null) {
            builder.withQueryTimeoutSecs(queryTimeoutSecs);
        }
        return builder.build();
    }
}
