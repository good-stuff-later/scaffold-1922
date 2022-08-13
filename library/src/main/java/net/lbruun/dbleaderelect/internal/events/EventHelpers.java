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
import java.util.ArrayList;
import java.util.List;
import net.lbruun.dbleaderelect.LeaderElectorListener;
import net.lbruun.dbleaderelect.LeaderElectorListener.Event.ErrorEvent;
import net.lbruun.dbleaderelect.exception.LeaderElectorException;

public class EventHelpers {
    public static LeaderElectorListener.Event createLeadershipAssumedEvent(Instant startTime, String roleId, String previousCandidateId, long previousLastSeenMillisEpoch, long newLeaseCounter) {
        return new EventImpl(LeaderElectorListener.EventType.LEADERSHIP_ASSUMED, startTime, roleId, previousCandidateId, Instant.ofEpochMilli(previousLastSeenMillisEpoch), newLeaseCounter,  null);
    }

    public static LeaderElectorListener.Event createLeadershipLostEvent(Instant startTime, String roleId, String candidateId, long previousLastSeenMillisEpoch, long leaseCounter ) {
        return new EventImpl(LeaderElectorListener.EventType.LEADERSHIP_LOST, startTime, roleId, candidateId, Instant.ofEpochMilli(previousLastSeenMillisEpoch), leaseCounter, null);
    }

    public static LeaderElectorListener.Event createLeadershipConfirmedEvent(Instant startTime, String roleId, String candidateId, long previousLastSeenMillisEpoch, long existingLeaseCounter) {
        return new EventImpl(LeaderElectorListener.EventType.LEADERSHIP_CONFIRMED, startTime, roleId, candidateId, Instant.ofEpochMilli(previousLastSeenMillisEpoch), existingLeaseCounter, null);
    }

    public static LeaderElectorListener.Event createLeadershipNoOp(Instant startTime, String roleId, String candidateId, long previousLastSeenMillisEpoch, long leaseCounter) {
        return new EventImpl(LeaderElectorListener.EventType.LEADERSHIP_NOOP, startTime, roleId, candidateId, Instant.ofEpochMilli(previousLastSeenMillisEpoch), leaseCounter, null);
    }

    public static class ErrorEventsBuilder {
        private final List<ErrorEvent> errorEvents = new ArrayList<>(4);
        
        public void add(LeaderElectorException error) {
            errorEvents.add(new ErrorEventImpl(Instant.now(), error));
        }
        
        public LeaderElectorListener.Event build(Instant startTime, boolean wasLeaderAtStartOfElection, String roleId) {
             return new EventImpl(
                     (wasLeaderAtStartOfElection) ? LeaderElectorListener.EventType.LEADERSHIP_LOST : LeaderElectorListener.EventType.LEADERSHIP_UNDETERMINED,
                     startTime, 
                     roleId,
                     null, // candidateId
                     null, // previousLastSeenTimestamp
                     -1,   // newLeaseCounter
                     errorEvents.toArray(new ErrorEvent[errorEvents.size()])
             );
        }
    }

}
