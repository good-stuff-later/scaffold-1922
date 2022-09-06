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
     * Handle errors received with the event.
     * This implementation errs on the side of caution and simply exits
     * the application if there are errors of type non-recoverable.
     * For most application this is the most appropriate strategy.
     */
    private void errorHandling(Event event, LeaderElector leaderElector) {
        if (event.hasErrors()) {
            if (event.isNonRecoverableError()) {
                logErrorsFromEvent("Non-recoverable error from Leader Election", event);
                logger.error("Exiting application because of non-recoverable errors from Leader Election");
                leaderElector.close();
                SpringApplication.exit(applicationContext, () -> 1);
            } else {
                logErrorsFromEvent("Recoverable error from Leader Election", event);
            }
        }
    }
    
    @Override
    public void onLeaderElectionEvent(Event event, LeaderElector leaderElector) {
        logger.trace("Received leader election event : {}", event);
        errorHandling(event, leaderElector);

        switch (event.getEventType()) {
            case LEADERSHIP_ASSUMED:
                logger.info("Leadership assumed from previous leader {}", event.getCandidateId());
                // Here: We can optionally extract 'leaseCounter' from the event
                // and use as a fencing token on an external resource. 
                // This example does not demonstrate this.
                
                // Start doiing leader work ...
                break;
 
            case LEADERSHIP_LOST:
                logger.info("Leadership lost for candidate {0} (me)", event.getCandidateId());

                // Stop doiing leader work ...
                break;
        }

    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
