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

import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Simulates a thread doing work that only leaders should do.
 */
public class LeaderWork implements Runnable {

    private static final Logger LOGGER = Logger.getLogger(LeaderWork.class.getName());
    private final BlockingDeque<Action> queue = new LinkedBlockingDeque<>();

    private enum Action {
        START,
        STOP,
        EXIT
    }

    public LeaderWork() {
        new Thread(this).start();
    }

    @Override
    public void run() {
        boolean doLeaderWork = false;
        boolean run = true;
        while (run) {
            try {
                Action action = queue.pollLast(500, TimeUnit.MILLISECONDS);
                if (action != null) {
                    switch (action) {
                        case START:
                            doLeaderWork = true;
                            break;
                        case STOP:
                            doLeaderWork = false;
                            break;
                        case EXIT:
                            run = false;
                            break;
                    }
                }
            } catch (InterruptedException ex) {
                LOGGER.log(Level.SEVERE, "Unexpected", ex);
                break;  
            }
            if (doLeaderWork) {
                System.out.println("Leader work : doing tasks ...");
            }
        }
        System.out.println("Leader work: exiting ...");
    }

    private void put(Action action, boolean last) {
        try {
            if (last) {
                queue.putLast(action);
            } else {
                queue.putFirst(action);
            }
        } catch (InterruptedException ex) {
            LOGGER.log(Level.SEVERE, "Unexpected", ex);
        }
    }

    /**
     * Start (or resume) doing leader work.
     */
    public void start() {
        put(Action.START, true);
    }

    /**
     * Stop doing leader work.
     */
    public void stop() {
        put(Action.STOP, true);
    }

    /**
     * Exit leader work thread (this)
     */
    public void exit() {
        put(Action.EXIT, false);
    }
}
