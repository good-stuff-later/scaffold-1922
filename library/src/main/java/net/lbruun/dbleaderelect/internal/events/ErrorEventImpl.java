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
import net.lbruun.dbleaderelect.LeaderElectorListener.Event.ErrorEvent;
import net.lbruun.dbleaderelect.exception.LeaderElectorException;

/**
 *
 */
public class ErrorEventImpl implements ErrorEvent {

    private final Instant errorEventTime;
    private final LeaderElectorException error;

    public ErrorEventImpl(Instant errorEventTime, LeaderElectorException error) {
        this.errorEventTime = errorEventTime;
        this.error = error;
    }

    @Override
    public LeaderElectorException getError() {
        return error;
    }


    @Override
    public Instant getErrorEventTime() {
        return errorEventTime;
    }

    @Override
    public String toString() {
        return "ErrorEvent{" + "errorEventTime=" + errorEventTime + ", error=" + error + '}';
    }
}
