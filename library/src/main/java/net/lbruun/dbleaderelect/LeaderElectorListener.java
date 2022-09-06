/*
 * Copyright 2021 lbruun.org
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

import java.time.Instant;
import java.util.EnumSet;
import net.lbruun.dbleaderelect.exception.LeaderElectorException;
import net.lbruun.dbleaderelect.exception.LeaderElectorExceptionNonRecoverable;

/**
 * Consumer for events from Leader Elector.
 */
public interface LeaderElectorListener {
    
    
    public static final EnumSet<EventType> ALL_EVENT_TYPES = EnumSet.allOf(EventType.class);
    /**
     * Event type.
    **/ 
    public enum EventType {
        
        /**
         * Candidate is now leader.
         */
        LEADERSHIP_ASSUMED,
        
        /**
         * Candidate has lost its leadership role. This is an abnormal event.
         * 
         * <p>
         * This can happen for several reasons:
         * <ol>
         *   <li>Leadership was voluntarily relinquished by a call to
         *       {@link LeaderElector#relinquish()}.
         *   </li>
         *   <li>There was a technical error during processing, for example an 
         *       temporary error in accessing the database. The logic will in this case err
         *       on the side of caution and treat it as a case of lost leadership
         *       regardless if the error 
         *       In this scenario {@link Event#getErrors() Event.getErrors()} will return a non-null value.
         *   </li>
         * </ol>
         */
        LEADERSHIP_LOST,
        
        /**
         * Leadership cannot be determined. This is an abnormal event.
         *
         * <p>
         * The candidate was not the leader before the leader election process
         * which triggered this event. However, there was an error during
         * processing so it cannot be determined if a new leader should be
         * promoted. If other candidates experience the same problem then the
         * system as a whole may be without leader until the problem is
         * resolved.
         *
         * <p>
         * For this event type, {@link Event#getErrors() Event.getErrors()} will 
         * always return a non-null value.
         */
        LEADERSHIP_UNDETERMINED,
        
        /**
         * Candidate has successfully re-affirmed its existing leadership by
         * renewing its lease. An application will not need to react to this
         * event as it is status quo.
         */
        LEADERSHIP_CONFIRMED,
       
        /**
         * Candidate has successfully checked if it should assume leadership
         * but found this to be unnecessary as there is an existing active
         * leader. An application will not need to react to this
         * event as it is status quo.
         */
        LEADERSHIP_NOOP,
        

        /**
         * A serious error has occurred in the leader election process. This
         * will typically be a database error.
         * 
         * <p>
         * During normal operation this event should not happen. If this event
         * occurs there will be a risk that no candidates can be promoted to leader
         * and there will therefore potentially be no leader. If the application
         * can live with this situation for a period of time then the
         * application can treat the event as a recoverable one, meaning the
         * application can simply log the error as a warning and hope the
         * problem goes away. 
         * 
         * <p>
         * As a minimum the application should always log this event.
         *
         */
        //ERROR
    }
    
    /**
     * Callback method for events from the leader election process.This method is always invoked on the same dedicated thread.<p>
     * The method body must not throw exceptions. If the method indeed throws an
     * exception then the Leader Elector will be
     * {@link LeaderElector#close() closed} and will no longer be usable. To
     * avoid this, the recommendation is to wrap the code in a try-catch, for
     * example:
     *
     * <pre>
     * &#64;Override 
     * public void onLeaderElectionEvent(Event event) {
     *     try {
     *         // Do processing
     *     } catch (Exception ex) {
     *         // Log exception
     *     }
     * }
     * </pre>
     * 
     * The method must execute swiftly as it blocks the thread where
     * notifications from the leader election process happens. It recommended to
     * offload any heavier processing to another thread so that this method
     * returns quickly.
     *
     * @param event
     * @param leaderElector the instance of LeaderElector which produced the event
     */
    public void onLeaderElectionEvent(Event event, LeaderElector leaderElector);
    
    /**
     * Event from the leader election process.
     */
    public interface Event {
        
        /**
         * Start time of the leader election which was the source 
         * of this event. By comparing this value with {@link #getEventTime()}
         * it is possible to get the duration of the leader election. 
         * A leader election will normally only take milliseconds to perform.
         * If there is a slow connection to the database or if database is itself
         * slow then a leader election may take longer.
         * 
         * @return timestamp of start
         */
        public Instant getStartTime();

        /**
         * Time when the event occurred.
         * @return 
         */
        public Instant getEventTime();

        /**
         * Type of event.
         * @see EventType
         * @return event type
         */
        public EventType getEventType();

        /**
         * RoleId to which the event relates. 
         * @return role id
         */
        public String getRoleId();
        
