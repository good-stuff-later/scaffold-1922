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

import net.lbruun.dbleaderelect.exception.LeaderElectorExceptionNonRecoverable;
import net.lbruun.dbleaderelect.internal.core.SQLLeaderElect;
import java.sql.SQLException;
import javax.sql.DataSource;
import net.lbruun.dbleaderelect.mocks.LeaderElectTableRow;
import net.lbruun.dbleaderelect.internal.sqltexts.SQLTexts;
import org.junit.jupiter.api.Assertions;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 *
 */
public class SQLLeaderElectTest {

    private static final LeaderElectorConfiguration CONF
            = LeaderElectorConfiguration.builder()
                    .withDatabaseEngine(DatabaseEngine.POSTGRESQL) // Doesn't matter, we are using a mock
                    .build();
    private static final  SQLTexts SQL_TEXTS = SQLTexts.getSQL(CONF);
    private static String TABLENAME_DISPLAY = "testschema.testable";
    
    
    @Test
    public void testStructureError_NoRowsInTable() throws SQLException {
        DataSource mockDS = DbMock.getMockDb(SQL_TEXTS, new LeaderElectTableRow[]{}).getDataSource();
        
        SQLLeaderElect sqlLeaderElect = new SQLLeaderElect(CONF, mockDS,TABLENAME_DISPLAY);
        LeaderElectorListener.Event event = sqlLeaderElect.electLeader(false);
        assertTrue(event.isNonRecoverableError());
    }

    @Test
    public void testStructureError_MoreThanOneRowInTableSelect() throws SQLException {
        
        LeaderElectTableRow row1 = new LeaderElectTableRow();
        LeaderElectTableRow row2 = new LeaderElectTableRow();
        LeaderElectTableRow row3 = new LeaderElectTableRow();
        
        DataSource mockDS = DbMock.getMockDb(SQL_TEXTS, new LeaderElectTableRow[]{row1, row2, row3}).getDataSource();
        
        SQLLeaderElect sqlLeaderElect = new SQLLeaderElect(CONF, mockDS, TABLENAME_DISPLAY);
        
        LeaderElectorListener.Event event = sqlLeaderElect.electLeader(false);
        assertTrue(event.isNonRecoverableError());
    }


    @Test
    public void testStructureError_MoreThanOneRowInTableUpdate() throws SQLException {
        
        LeaderElectTableRow row1 = new LeaderElectTableRow();
        row1.lastSeenTimestamp = 0;
        
        DbMock mockDb = DbMock.getMockDb(SQL_TEXTS, new LeaderElectTableRow[]{row1});
        Mockito.when(mockDb.getAssumeLeadershipStatement().executeUpdate()).thenReturn(9);
        
        SQLLeaderElect sqlLeaderElect = new SQLLeaderElect(CONF, mockDb.getDataSource(), TABLENAME_DISPLAY);
        LeaderElectorListener.Event event = sqlLeaderElect.electLeader(false);
        assertTrue(event.isNonRecoverableError());
    }


    @Test
    public void testAssumeLeaderShip() throws SQLException, LeaderElectorExceptionNonRecoverable {
        
        LeaderElectTableRow row1 = new LeaderElectTableRow();
        row1.lastSeenTimestamp = 0;
        row1.leaseCounter = 0;
        
        DbMock mockDb = DbMock.getMockDb(SQL_TEXTS, new LeaderElectTableRow[]{row1});
        Mockito.when(mockDb.getAssumeLeadershipStatement().executeUpdate()).thenReturn(1);
        
        SQLLeaderElect sqlLeaderElect = new SQLLeaderElect(CONF, mockDb.getDataSource(), TABLENAME_DISPLAY);
        
        LeaderElectorListener.Event event = sqlLeaderElect.electLeader(false);
        
        Assertions.assertEquals(LeaderElectorListener.EventType.LEADERSHIP_ASSUMED, event.getEventType());
        Assertions.assertEquals(row1.candidateId, event.getCandidateId());
        Assertions.assertEquals(row1.leaseCounter +1, event.getLeaseCounter());
        Assertions.assertNull(event.getErrors());
    }
    
}
