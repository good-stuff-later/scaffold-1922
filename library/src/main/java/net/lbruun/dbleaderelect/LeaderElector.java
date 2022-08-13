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

import net.lbruun.dbleaderelect.exception.LeaderElectorExceptionNonRecoverable;
import net.lbruun.dbleaderelect.internal.core.SQLLeaderElect;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Instant;
import java.util.EnumSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import javax.sql.DataSource;
import net.lbruun.dbleaderelect.exception.LeaderElectorConfigurationException;
import net.lbruun.dbleaderelect.exception.LeaderElectorPreFlightException;
import net.lbruun.dbleaderelect.internal.utils.SQLUtils;
import net.lbruun.dbleaderelect.internal.utils.ThreadFactoryWithNamePrefix;

/**
 * Leader Elector which uses a table in an ACID-compliant relational database to
 * elect a leader among participating candidates. This class represents a single
 * leader candidate. Candidates are uniquely identified by their {@link #getCandidateId() 
 * candidate id}.
 *
 * <p>
 * The elector will start a background process which at periodic intervals will
 * contact the database to either renew a lease for current leadership or check
 * if existing leader has died (a new leader is then promoted). The result of
 * the periodic process is delivered to the
 * {@link LeaderElectorConfiguration#getListener() registered listener}.
 *
 * <p>
 * The background process will not start immediately rather it will start at a
 * random time within 1/3 of the {@link LeaderElectorConfiguration#getIntervalMs()
 * intervalMs} milliseconds from invocation of the constructor. This
 * <i>jitter</i> makes sure that even if LeaderElectors are started at exactly
 * the same time they will not use the exact same intervals for the leader
 * election process. As a result, contention on the database table will be
 * reduced.
 *
 * <p>
 * {@link #close() Closing} the instance will mean that any current leadership
 * will be relinquished.
 */
public class LeaderElector implements AutoCloseable {

    /**
     * Value used as {@code candidateId} to represent no-leader scenario.
     */
    public static final String NO_LEADER_CANDIDATE_ID = "//noleader//";

    /**
     * Value used as {@code lastSeenTimestamp} to represent no leader scenario.
     */
    public static final long NO_LEADER_LASTSEENTIMESTAMP_MS = 0;
    public static final Instant NO_LEADER_LASTSEENTIMESTAMP = Instant.ofEpochMilli(NO_LEADER_LASTSEENTIMESTAMP_MS);

    private final ScheduledExecutorService executorElector;
    private final ExecutorService executorNotifier;
    private volatile boolean closing = false;
    private final LeaderElectorConfiguration configuration;
    private final DataSource dataSource;
    private final SQLLeaderElect sqlLeaderElect;

    /**
     * Create a leader elector.
     *
     * @param configuration configuration for the leader elector
     * @param dataSource datasource where the leader election table reside. This
     * can be a pooled DataSource.
     * @throws LeaderElectorPreFlightException if startup verification fails
     * (for example if connection to database cannot be obtained)
     */
    public LeaderElector(LeaderElectorConfiguration configuration, DataSource dataSource) throws LeaderElectorPreFlightException {
        long startTime = System.currentTimeMillis();
        configuration.getLeaderElectorLogger().logInfo(
                this.getClass(), "Leader Elector starting (configuration: " + configuration + ")");
        this.dataSource = dataSource;

        // Pre-flight check
        verifyConnection();

        // Amend configuration with auto-detected values
        try {
            this.configuration = LeaderElectorConfiguration.getRuntimeConfiguration(configuration, dataSource);
        } catch (LeaderElectorConfigurationException ex) {
            throw new LeaderElectorPreFlightException("Cannot auto-detect configuration values", ex);
        }

        // Pre-flight check
        String tableNameDisplay = verifyTable();

        // Executors
        executorElector = Executors.newScheduledThreadPool(1, new ThreadFactoryWithNamePrefix("LeaderElector-election"));
        executorNotifier = Executors.newSingleThreadExecutor(new ThreadFactoryWithNamePrefix("LeaderElector-notification"));

        sqlLeaderElect = new SQLLeaderElect(this.configuration, dataSource, tableNameDisplay);
        
        try {
            sqlLeaderElect.ensureRoleRow();
        } catch (SQLException ex) {
            String msg = "Could not insert row into " + tableNameDisplay + " for role_id='" + configuration.getRoleId() + "'";
            throw new LeaderElectorPreFlightException(msg, ex);
        }
        
        start();

        // Logging
        long durationMs = System.currentTimeMillis() - startTime;
        this.configuration.getLeaderElectorLogger().logInfo(
                this.getClass(), "Leader Elector started in " + durationMs + " ms");
    }

    /**
     * Gets the resolved configuration used by the Leader Elector. "Resolved"
     * means that any values which are auto-detected are filled-in. For this
     * reason, the object returned here is not necessarily the same object as
     * was input in the constructor.
     *
     * @return resolved configuration
     */
    public LeaderElectorConfiguration getConfiguration() {
        return configuration;
    }

    private void verifyConnection() throws LeaderElectorPreFlightException {
        int timeoutSecs = 10;
        boolean isValid = false;
        try ( Connection connection = dataSource.getConnection()) {
            isValid = connection.isValid(timeoutSecs);
        } catch (SQLException ex) {
            throw new LeaderElectorPreFlightException("Could not connect to database", ex);
        }

        if (!isValid) {
            throw new LeaderElectorPreFlightException("Could not validate existing connection within " + timeoutSecs + " seconds. The database is potentially deadlocked, sluggish or network is congested");
        }
    }

