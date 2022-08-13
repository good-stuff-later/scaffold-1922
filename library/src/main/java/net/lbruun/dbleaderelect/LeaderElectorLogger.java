/*
 * Copyright 2022 lbruun.net.
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

/**
 * Catches informational startup and closedown messages from a Leader Elector
 * instance. Users who wish to forward such messages to the logging framework 
 * of their choice must implement this interface.
 */
public interface LeaderElectorLogger {
    
    
    /**
     * Logger which does nothing.
     */
    public static final LeaderElectorLogger NULL_LOGGER =  new LeaderElectorLogger(){
        @Override
        public void logInfo(Class klazz, String message) {
        }

        @Override
        public void logError(Class klazz, String message, Throwable cause) {
        }
    };
    
    /**
     * Logger which simply sends the message to {@code stdout}.
     */
    public static final LeaderElectorLogger STDOUT_LOGGER = new LeaderElectorLogger(){
        @Override
        public void logInfo(Class klazz, String message) {
            System.out.println(klazz.getName() + ": " + message);
        }

        @Override
        public void logError(Class klazz, String message, Throwable cause) {
            System.err.println(klazz.getName() + "ERROR: " + message);
            if (cause != null) {
                cause.printStackTrace(System.err);
            }
        }
    };
        
    
    /**
     * Gets called when there are informational messages from the Leader Elector
     * related to startup and close-down of the Leader Elector instance.
     *
     * <p>
     * For example messages like the following:
     * <pre>
     * Leader Elector starting (configuration: candidateId=19476@apollo, tableName=...)
     * Leader Elector using POSTGRESQL database engine (auto-detected)
     * Leader Elector started in 16 ms
     * ...
     * Leader Elector is closing down
     * Leader Elector closed in 2 ms
     * </pre>
     * 
     * <p>
     * The method must return quickly and must not throw exceptions.
     *
     * @param klazz the class which emitted the message.
     * @param message message to be logged
     */
    public void logInfo(Class klazz, String message);

    /**
     * Gets called when there are problems delivering events into the
     * application (your application!), meaning an exception was caught when
     * delivering an event via
     * {@link LeaderElectorListener#onLeaderElectionEvent(LeaderElectorListener.Event)}.
     * Such exception is delivered here but it is much better simply to make
     * sure that the {@link LeaderElectorListener} implementation doesn't throw
     * exceptions in the first place.
     *
     * <p>
     * The method must return quickly and must not throw exceptions.
     * 
     * @param klazz the class which emitted the message.
     * @param message message to be logged
     * @param cause the exception
     */
    public void logError(Class klazz, String message, Throwable cause);

}
