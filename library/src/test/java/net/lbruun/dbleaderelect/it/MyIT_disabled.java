/*
 * Copyright 2022 lbruun.org
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
package net.lbruun.dbleaderelect.it;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.SQLException;
import java.util.EnumSet;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import net.lbruun.dbleaderelect.LeaderElector;
import net.lbruun.dbleaderelect.LeaderElectorConfiguration;
import net.lbruun.dbleaderelect.LeaderElectorListener;
import net.lbruun.dbleaderelect.LeaderElectorListener.Event.ErrorEvent;
import net.lbruun.dbleaderelect.LeaderElectorListener.EventType;
import net.lbruun.dbleaderelect.helpers.LiquibaseRunner;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.junit.jupiter.api.Assertions;

/**
 *
 * @author lars
 */

@Testcontainers
public class MyIT_disabled {
    
    private LiquibaseRunner liquibaseRunner;
    private HikariDataSource dataSource;

    // will be shared between test methods
    @Container
    private static final PostgreSQLContainer POSTGRESQL_CONTAINER = new PostgreSQLContainer("postgres:14.1")
        .withDatabaseName("testdb")
        .withUsername("sa")
        .withPassword("sa");

    
    @BeforeEach
    public void setUp() throws SQLException {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(POSTGRESQL_CONTAINER.getJdbcUrl());
        config.setUsername(POSTGRESQL_CONTAINER.getUsername());
        config.setPassword(POSTGRESQL_CONTAINER.getPassword());
        config.setConnectionTimeout(3000);

        dataSource = new HikariDataSource(config);
        dataSource.setLoginTimeout(3);
        liquibaseRunner = new LiquibaseRunner(dataSource);
        liquibaseRunner.execStart();
    }

    @AfterEach
    public void tearDown() {
       // liquibaseRunner.execEnd();
       // dataSource.close();
    }

    


    @Test
    public void testKilledDatabaseServer() throws InterruptedException, SQLException {
        System.out.println("Test : killedDatabaseServer");
        BlockingQueue<LeaderElectorListener.Event> queue = new LinkedBlockingQueue<>();
        LeaderElectorListener leaderElectorListener = new LeaderElectorListener(){
            @Override
            public void onLeaderElectionEvent(LeaderElectorListener.Event event, LeaderElector leaderElector) {
                queue.add(event);
            }
        };
        LeaderElectorConfiguration leaderElectorConfiguration = LeaderElectorConfiguration.builder()
                .withTableName(liquibaseRunner.getLeaderElectTableName())
                .withListener(leaderElectorListener)
                .withListenerSubscription(EnumSet.allOf(LeaderElectorListener.EventType.class))
                .withIntervalMs(1000)
                .withAssumeDeadMs(8000)
                .build();
        
        // Use a test container dedicated to this test alone
        final PostgreSQLContainer testContainer = new PostgreSQLContainer("postgres:14.1")
                .withDatabaseName("testdb")
                .withUsername("sa")
                .withPassword("sa");
        
        testContainer.start();

        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl(testContainer.getJdbcUrl());
        hikariConfig.setUsername(testContainer.getUsername());
        hikariConfig.setPassword(testContainer.getPassword());
        hikariConfig.setMaximumPoolSize(2);
        hikariConfig.setMinimumIdle(1);
        hikariConfig.setConnectionTimeout(3000);

        HikariDataSource hikariDataSource = new HikariDataSource(hikariConfig);
        hikariDataSource.setLoginTimeout(3);  // value is seconds
        liquibaseRunner = new LiquibaseRunner(hikariDataSource);
        liquibaseRunner.execStart();

        LeaderElector leaderElector = new LeaderElector(leaderElectorConfiguration, hikariDataSource);
        
        LeaderElectorListener.Event event = queue.poll(5, TimeUnit.SECONDS);
        if (event.hasErrors()) {
            for(ErrorEvent ee : event.getErrors()) {
                ee.getError().printStackTrace();
            }
        }
        Assertions.assertNotNull(event);
        Assertions.assertEquals(EventType.LEADERSHIP_ASSUMED, event.getEventType());
        
        System.out.println("Stopping TestContainer");
        testContainer.stop();
        

        LeaderElectorListener.Event event2 = queue.poll(5, TimeUnit.SECONDS);
        Assertions.assertNotNull(event2);
        Assertions.assertEquals(EventType.LEADERSHIP_LOST, event2.getEventType());
        testContainer.close();
    }

    

    @Test
    public void testSomething() throws InterruptedException {
        System.out.println("Test : something");
        
        LeaderElectorConfiguration config = LeaderElectorConfiguration.builder()
                .withTableName(liquibaseRunner.getLeaderElectTableName())
                .withIntervalMs(5000)
                .withAssumeDeadMs(8000)
                .withListenerSubscription(EnumSet.allOf(LeaderElectorListener.EventType.class))
                .build();
        
        try (LeaderElector leaderElector = new LeaderElector(config, dataSource)) {
            Thread.sleep(30000);
        }
    }
    
}