    private String verifyTable() throws LeaderElectorPreFlightException {
        String schemaName = configuration.getSchemaName();
        String tableName = configuration.getTableName();

        try ( Connection connection = dataSource.getConnection()) {
            String schemaNameDisp = (schemaName == null) ? connection.getSchema() : schemaName;
            schemaNameDisp = (schemaNameDisp == null) ? "" : schemaNameDisp + ".";

            String tableNameDisplay = schemaNameDisp + tableName;
            // Check table exist
            if (!SQLUtils.tableExists(connection, schemaName, tableName)) {
                throw new LeaderElectorPreFlightException("Table " + tableNameDisplay + " does not exist");
            }

            // Check table structure
            SQLUtils.TableColumn[] expectedColumns
                    = new SQLUtils.TableColumn[]{
                        new SQLUtils.TableColumn("role_id", java.sql.Types.VARCHAR, 20),
                        new SQLUtils.TableColumn("candidate_id", java.sql.Types.VARCHAR, 256),
                        new SQLUtils.TableColumn("last_seen_timestamp", java.sql.Types.BIGINT)
                    };
            SQLUtils.tableColumnVerification(connection, schemaName, tableName, expectedColumns);
            
            return tableNameDisplay;
        } catch (SQLException ex) {
            throw new LeaderElectorPreFlightException("Error on initial verification", ex);
        }
    }

    private void start() {

        int maxJitter = (int) (configuration.getIntervalMs() / 3);
        int jitter = ThreadLocalRandom.current().nextInt(maxJitter + 1);
        executorElector.scheduleWithFixedDelay(
                getRunnable(null), // task to execute
                jitter, // initial delay 
                configuration.getIntervalMs(), // subsequent delay
                TimeUnit.MILLISECONDS);
    }

    
    /**
     * Gets the candidate id used by this LeaderElector. Shortcut for
     * {@link #getConfiguration()}.{@link LeaderElectorConfiguration#getCandidateId()}.
     * @return candidate id
     */
    public String getCandidateId() {
        return this.configuration.getCandidateId();
    }
    
    /**
     * Relinquishes current leadership, if any. This will allow other
     * candidates to become leader. The current candidate is excluded from being
     * a leader election until a new (other) leader has been appointed.
     * 
     */
    public void relinquish() {
        if (closing) {
            return;
        }
        executorElector.schedule(getRunnable(Boolean.TRUE), 0, TimeUnit.MILLISECONDS);
    }
    
    /**
     * Gets if the current candidate is currently the leader.
     *
     * This simply returns the result of the last check against the database. It
     * does <i>not</i> connect to the database and is therefore not entirely
     * bullet proof (theoretically, the current candidate may not have kept its lease
     * "alive" in the database and therefore may have lost its leadership to
     * another candidate, however this situation may be unknown to itself)
     *
     * <p>
     * The application should in general react to
     * {@link LeaderElectorListener events} rather than using this method.
     *
     * @throws LeaderElectorExceptionNonRecoverable if this instance is closed
     * @return {@code true} if the current candidate is currently leader
     */
    public boolean isLeader() throws LeaderElectorExceptionNonRecoverable {
        if (closing) {
            throw new LeaderElectorExceptionNonRecoverable("Instance is closed");
        }
        return sqlLeaderElect.isLeader();
    }

    /**
     * Gets if this instance is closed. An instance which is closed can no
     * longer be used and should be discarded.
     *
     * @return true if this instance is closed.
     */
    public boolean isClosed() {
        return closing;
    }

    private Runnable getRunnable(Boolean relinquish) {
        return () -> {
            try {
                LeaderElectorListener.Event event = sqlLeaderElect.electLeader((relinquish != null) ? relinquish : closing);
                sendEvent(event);
                if (event.isNonRecoverableError()) {
                    close();
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        };
    }

    private void sendEvent(final LeaderElectorListener.Event event) {
        // Ignore events if we're closing or closed.
        if (closing) {
            return;
        }
        final EnumSet<LeaderElectorListener.EventType> listenerSubscription = configuration.getListenerSubscription();
        final LeaderElectorListener listener = configuration.getListener();
        if (listenerSubscription.contains(event.getEventType())) {
            executorNotifier.submit(() -> {
                try {
                    listener.onLeaderElectionEvent(event);
                } catch (Exception ex) {
                    String msg = "Error while propagating event from Leader Elector. Leader Elector will be shut down";
                    try {
                        configuration.getLeaderElectorLogger()
                                .logError(listener.getClass(), msg, ex);
                    } catch (Exception exIgnored) {
                        System.err.println("ERROR: " + msg);
                        ex.printStackTrace(System.err);
                    }
                    close();
                }
            });
        } else {
            // Event is ignored as its type doesn't match the subscription
        }
    }

    /**
     * Closes down the Leader Elector. If the current candidate has leadership then
     * such leadership is relinquished on a best-effort basis before close-down.
     *
     * <p>
     * After this method returns, {@link isClosed()}} will return {@code true}.
     * An instance which has been closed cannot be re-used. Calling this method
     * more than once on the same instance has no effect.
     */
    @Override
    public synchronized void close() {
        if (closing) {
            return;
        }
        closing = true;
        long startTime = System.currentTimeMillis();
        this.configuration.getLeaderElectorLogger().logInfo(
                this.getClass(), "Leader Elector is closing down");

        // Make a best-effort attempt at relinquishing current
        // leadership (if any). Events from this attempt are not propagated.
        executorElector.schedule(getRunnable(null), 0, TimeUnit.MILLISECONDS);

        executorElector.shutdown();
        executorNotifier.shutdown();
        long durationMs = System.currentTimeMillis() - startTime;
        this.configuration.getLeaderElectorLogger().logInfo(
                this.getClass(), "Leader Elector closed in " + durationMs + " ms");
    }
}
