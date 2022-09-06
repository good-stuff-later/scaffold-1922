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
public class SQLCmdsOracle extends SQLCmds {
    
    private static final String SQL_CURRENT_UTC_MS = 
            "(EXTRACT(DAY FROM (SYSTIMESTAMP - TIMESTAMP '1970-01-01 00:00:00 UTC') * 24 * 60) * 60 + EXTRACT(SECOND FROM SYSTIMESTAMP)) *  1000";

    public SQLCmdsOracle(LeaderElectorConfiguration configuration) {
        super(configuration);
    }

    public String currentUtcMsExpression() {
        return SQL_CURRENT_UTC_MS;
    }

    @Override
    public String fromNothingString() {
        return " FROM DUAL";
    }
    
    @Override
    public PreparedStatement getInsertRoleStmt(Connection connection, String roleId) throws SQLException {
        // The 'ignore_row_on_dupkey_index' hint was introduced in Oracle 11g
        String sql = "INSERT /*+ ignore_row_on_dupkey_index(" + getTabNamePlain() + ",(role_id)) */ INTO " + getTabName() + " " + COLUMN_LIST_FOR_INSERT
                + " VALUES " + VALUES_LIST_FOR_INSERT;
        PreparedStatement preparedStatement = connection.prepareStatement(sql);
        preparedStatement.setString(1, roleId);
        return preparedStatement;
    }

    @Override
    public String getVarcharStr() {
        return "varchar2";
    }

    @Override
    public String getBigIntStr() {
        return "number(38)";
    }

    @Override
    public boolean isTableAlreadyExistException(SQLException ex) {
        return (ex.getSQLState().equals("42000") && (ex.getErrorCode() == 955));
    }
}
