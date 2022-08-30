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
package net.lbruun.dbleaderelect.internal.core;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import static net.lbruun.dbleaderelect.LeaderElector.NO_LEADER_CANDIDATE_ID;

/**
 * Represents a row read from the leader election table.
 */
public class RowInLeaderElectionTable {

    /**
     * What the database row tells us in terms of leadership status
     */
    public static enum CurrentLeaderDbStatus {
        SOMEONE_ELSE,
        ME,
        NOBODY
    }

    private final String candidateId;
    private final long lastSeenTimestampMillis;
    private final long nowUTCMillis;
    private final long leaseCounter;
    private final CurrentLeaderDbStatus currentLeaderDbStatus;

    public RowInLeaderElectionTable(ResultSet rs, String ownCandidateId) throws SQLException {
        this.candidateId = rs.getString(1);                  // Column: CANDIDATE_ID
        this.lastSeenTimestampMillis = rs.getLong(2);        // Column: LAST_SEEN_TIMESTAMP
        this.nowUTCMillis = rs.getLong(3);                   // Column: <calculated column, constructed DB time>
        this.leaseCounter = rs.getLong(4);                   // Column: LEASE_COUNTER
        this.currentLeaderDbStatus = calcCurrentLeaderDbStatus(ownCandidateId);
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

    public CurrentLeaderDbStatus getCurrentLeaderDbStatus() {
        return currentLeaderDbStatus;
    }

    private CurrentLeaderDbStatus calcCurrentLeaderDbStatus(String ownCandidateId) {
        if (candidateId.equals(ownCandidateId)) {
            return CurrentLeaderDbStatus.ME;
        }
        if (candidateId.equals(NO_LEADER_CANDIDATE_ID)) {
            return CurrentLeaderDbStatus.NOBODY;
        }
        return CurrentLeaderDbStatus.SOMEONE_ELSE;
    }

    @Override
    public String toString() {
        
        Instant nowUTCInstant = Instant.ofEpochMilli(nowUTCMillis);
        Instant lastSeenTimestampInstant = Instant.ofEpochMilli(lastSeenTimestampMillis);
        return "{" 
                + "CANDIDATE_ID=" + candidateId 
                + ", LAST_SEEN_TIMESTAMP=" + lastSeenTimestampMillis + " (" + lastSeenTimestampInstant + ")"
                + ", LEASE_COUNTER=" + leaseCounter 
                + ", nowUTCMillis=" + nowUTCMillis+ " (" + nowUTCInstant + ")"
                + ", (calculated) currentLeaderBbStatus=" + currentLeaderDbStatus
                + "}";
    }
}
