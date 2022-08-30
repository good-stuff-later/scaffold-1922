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
package net.lbruun.dbleaderelection.example1;

import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import net.lbruun.dbleaderelect.LeaderElector;
import net.lbruun.dbleaderelect.LeaderElectorListener;
import net.lbruun.dbleaderelect.LeaderElectorLogger;

/**
 *
 */
public class LeaderElectionExample implements LeaderElectorListener, LeaderElectorLogger {

    private static final Logger LOGGER = Logger.getLogger(LeaderElectionExample.class.getName());
    private final LeaderElector leaderElector;
    private final HttpServer httpServer;
    private final LeaderWork leaderWork = new LeaderWork();

    public LeaderElectionExample() throws IOException {
        leaderElector = LeaderElectionFactory.getLeaderElector(this, this);
        httpServer = HttpServer.create(new InetSocketAddress(8008), 0);
        HttpContext httpContext = httpServer.createContext("/");
        httpContext.setHandler(this::handleRequest);
        httpServer.start();
    }

    public static void main(String[] args) throws IOException {
        
        // Configure J.U.L Logging
        if (System.getProperty("java.util.logging.config.file") == null) {
            try ( InputStream inputStream = LeaderElectionExample.class.getResourceAsStream("/logging.properties")) {
                LogManager.getLogManager().readConfiguration(inputStream);
            }
        }
        
        new LeaderElectionExample();
    }

    private void handleRequest(HttpExchange exchange) throws IOException {
        String response
                = "<html>"
                + "<body>"
                + "<h1>Leader Election - example</h1>"
                + "Candidate id : <code>" + leaderElector.getCandidateId() + "</code><br><br><br>"
                + "<a href=\"EXIT\">EXIT</a><br><br>"
                + "<a href=\"RELINQUISH\">RELINQUISH</a>"
                + "</body>"
                + "</html>";
        
        String path = exchange.getRequestURI().getPath();
        if (path.endsWith("EXIT")) {
            endApp(0);
        } else if (path.endsWith("RELINQUISH")) {
            leaderElector.relinquish();
        }
        
        exchange.getResponseHeaders().add("Content-Type", "text/html");
        exchange.sendResponseHeaders(200, response.getBytes().length);//response code and length
        OutputStream os = exchange.getResponseBody();
        os.write(response.getBytes());
        os.close();
    }
    
    
    private void endApp(int exitCode) {
        leaderWork.exit();
        leaderElector.close();
        System.exit(exitCode);
    }

    /**
     * Callback for Leader Election events.
     * 
     * This is executed on an internal notification thread. Be sure the method
     * executes quickly so as to not block the thread.
     */
    @Override
    public void onLeaderElectionEvent(Event event) {
        
        LOGGER.log(Level.FINEST, "Received leader election event : {0}", event);
        errorHandling(event);

        switch (event.getEventType()) {
            case LEADERSHIP_ASSUMED:
                LOGGER.log(Level.INFO, "Leadership assumed from candidate {0}", event.getCandidateId());
                // Here: We can optionally extract 'leaseCounter' from the event
                // and use as a fencing token on an external resource. 
                // This example does not demonstrate this.
                
                leaderWork.start();
                break;
 
            case LEADERSHIP_LOST:
                LOGGER.log(Level.INFO, "Leadership lost for candidate {0} (me)", event.getCandidateId());
                leaderWork.stop();
                break;

        }
    }

    /**
     * Handle errors received with the event.
     * This implementation errs on the side of caution and simply exits
     * the application if there are errors of type non-recoverable.
     * For most application this is the most appropriate strategy.
     */
    private void errorHandling(Event event) {
        if (event.hasErrors()) {
            if (event.isNonRecoverableError()) {
                logErrorsFromEvent(Level.SEVERE, "Non-recoverable error from Leader Election", event);
                LOGGER.log(Level.SEVERE, "Exiting application because of non-recoverable errors from Leader Election");
                endApp(1);
            } else {
                logErrorsFromEvent(Level.WARNING, "Recoverable error from Leader Election", event);
            }
        }
    }

    /**
     * Print all exceptions on event
     * 
     * @param logLevel the log level
     * @param msg message to log with the exception
     * @param event 
     */
    private void logErrorsFromEvent(Level logLevel, String msg, Event event) {
        for (Event.ErrorEvent errEvent : event.getErrors()) {
            LOGGER.log(logLevel, msg, errEvent.getError());
        }
    }

    
    
    // From interface LeaderElectorLogger
    // Emits some internal stuff from the LeaderElector process.
    
    @Override
    public void logInfo(Class sourceClass, String msg) {
        LOGGER.log(Level.INFO, msg);
    }

    @Override
    public void logError(Class sourceClass, String msg, Throwable thrwbl) {
        LOGGER.log(Level.SEVERE, msg, thrwbl);
    }
}
