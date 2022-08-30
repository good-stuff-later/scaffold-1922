/*
 * Copyright 2022 lbruun.
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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import javax.sql.DataSource;
import net.lbruun.dbleaderelect.mocks.LeaderElectTableRow;
import net.lbruun.dbleaderelect.internal.sql.SQLCmds;
import org.mockito.AdditionalAnswers;
import org.mockito.Mockito;

/**
 *
 */
public class DbMock {

    private final PreparedStatement selectStatement ;
    private final PreparedStatement affirmLeadershipStatement ;
    private final PreparedStatement assumeLeadershipStatement ;
    private final PreparedStatement relinquishLeadershipStatement ;
    private final Connection connection;

    public DbMock(Connection connection, PreparedStatement selectStatement, PreparedStatement affirmLeadershipStatement, PreparedStatement assumeLeadershipStatement, PreparedStatement relinquishLeadershipStatement) {
        this.connection = connection;
        this.selectStatement = selectStatement;
        this.affirmLeadershipStatement = affirmLeadershipStatement;
        this.assumeLeadershipStatement = assumeLeadershipStatement;
        this.relinquishLeadershipStatement = relinquishLeadershipStatement;
    }

    public Connection getConnection() {
        return connection;
    }

    public PreparedStatement getAffirmLeadershipStatement() {
        return affirmLeadershipStatement;
    }

    public PreparedStatement getAssumeLeadershipStatement() {
        return assumeLeadershipStatement;
    }

    public PreparedStatement getRelinquishLeadershipStatement() {
        return relinquishLeadershipStatement;
    }

    public PreparedStatement getSelectStatement() {
        return selectStatement;
    }

    public static DbMock getMockDb(SQLCmds sqlCmds, LeaderElectTableRow[] rows) throws SQLException {
        return getMockDb(sqlCmds, rows, null);
    }
    public static DbMock getMockDb(SQLCmds sqlCmds, Throwable selectStatementError) throws SQLException {
        return getMockDb(sqlCmds, null, selectStatementError);
    }

    
    private static DbMock getMockDb(SQLCmds sqlCmds, LeaderElectTableRow[] rows, Throwable selectStatementError) throws SQLException {

        Connection connection = Mockito.mock(Connection.class);

        // selectStatement
        PreparedStatement selectStatement = Mockito.mock(PreparedStatement.class);
        if (selectStatementError != null) {
            Mockito.when(selectStatement.executeQuery()).thenThrow(selectStatementError);
        } else {
            ResultSet resultSet = getResultSet(rows);
            Mockito.when(selectStatement.executeQuery()).thenReturn(resultSet);
        }
        Mockito.when(connection.prepareStatement(sqlCmds.getSelectSQL())).thenReturn(selectStatement);

        
        // affirmLeadershipStatement
        PreparedStatement affirmLeadershipStatement = Mockito.mock(PreparedStatement.class);
//        Mockito.when(affirmLeadershipStatement.executeUpdate()).thenReturn(1);
        Mockito.when(connection.prepareStatement(sqlCmds.getAffirmLeadershipSQL())).thenReturn(affirmLeadershipStatement);
        

        // assumeLeadershipStatement
        PreparedStatement assumeLeadershipStatement = Mockito.mock(PreparedStatement.class);
//        Mockito.when(assumeLeadershipStatement.executeUpdate()).thenReturn(1);
        Mockito.when(connection.prepareStatement(sqlCmds.getAssumeLeadershipSQL())).thenReturn(assumeLeadershipStatement);

        // assumeLeadershipStatement
        PreparedStatement relinquishLeadershipStatement = Mockito.mock(PreparedStatement.class);
//        Mockito.when(relinquishLeadershipStatement.executeUpdate()).thenReturn(1);
        Mockito.when(connection.prepareStatement(sqlCmds.getRelinquishLeadershipSQL())).thenReturn(relinquishLeadershipStatement);
        
        return new DbMock(connection, selectStatement, affirmLeadershipStatement, assumeLeadershipStatement, relinquishLeadershipStatement);
    }

    public DataSource getDataSource() throws SQLException {
        DataSource dataSource = Mockito.mock(DataSource.class);
        Mockito.when(dataSource.getConnection()).thenReturn(connection);
        return dataSource;
    }
    
    public static ResultSet getResultSet(LeaderElectTableRow[] rows) throws SQLException {
        ResultSet resultSet = Mockito.mock(ResultSet.class);
        
        ArrayList<Boolean> listOfNextBool = new ArrayList<>();
        ArrayList<String> listOfNodeId = new ArrayList<>();
        ArrayList<Long> listOfLastSeenTimestamp = new ArrayList<>();
        ArrayList<Long> listOfNowUtcMs = new ArrayList<>();
        ArrayList<Long> listOfLeaseCounter = new ArrayList<>();
        for (LeaderElectTableRow r : rows) {
            listOfNextBool.add(Boolean.TRUE);
            listOfNodeId.add(r.candidateId);
            listOfLastSeenTimestamp.add(r.lastSeenTimestamp);
            listOfNowUtcMs.add(r.nowUtcMs);
            listOfLeaseCounter.add(r.leaseCounter);
        }
        listOfNextBool.add(Boolean.FALSE);

        Mockito.when(resultSet.next()).thenAnswer(AdditionalAnswers.returnsElementsOf(listOfNextBool));

        // This can definitely be improved upon. It assumes that
        // column values are retrieved exactly once for each row in the
        // ResultSet. This is how code which traverses a ResultSet normally 
        // behaves so it is good enough for our test.
        Mockito.when(resultSet.getString(1)).thenAnswer(AdditionalAnswers.returnsElementsOf(listOfNodeId));
        Mockito.when(resultSet.getLong(2)).thenAnswer(AdditionalAnswers.returnsElementsOf(listOfLastSeenTimestamp));
        Mockito.when(resultSet.getLong(3)).thenAnswer(AdditionalAnswers.returnsElementsOf(listOfNowUtcMs));
        Mockito.when(resultSet.getLong(4)).thenAnswer(AdditionalAnswers.returnsElementsOf(listOfLeaseCounter));
        return resultSet;
    }
}
