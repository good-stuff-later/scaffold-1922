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

import javax.sql.DataSource;
import net.lbruun.dbleaderelect.LeaderElector;
import net.lbruun.dbleaderelect.LeaderElectorConfiguration;
import net.lbruun.dbleaderelect.LeaderElectorListener;
import static net.lbruun.dbleaderelect.LeaderElectorListener.ALL_EVENT_TYPES;
import net.lbruun.dbleaderelect.LeaderElectorLogger;
import org.postgresql.ds.PGSimpleDataSource;

/**
 *
 */
public class LeaderElectionFactory  {
    private LeaderElectionFactory(){}

    
    public static LeaderElector getLeaderElector(LeaderElectorListener listener, LeaderElectorLogger logger) {
        return new LeaderElector(configuration(listener, logger), dataSource());
    }

    private static LeaderElectorConfiguration configuration(LeaderElectorListener listener, LeaderElectorLogger logger) {
        return LeaderElectorConfiguration.builder()
                .withListenerSubscription(ALL_EVENT_TYPES)
                .withListener(listener)
                .withLogger(logger)
                .build();
    }

    private static DataSource dataSource() {
        PGSimpleDataSource ds = new PGSimpleDataSource();
        ds.setServerNames(new String[]{"localhost"});
        ds.setDatabaseName("leaderelect");
        ds.setUser("postgres");
        ds.setPassword("postgres");
        return ds;
    }

}
