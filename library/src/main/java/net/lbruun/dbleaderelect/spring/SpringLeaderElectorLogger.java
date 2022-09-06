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
package net.lbruun.dbleaderelect.spring;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import net.lbruun.dbleaderelect.LeaderElectorLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Logger for LeaderElector based on SL4J. Useful in Spring Boot applications.
 */
public class SpringLeaderElectorLogger implements LeaderElectorLogger {

    // This caching is likely to be redundant if the underlying logging 
    // implementation also does this. 
    // TODO: Consider removing this caching.
    private static final ConcurrentMap<Class, Logger> LOGGERS = new ConcurrentHashMap<>();

    @Override
    public void logInfo(Class originatingClass, String msg) {
        getCachedLogger(originatingClass).info(msg);
    }

    @Override
    public void logError(Class originatingClass, String msg, Throwable thrwbl) {
        getCachedLogger(originatingClass).error(msg, thrwbl);
    }
    
    private Logger getCachedLogger(Class klass) {
        return LOGGERS.computeIfAbsent(klass, LoggerFactory::getLogger);
    }
}