        /**
         * CandidateId.
         * 
         * <p>
         * Has value for these event types:
         * <ul>
         *    <li>{@link EventType#LEADERSHIP_ASSUMED LEADERSHIP_ASSUMED}. The
         *        value is the {@code candidateId} of the previous leader. If there
         *        was no previous leader then this value will be {@link LeaderElector#NO_LEADER_CANDIDATE_ID NO_LEADER_CANDIDATEID}.</li>
         *    <li>{@link EventType#LEADERSHIP_CONFIRMED LEADERSHIP_CONFIRMED}</li>
         * </ul>
         * otherwise {@code null}.
         * 
         */ 
        public String getCandidateId();

        
        /**
         * Lease counter. A value which increments by 1 (one) every time a new
         * leader is elected. This can be used as <i>fencing token</i> on
         * external services. 
         * 
         * <p>
         * Fencing tokens can provide insurance against the situation where
         * a leader has been overthrown but hasn't realized it himself.
         * Example: Suppose {@code CandidateA} is the current leader. For whatever
         * unforeseen reason {@code CandidateA} goes asleep and therefore doesn't renew
         * its lease. Eventually another leader will be promoted, say for example
         * {@code CandidateB}. However, imagine that now {@code CandidateA} wakes up again.
         * It has in-memory knowledge that it is (was!) the leader and starts doing
         * tasks that only leaders should do. This is a problem because at the 
         * same time {@code CandidateB} also believes it is leader and it too is doing
         * tasks that only leaders should do. This is where a fencing token may
         * be useful: It can be supplied to an external service which can then
         * check that the supplied number is never <i>smaller</i> than the previously
         * supplied number. If so, it must reject the requested action. In the
         * example this would mean that the service would reject requests
         * from {@code CandidateA} if the service has already begun to accept 
         * requests from {@code CandidateB}. With this we have ensured that the
         * external service is only servicing one leader at a time. And 
         * this has been accomplished without the external service needing to
         * have knowledge about the leader election: all it needs to do is
         * to apply a very simple rule.  The obvious downside to the use
         * of fencing tokens is that the external service must play along.
         * 
         * <p>
         * Note: The value increments until it reaches its max value 2<sup>63</sup>-1 
         * (9,223,372,036,854,775,807). The next value after this will be 0 (zero) 
         * and the cycle starts over. Because leaders are expected to be long-lived
         * and because leader election only takes places every 
         * {@link LeaderElectorConfiguration#getIntervalMs()} milliseconds
         * it is very unlikely that this rollover will ever happen.
         * 
         * <p>
         * Has value for these event types:
         * <ul>
         *    <li>{@link EventType#LEADERSHIP_ASSUMED LEADERSHIP_ASSUMED} - value is the <i>new</i> lease counter</li>
         *    <li>{@link EventType#LEADERSHIP_NOOP LEADERSHIP_NOOP} - value is the <i>existing</i> lease counter</li>
         * </ul>
         * otherwise the value {@code -1}.
         * 
         * @return lease counter
         */
        public long getLeaseCounter();
        
        /**
         * Timestamp at which the {@link #getCandidateId()} was last seen.
         * This is measured by the clock of the database.
         * 
         * <p>
         * Has value for these event types:
         * <ul>
         *    <li>{@link EventType#LEADERSHIP_ASSUMED LEADERSHIP_ASSUMED}</li>
         *    <li>{@link EventType#LEADERSHIP_CONFIRMED LEADERSHIP_CONFIRMED}</li>
         * </ul>
         * otherwise {@code null}.
         * @return timestamp or {@code null}
         */
        public Instant getLastSeenTimestamp();

        /**
         * Error during processing.
         *
         * <p>
         * Has value for these event types:
         * <ul>
         *    <li>{@link EventType#LEADERSHIP_LOST LEADERSHIP_LOST}</li>
         *    <li>{@link EventType#LEADERSHIP_UNDETERMINED LEADERSHIP_UNDETERMINED}</li>
         * </ul>
         * otherwise {@code null}.
         * 
         * <p>
         * There may be several errors in the returned array. Errors appear in
         * the array in the order they occurred. The first error in the
         * array is the true error while the other errors in the array are
         * most often simply consequences of the first one.
         * 
         * @return the error(s) which has/have occurred or {@code null} for any
         *    other type of event than the ones listed.
         */
        public ErrorEvent[] getErrors();

        /**
         * If errors were encountered during processing.
         * When [@code true}, then {@link #getErrors()} will have at least
         * one element.
         */
        default public boolean hasErrors() {
            return (getErrors() != null && getErrors().length > 0);
        }

        /**
         * If the event represents a non-recoverable error?
         * 
         * <p>
         * Depending on the application's environment and how important 
         * leader election is to the application, it may indeed be prudent to
         * simply exit the application with error when this type of event
         * occurs.
         */
        default public boolean isNonRecoverableError() {
            if (!hasErrors()) {
                return false;
            }
            for (ErrorEvent errEvent : getErrors()) {
                Throwable ex = errEvent.getError();
                if (ex != null && ex instanceof LeaderElectorExceptionNonRecoverable) {
                    return true;
                }
            }
            return false;
        }
    
        /**
         * Error event from the leader election process.
         */
        public interface ErrorEvent {

            /**
             * The error. This will typically be of type 
             * {@link java.sql.SQLException SQLException}.
             */
            public LeaderElectorException getError();

            /**
             * Exact time when error occurred.
             */
            public Instant getErrorEventTime();

        }
    }
    
    
    /**
     * No-op implementation of {@code LeaderElectorListener} which simply prints 
     * the event to {@code stdout} / {@code stderr}.
     * 
     * <p>
     * Events of type {@link EventType#LEADERSHIP_LOST LEADERSHIP_LOST} are printed on {@code stderr},
     * any other type on {@code stdout}.
     */
    public static final class NoOpListener implements LeaderElectorListener {

        @Override
        public void onLeaderElectionEvent(Event event, LeaderElector leaderElector) {
            String prefixTxt = "Event from Leader Election process: ";
            switch (event.getEventType()) {
                case LEADERSHIP_LOST: {
                    System.err.println(prefixTxt + event);
                }
                default: {
                    System.out.println(prefixTxt + event);
                }
            }
        }
    }
}
