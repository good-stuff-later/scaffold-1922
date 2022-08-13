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
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Implementation of LeaderElectorLogger for Spring-based applications.
 */
public class LeaderElectorLoggerSpring implements LeaderElectorLogger {

    // Caching of named loggers.
    // It is likely that the underlying logger implementation already
    // does this but we cannot be sure. 
    private static final ConcurrentMap<Class,Log> LOGGERS = new ConcurrentHashMap<>();
    
    @Override
    public void logInfo(Class originatingClass, String msg) {
        Log log = getNamedLogger(originatingClass);
        log.info(msg);
    }

    @Override
    public void logError(Class originatingClass, String msg, Throwable throwable) {
        Log log = getNamedLogger(originatingClass);
        log.error(msg, throwable);
    }
    
    private Log getNamedLogger(Class klazz) {
        return LOGGERS.computeIfAbsent(klazz, LogFactory::getLog);
    }
}
