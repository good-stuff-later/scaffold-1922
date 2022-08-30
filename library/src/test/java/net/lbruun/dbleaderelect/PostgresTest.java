/*
 * Copyright 2021 lbruun.
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
package net.lbruun.dbleaderelect;

import net.lbruun.dbleaderelect.helpers.LiquibaseRunner;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.AfterAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 *
 */
public class PostgresTest {
    
    private static DataSource DS;
    private static String tmpTableName;
    private static LiquibaseRunner liquibaseRunner;

    public PostgresTest() {
    }

    @BeforeAll
    public static void setUpClass() throws SQLException {
        System.out.println("BeforeAll: Setting up DataSource, creating temp table and populating it.");
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:postgresql://localhost:5432/leaderelect");
        config.setUsername("postgres");
        config.setPassword("postgres");

        DS = new HikariDataSource(config);
        
        tmpTableName = getTmpTableName();
        liquibaseRunner = new LiquibaseRunner(DS, tmpTableName);
        liquibaseRunner.execStart();
        
        try (LeaderElector leaderElector = new LeaderElector(
                LeaderElectorConfiguration.builder()
                .withTableName(tmpTableName).build(), 
                DS)) {
        }
    }
    
    @AfterAll
    public static void tearDownClass() throws SQLException {
        System.out.println("AfterAll: Dropping tables.");
        liquibaseRunner.execEnd();

        System.out.println("AfterAll: Closing connection pool.");
        ((HikariDataSource) DS).close();
    }
    
    @BeforeEach
    public void setUp() {
    }
    
    @AfterEach
    public void tearDown() {
    }

    @Test
    public void testSelectForUpdate() throws InterruptedException {
        
        AtomicInteger threadCount = new AtomicInteger(0);
        ExecuteSelectForUpdateThread thread1 = new ExecuteSelectForUpdateThread("Thread1", DS, threadCount, true);
        ExecuteSelectForUpdateThread thread2 = new ExecuteSelectForUpdateThread("Thread2", DS, threadCount, false);
        thread1.start();
        thread1.waitForInside();
        thread2.start();
        thread2.waitForOutside();

        Thread.sleep(2000);
        int threadCount1 = threadCount.get();
        thread1.continueExecution();
        
        
        // Wait for threads to finish
        thread1.join();
        thread2.join();
        int threadCount2 = threadCount.get();
        assertEquals(1, threadCount1);
        assertEquals(2, threadCount2);
    }
    
    
    /**
     * Tests if the "SELECT FOR UPDATE" is truly only letting one database
     * session in at a time. This is meant to test if the locking mechanism in
     * the database behaves as expected.
     *
     * <p>
     * This is not a perfect test. But close enough.
     */
    @Test
    public void testSelectForUpdateMany()  {
        System.out.println("testSelectForUpdateMany");
        assertAll(() -> {
            final ReentrantLock lock = new ReentrantLock();
            int noOfThreads = 10;
            int noOfIterationsPerThread = 40;
            Thread[] threads = new Thread[noOfThreads];
            for (int i = 0; i < noOfThreads; i++) {
                Thread thread = new Thread(runSelectForUpdateIter(i + 1, DS, tmpTableName, lock, noOfIterationsPerThread));
                thread.setUncaughtExceptionHandler((Thread th, Throwable ex) -> {
                    ex.printStackTrace();
                    throw new RuntimeException(ex);
                });
                thread.start();
                threads[i] = thread;
            }
            // Wait for all threads to finish
            for (Thread t : threads) {
                try {
                    t.join();
                } catch (InterruptedException ex) {
                    throw new RuntimeException(ex);
                }
            }
        });
    }
    
    
    private Runnable runSelectForUpdateIter(
            int threadNo, 
            final DataSource dataSource, 
            final String tableName, 
            final ReentrantLock lock, 
            final int iterations) {
        return () -> {
            System.out.println("Thread-" + threadNo + " starting.");
            try ( Connection connection = dataSource.getConnection()) {
                boolean originalAutoCommit = connection.getAutoCommit();
                connection.setAutoCommit(false);
                PreparedStatement stmt = connection.prepareStatement(" ffffff");
                for (int i = 0; i < iterations; i++) {
                    System.out.println("Thread-" + threadNo + " . Execution no: " + (i+1));
                    runSelectForUpdate(connection, tableName, lock);
                }
                connection.setAutoCommit(originalAutoCommit);
            } catch (SQLException ex) {
                throw new RuntimeException(ex);
            }
        };
    }

    private void runSelectForUpdate(Connection connection, String tableName, ReentrantLock lock) throws SQLException {
        try ( Statement stmt = connection.createStatement()) {
            boolean wasLocked = false;
            try ( ResultSet rs = stmt.executeQuery("SELECT * FROM " + tmpTableName + " FOR UPDATE")) {
                // Is someone else already in here?
                // We should be the only ones here!!
                if (lock.isLocked()) {
                    throw new RuntimeException("Lock is already held. This is unexpected. It means some other thread is also here.");
                } else {
                    lock.lock();
                    wasLocked = true;
                }
                if (!rs.next()) {
                    throw new RuntimeException("Table is empty. This is unexpected.");
                }
            } catch (SQLException ex) {
                int errorCode = ex.getErrorCode();
                String sqlState = ex.getSQLState();
                ex.printStackTrace();
                throw ex;
            } finally {
                if (wasLocked) {
                    lock.unlock();
                }
                connection.commit();
            }
        }
    }
    
    private static String getTmpTableName() {
        Random random = new Random();
        return "tmp_" 
                + System.currentTimeMillis() 
                + "_"
                +  Math.abs(random.nextLong());
    }
    
    private class ExecuteSelectForUpdateThread extends Thread {

        private final DataSource dataSource;
        private final CountDownLatch gate;
        private final CountDownLatch amInside;
        private final CountDownLatch amOutside;
        private final String threadName;
        private final AtomicInteger threadCount;

        public ExecuteSelectForUpdateThread(String threadName, DataSource dataSource, AtomicInteger threadCount, boolean wait) {
            this.threadName = threadName;
            this.dataSource = dataSource;
            this.gate = (wait) ? new CountDownLatch(1) : null;
            this.amInside = new CountDownLatch(1);
            this.amOutside = new CountDownLatch(1);
            this.threadCount = threadCount;
        }

        public void continueExecution() {
            gate.countDown();
        }

        public void waitForInside() {
            try {
                amInside.await();
            } catch (InterruptedException ex) {
            }
        }
        
        public void waitForOutside() {
            try {
                amOutside.await();
            } catch (InterruptedException ex) {
            }
        }
        
        public void run() {
            
            log("Starting thread");

            try ( Connection connection = dataSource.getConnection()) {
                boolean originalAutoCommit = connection.getAutoCommit();
                connection.setAutoCommit(false);

                try ( Statement stmt = connection.createStatement()) {
                    amOutside.countDown();
                    try ( ResultSet rs = stmt.executeQuery("SELECT * FROM " + tmpTableName + " FOR UPDATE")) {
                        log("Inside SELECT FOR UPDATE");
                        threadCount.incrementAndGet();
                        amInside.countDown();
                        if (gate != null) {
                            try {
                                log("Waiting to continue ...");
                                gate.await();
                            } catch (InterruptedException ex) {
                            }
                        }
                    } finally {
                        connection.commit();
                    }
                }
                connection.setAutoCommit(originalAutoCommit);
            } catch (SQLException ex) {
                throw new RuntimeException(ex);
            }
        }
        
        private void log(String msg) {
            System.out.println(threadName + ": " + msg);
        }
    }

}
