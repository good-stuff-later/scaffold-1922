/*
 * Copyright 2021 lbruun.
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
package net.lbruun.dbleaderelect.internal.core;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLRecoverableException;
import java.sql.SQLTransientException;
import java.time.Instant;
import javax.sql.DataSource;
import static net.lbruun.dbleaderelect.LeaderElector.NO_LEADER_LASTSEENTIMESTAMP_MS;
import net.lbruun.dbleaderelect.internal.events.EventHelpers;
import net.lbruun.dbleaderelect.LeaderElectorConfiguration;
import net.lbruun.dbleaderelect.exception.LeaderElectorExceptionNonRecoverable;
import net.lbruun.dbleaderelect.exception.LeaderElectorExceptionRecoverable;
import net.lbruun.dbleaderelect.internal.sqltexts.SQLTexts;
import net.lbruun.dbleaderelect.LeaderElectorListener;
import net.lbruun.dbleaderelect.exception.LeaderElectorException;
import static net.lbruun.dbleaderelect.LeaderElector.NO_LEADER_CANDIDATE_ID;

/**
 *
 */
public class SQLLeaderElect {

    private final LeaderElectorConfiguration configuration;
    private final SQLTexts sqlTexts;
    private boolean currentlyAmLeader = false;
    private int noOfConsecutiveTransientErrors = 0;
    private volatile boolean hasHadSuccessfulExection = false;
    
    // Used to keep track of relinquish invocations. If we've volunatarily
    // given up leadership then we should wait for some else to assume leadership.
    // After we see that someone else has assumed leadership then this variable
    // will be reset (i.e. set to false again).
    private boolean hasRelinquishedLeadership = false;
    
    private final String myRoleId;
    private final String myCandidateId;
    private final DataSource dataSource;
    private final String tableNameDisplay;

    public SQLLeaderElect(LeaderElectorConfiguration configuration, DataSource dataSource, String tableNameDisplay) {
        this.dataSource = dataSource;
        this.configuration = configuration;
        this.myRoleId = configuration.getRoleId();
        this.myCandidateId = configuration.getCandidateId();
        this.sqlTexts = SQLTexts.getSQL(configuration);
        this.tableNameDisplay = tableNameDisplay;
    }
    
    public boolean isLeader() {
        return currentlyAmLeader;
    }
    
    public void ensureRoleRow() throws SQLException {
        try (Connection connnection = this.dataSource.getConnection()) {
            try (PreparedStatement p = sqlTexts.getInsertRoleSQL(connnection, this.configuration.getRoleId())) {
                p.executeUpdate();
            }
        }
    }
    
    private void affirmLeadership(Connection connection, String roleId, String candidateId) 
            throws SQLException, LeaderElectorExceptionNonRecoverable {
        try (PreparedStatement pstmt = connection.prepareStatement(sqlTexts.getAffirmLeadershipSQL())) {
            pstmt.setString(1, roleId);
            pstmt.setString(2, candidateId);
            executeUpdate(pstmt);
        }
    }
    
    private void assumeLeadership(Connection connection, String roleId, String candidateId, long newLeaseCounter) 
            throws SQLException, LeaderElectorExceptionNonRecoverable {
        try (PreparedStatement pstmt = connection.prepareStatement(sqlTexts.getAssumeLeadershipSQL())) {
            pstmt.setString(1, candidateId);
            pstmt.setLong(2, newLeaseCounter);
            pstmt.setString(3, roleId);
            executeUpdate(pstmt);
        }
    }
    
    private void relinquishLeadership(Connection connection, String roleId, String candidateId) 
            throws SQLException, LeaderElectorExceptionNonRecoverable {
        try (PreparedStatement pstmt = connection.prepareStatement(sqlTexts.getRelinquishLeadershipSQL())) {
            pstmt.setString(1, roleId);
            pstmt.setString(2, candidateId);
            executeUpdate(pstmt);
        }
    }
    
    private void executeUpdate(PreparedStatement pstmt) 
            throws SQLException, LeaderElectorExceptionNonRecoverable {
        int rowsAffected = pstmt.executeUpdate();
        if (rowsAffected != 1) {
            throw new LeaderElectorExceptionNonRecoverable(rowsAffected + " rows was affected by UPDATE statement. Expected exactly 1 (one) row to be affected.");
        }
    }
    
    private long getNewLeaseCounter(long existingLeaseCounter) {
        return (existingLeaseCounter == Long.MAX_VALUE) ? 0 : existingLeaseCounter + 1;
    }
    
