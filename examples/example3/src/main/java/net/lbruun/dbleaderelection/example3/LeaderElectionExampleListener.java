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
package net.lbruun.dbleaderelection.example3;

import net.lbruun.dbleaderelect.LeaderElector;
import net.lbruun.dbleaderelect.LeaderElectorListener;
import net.lbruun.dbleaderelect.LeaderElectorListener.Event;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 *
 */
public class LeaderElectionExampleListener implements LeaderElectorListener, ApplicationContextAware {

    private static final Logger logger = LoggerFactory.getLogger(LeaderElectionExampleListener.class);

    private ApplicationContext applicationContext;

    /**
     * Print all exceptions on event
     *
     * @param msg message to log with the exception
     * @param event
     */
    private void logErrorsFromEvent(String msg, Event event) {
        for (Event.ErrorEvent errEvent : event.getErrors()) {
            if (event.isNonRecoverableError()) {
                logger.error(msg, errEvent.getError());
            } else {
                logger.warn(msg, errEvent.getError());
            }
        }
    }

    /**
     * Ends the application with error. Before doing so the LeaderElector is
     * closed meaning that an attempt is made to relinquish (from the table)
     * current leadership, if any. This makes it faster for other nodes
     * (candidates) to assume leadership.
     *
     * @param leaderElector 
     */
    private void endAppWithError(LeaderElector leaderElector) {
        leaderElector.close();
        SpringApplication.exit(applicationContext, () -> 1);
    }
    
    /**
     * Handle errors received with the event. This implementation errs on the
     * side of caution and simply exits the application if there are errors of
     * type non-recoverable. For most application this is the most appropriate
     * strategy.
     */
    private void errorHandling(Event event, LeaderElector leaderElector) {
        if (event.hasErrors()) {
            if (event.isNonRecoverableError()) {
                logErrorsFromEvent("Non-recoverable error from Leader Election", event);
                logger.error("Exiting application because of non-recoverable errors from Leader Election");
                endAppWithError(leaderElector);
            } else {
                logErrorsFromEvent("Recoverable error from Leader Election", event);
            }
        }
    }

    @Override
    public void onLeaderElectionEvent(Event event, LeaderElector leaderElector) {
        // Note: This executes on a notification thread owned by the LeaderElector.
        //       We must execute quickly in order to free up the thread for 
        //       subsequent work. Operations which may block for extended time 
        //       is a bad idea here.
        
        logger.trace("Received leader election event : {}", event);
        errorHandling(event, leaderElector);

        switch (event.getEventType()) {
            case LEADERSHIP_ASSUMED:
                logger.info("Leadership assumed from previous leader {}", event.getCandidateId());
                // Here: We can optionally extract 'leaseCounter' from the event
                // and use as a fencing token on an external resource. 
                // This example does not demonstrate this.
                long fencingToken = event.getLeaseCounter();

                // Start doing leader work ...
                break;

            case LEADERSHIP_LOST:
                logger.info("Leadership lost for candidate {0} (me)", event.getCandidateId());

                // Stop doing leader work ...
                break;
        }

    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
