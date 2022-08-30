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
package net.lbruun.dbleaderelect.internal.sql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import net.lbruun.dbleaderelect.LeaderElectorConfiguration;

/**
 *
 */
public class SQLCmdsMSSQL extends SQLCmds {
    
    // MS SQL Server doesn't support "SELECT FOR UPDATE" but instead
    // supports "WITH (UPDLOCK,HOLDLOCK,ROWLOCK)".
    private static final String SQL_SELECT_TEMPLATE
            = "SELECT "
            + "    candidate_id," // #1
            + "    last_seen_timestamp," // #2
            + "    %s as now_utc_ms," // #3
            + "    lease_counter" // #4
            + " FROM %s"
            + " WITH (UPDLOCK,HOLDLOCK,ROWLOCK)"
            + " WHERE role_id = ?";

    // DATEDIFF_BIG was instroduced in SQL Server 2016
    private static final String SQL_CURRENT_UTC_MS = 
            "DATEDIFF_BIG(millisecond, '1970-01-01 00:00:00', GETUTCDATE())";
    
    private final String selectSQL_MSSQL;

    
    public SQLCmdsMSSQL(LeaderElectorConfiguration configuration) {
        super(configuration);
        this.selectSQL_MSSQL = String.format(SQL_SELECT_TEMPLATE, currentUtcMsExpression(), this.getTabName());
    }

    public String currentUtcMsExpression() {
        return SQL_CURRENT_UTC_MS;
    }

    @Override
    public String getSelectSQL() {
        return this.selectSQL_MSSQL;
    }
    
    @Override
    public PreparedStatement getInsertRoleStmt(Connection connection, String roleId) throws SQLException {
        
        String sql
                = "BEGIN TRY "
                + "  INSERT INTO " + getTabName() + " " + COLUMN_LIST_FOR_INSERT + "  VALUES " + VALUES_LIST_FOR_INSERT + "; "
                + "END TRY "
                + "BEGIN CATCH "
                + "  IF ERROR_NUMBER() NOT IN (2601, 2627) THROW; "
                + "END CATCH;";

        PreparedStatement preparedStatement = connection.prepareStatement(sql);
        preparedStatement.setString(1, roleId);
        return preparedStatement;
    }

}
