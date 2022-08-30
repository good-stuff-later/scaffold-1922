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

import net.lbruun.dbleaderelect.LeaderElectorLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 */
public class SpringLeaderElectorLogger implements LeaderElectorLogger {

    private Logger logger = LoggerFactory.getLogger(SpringLeaderElectorLogger.class);

    @Override
    public void logInfo(Class originatingClass, String msg) {
        logger.info(msg);
    }

    @Override
    public void logError(Class originatingClass, String msg, Throwable thrwbl) {
        logger.error(msg, thrwbl);
    }
}