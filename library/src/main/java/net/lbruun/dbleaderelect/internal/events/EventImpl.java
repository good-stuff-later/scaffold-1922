/*
 * Copyright 2022 lbruun.net
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
package net.lbruun.dbleaderelect.internal.events;

import java.time.Instant;
import java.util.Arrays;
import java.util.Objects;
import static net.lbruun.dbleaderelect.LeaderElector.NO_LEADER_LASTSEENTIMESTAMP;
import net.lbruun.dbleaderelect.LeaderElectorListener;
import static net.lbruun.dbleaderelect.LeaderElectorListener.EventType.LEADERSHIP_ASSUMED;
import static net.lbruun.dbleaderelect.LeaderElectorListener.EventType.LEADERSHIP_CONFIRMED;
import static net.lbruun.dbleaderelect.LeaderElectorListener.EventType.LEADERSHIP_LOST;
import static net.lbruun.dbleaderelect.LeaderElectorListener.EventType.LEADERSHIP_NOOP;
import static net.lbruun.dbleaderelect.LeaderElector.NO_LEADER_CANDIDATE_ID;

/**
 *
 */
public class EventImpl implements LeaderElectorListener.Event {

    private final LeaderElectorListener.EventType eventType;
    private final Instant startTime;
    private final Instant eventTime;
    private final String roleId;
    private final String candidateId;
    private final Instant lastSeenTimestamp;
    private final long leaseCounter;
    private final LeaderElectorListener.Event.ErrorEvent[] errors;

    public EventImpl(LeaderElectorListener.EventType eventType, Instant startTime, String roleId, String candidateId, Instant previousLastSeenTimestamp, long newLeaseCounter, LeaderElectorListener.Event.ErrorEvent[] errors) {
        this.startTime = startTime;
        this.eventType = eventType;
        this.roleId = roleId;
        this.candidateId = candidateId;
        this.lastSeenTimestamp = previousLastSeenTimestamp;
        this.leaseCounter = newLeaseCounter;
        this.errors = errors;
        this.eventTime = Instant.now();
    }

    @Override
    public Instant getStartTime() {
        return startTime;
    }

    @Override
    public Instant getEventTime() {
        return eventTime;
    }

    @Override
    public LeaderElectorListener.EventType getEventType() {
        return eventType;
    }

    @Override
    public String getRoleId() {
        return roleId;
    }

    @Override
    public String getCandidateId() {
        return candidateId;
    }

    @Override
    public long getLeaseCounter() {
        return leaseCounter;
    }

    @Override
    public Instant getLastSeenTimestamp() {
        return lastSeenTimestamp;
    }

    @Override
    public LeaderElectorListener.Event.ErrorEvent[] getErrors() {
        return errors;
    }

    @Override
    public String toString() {
        String errorsStr = "";
        String candidateIdDisplay = candidateId;
        String lastSeenTimestampStr = Objects.toString(lastSeenTimestamp, "null");
        switch (eventType) {
            case LEADERSHIP_ASSUMED:
                if (candidateId != null && candidateId.equals(NO_LEADER_CANDIDATE_ID)) {
                    candidateIdDisplay = "<no-previous-leader>";
                }
                if (lastSeenTimestamp != null && lastSeenTimestamp.equals(NO_LEADER_LASTSEENTIMESTAMP)) {
                    lastSeenTimestampStr = "<no-previous-leader>";
                }
            case LEADERSHIP_CONFIRMED:
            case LEADERSHIP_NOOP:
            case LEADERSHIP_LOST: {
                if (errors != null) {
                    errorsStr = ", error = " + Arrays.toString(errors);
                }
                return "[" + eventType + "]"
                        + ", roleId=" + roleId
                        + ", candidateId=" + candidateIdDisplay
                        + ", lastSeenTimestamp=" + lastSeenTimestampStr
                        + ", leaseCounter=" + leaseCounter
                        + errorsStr;
            }
            default:
                return "unknown";
        }
    }
}