    public LeaderElectorListener.Event electLeader(boolean relinquish) {
        boolean wasLeaderAtStartOfElection = currentlyAmLeader;
        EventHelpers.ErrorEventsBuilder errorHolder = null;   // holds errors
        LeaderElectorListener.Event event = null;   // holds non-error events
        Instant startTime = Instant.now();

        try ( Connection connection = dataSource.getConnection()) {
            boolean originalAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);

            try ( PreparedStatement preparedStatement = connection.prepareStatement(sqlTexts.getSelectSQL())) {
                preparedStatement.setString(1, myRoleId);
                // Set a timeout. We do not wish to wait for the database lock forever.
                preparedStatement.setQueryTimeout(configuration.getQueryTimeoutSecs());

                try ( ResultSet rs = preparedStatement.executeQuery()) {
                    event = executeInsideTableLock(connection, rs, wasLeaderAtStartOfElection, relinquish);
                }
                connection.commit(); // release table lock
            } catch (SQLTransientException | SQLRecoverableException ex) {
                noOfConsecutiveTransientErrors++;
                if (noOfConsecutiveTransientErrors == 3) {
                    noOfConsecutiveTransientErrors = 0;
                    errorHolder = addError(errorHolder, new LeaderElectorExceptionNonRecoverable(ex));
                } else {
                    errorHolder = addError(errorHolder, new LeaderElectorExceptionRecoverable(ex));
                }
            } catch (Exception ex) {
                errorHolder = addError(errorHolder, new LeaderElectorExceptionNonRecoverable(ex));
                try {
                    connection.rollback();
                } catch (SQLException ex2) {
                    errorHolder = addError(errorHolder, new LeaderElectorExceptionNonRecoverable("Error performing rollback", ex2));
                }
            } finally {
                try {
                    connection.setAutoCommit(originalAutoCommit);
                } catch (SQLException ex) {
                    errorHolder = addError(errorHolder, new LeaderElectorExceptionNonRecoverable("Error resetting auto-commit", ex));
                }
            }
        } catch (SQLTransientException ex) {
            // Errors obtaining a connection. Note that Hikari turns *any* exception 
            // related to obtaining a connection into an exception of type 'SQLTransientConnectionException'
            // no matter what the exception from the JDBC driver is.
            if (hasHadSuccessfulExection) {
                errorHolder = addError(errorHolder, new LeaderElectorExceptionRecoverable("No longer able to connect to database", ex));
            } else {
                errorHolder = addError(errorHolder, new LeaderElectorExceptionNonRecoverable("Cannot connect to database (first time)", ex));
            }
        } catch (Exception ex) {
            // Any other type of error
            errorHolder = addError(errorHolder, new LeaderElectorExceptionNonRecoverable(ex));
        }

        if (errorHolder != null) {
            currentlyAmLeader = false;
            return errorHolder.build(startTime, wasLeaderAtStartOfElection, myRoleId);
        } else if (event != null) {
            if (!hasHadSuccessfulExection) {
                hasHadSuccessfulExection = true;
            }
            return event;
        }

        // Sanity check (we should not get here)
        throw new RuntimeException("Unexpected event. Neither 'event' nor 'errorHolder' has a value");
    }

    private LeaderElectorListener.Event executeInsideTableLock(
            Connection connection, 
            ResultSet rs, 
            boolean wasLeaderAtStartOfElection,
            boolean relinquish) throws SQLException, LeaderElectorExceptionNonRecoverable {
        Instant startTime = Instant.now();
        LeaderElectorListener.Event event = null;   // holds non-error events

        int rows = 0;

        while (rs.next()) {
            rows++;
            if (rows > 1) {
                throw new LeaderElectorExceptionNonRecoverable("Table " + tableNameDisplay + " has more than one row. This is unexpected. It must contain exactly one row.");
            }
            RowInLeaderElectionTable row = new RowInLeaderElectionTable(rs, myCandidateId);
            long lastSeenTimestampMillis = row.getLastSeenTimestampMillis();
            long nowUTCMillis = row.getNowUTCMillis();
            long leaseCounter = row.getLeaseCounter();
            CurrentLeader currentLeader = row.getCurrentLeader();
            long leaseAgeMillis = nowUTCMillis - lastSeenTimestampMillis;
            boolean leaseExpired = (leaseAgeMillis >= configuration.getAssumeDeadMs());

            checkRowValidity(row);

            switch (currentLeader) {
                case ME: {
                    if (relinquish) {
                        relinquishLeadership(connection, myRoleId, myCandidateId);
                        currentlyAmLeader = false;
                        hasRelinquishedLeadership = true;
                        if (wasLeaderAtStartOfElection) {
                            event = EventHelpers.createLeadershipLostEvent(startTime, myRoleId, row.getCandidateId(), lastSeenTimestampMillis, leaseCounter);
                        }
                    } else {
                        affirmLeadership(connection, myRoleId, myCandidateId);
                        event = EventHelpers.createLeadershipConfirmedEvent(startTime, myRoleId, myCandidateId, lastSeenTimestampMillis, leaseCounter);
                    }
                }
                break;
                case SOMEONE_ELSE:
                    hasRelinquishedLeadership = false; // Reset because another candidate has assumed leadership
                // intentional fall-thru (no break statement)
                case NOBODY: {
                    if (leaseExpired && (!hasRelinquishedLeadership)) {
                        long newLeaseCounter = getNewLeaseCounter(leaseCounter);
                        assumeLeadership(connection, myRoleId, myCandidateId, newLeaseCounter);
                        event = EventHelpers.createLeadershipAssumedEvent(startTime, myRoleId, row.getCandidateId(), lastSeenTimestampMillis, newLeaseCounter);
                        if (!wasLeaderAtStartOfElection) {
                            currentlyAmLeader = true;
                        }
                    } else {
                        event = EventHelpers.createLeadershipNoOp(startTime, myRoleId, row.getCandidateId(), lastSeenTimestampMillis, leaseCounter);
                    }
                }
                break;
                default:
                    // Sanity 
                    throw new LeaderElectorExceptionNonRecoverable("Unexpected value for 'currentLeader' : " + currentLeader);
            }

            if (event == null) {
                event = EventHelpers.createLeadershipNoOp(startTime, myRoleId, row.getCandidateId(), lastSeenTimestampMillis, leaseCounter);
            }
        }
        
        if (rows == 0) {
            throw new LeaderElectorExceptionNonRecoverable("No row for role_id='" + myRoleId + "' in table " + tableNameDisplay);
        }
        
        return event;
    }
                    
    private void checkRowValidity(RowInLeaderElectionTable row) throws LeaderElectorExceptionNonRecoverable {
        String prefix = "ERROR: Unexpected: Table " + tableNameDisplay + " with content " + row ;
        if (row.getCurrentLeader() == CurrentLeader.ME && (!currentlyAmLeader)) {
            throw new LeaderElectorExceptionNonRecoverable(prefix
                    + ", says current candidate is leader but 'currentlyAmLeader' is false. "
                    + "Possibly table content was altered by an unsolicated process.");
        }

        if (row.getCurrentLeader() != CurrentLeader.ME && (currentlyAmLeader)) {
            throw new LeaderElectorExceptionNonRecoverable(prefix
                    + ", says current candidate is leader, not me, but 'currentlyAmLeader' is true. "
                    + "Possible cause if that current process has not kept its lease alive. Perhaps the process has been dormant? "
                    + "Another possible cause is if candidates do not use the same configuration values for their Leader Elector process "
                    + "(for example, they use different values for 'assumeDeadMs')"
            );
        }
        if (row.getCurrentLeader() == CurrentLeader.NOBODY && row.getLastSeenTimestampMillis() != NO_LEADER_LASTSEENTIMESTAMP_MS) {
            throw new LeaderElectorExceptionNonRecoverable(prefix
                    + ", is inconsistent. "
                    + "Possibly table content was altered by an unsolicated process.");
        }
    }
    
    // Helper to accumulate errorHolder (Exceptions)
    private EventHelpers.ErrorEventsBuilder addError(final EventHelpers.ErrorEventsBuilder errorEventsBuilder, LeaderElectorException error) {
        EventHelpers.ErrorEventsBuilder e = (errorEventsBuilder == null) ? new EventHelpers.ErrorEventsBuilder() : errorEventsBuilder;
        e.add(error);
        return e;
    }
    
        
    private static enum CurrentLeader {
        SOMEONE_ELSE,
        ME,
        NOBODY
    }
    
    private static class RowInLeaderElectionTable {

        private final String candidateId;
        private final long lastSeenTimestampMillis;
        private final long nowUTCMillis;
        private final long leaseCounter;
        private final CurrentLeader currentLeader;
        
        public RowInLeaderElectionTable(ResultSet rs, String ownCandidateId) throws SQLException {
            this.candidateId = rs.getString(1);                  // Column: CANDIDATE_ID
            this.lastSeenTimestampMillis = rs.getLong(2);        // Column: LAST_SEEN_TIMESTAMP
            this.nowUTCMillis = rs.getLong(3);                   // Column: <constructed DB time>
            this.leaseCounter = rs.getLong(4);                   // Column: LEASE_COUNTER
            this.currentLeader = calcCurrentLeader(ownCandidateId);
        }

        public String getCandidateId() {
            return candidateId;
        }

        public long getLastSeenTimestampMillis() {
            return lastSeenTimestampMillis;
        }

        public long getNowUTCMillis() {
            return nowUTCMillis;
        }

        public long getLeaseCounter() {
            return leaseCounter;
        }

        public CurrentLeader getCurrentLeader() {
            return currentLeader;
        }

        private CurrentLeader calcCurrentLeader(String ownCandidateId) {
            if (candidateId.equals(ownCandidateId)) {
                return CurrentLeader.ME;
            }
            if (candidateId.equals(NO_LEADER_CANDIDATE_ID)) {
                return CurrentLeader.NOBODY;
            }
            return CurrentLeader.SOMEONE_ELSE;
        }
        
        @Override
        public String toString() {
            return "{" + "CANDIDATE_ID=" + candidateId + ", LAST_SEEN_TIMESTAMP=" + lastSeenTimestampMillis + ", LEASE_COUNTER=" + leaseCounter + '}';
        }
    }
}
