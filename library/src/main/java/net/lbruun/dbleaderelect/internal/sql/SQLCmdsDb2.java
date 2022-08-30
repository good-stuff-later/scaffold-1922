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
 * Db2 for Linux, Unix and Windows (aka Ddb2 LUW)
 */
public class SQLCmdsDb2 extends SQLCmds {
    
    // Db2 doesn't support "SELECT FOR UPDATE" but instead
    // supports .. well the below. Note that use of 'READ ONLY' is correct.
    private static final String SQL_SELECT_TEMPLATE
          = "SELECT "
            + "    candidate_id," // #1
            + "    last_seen_timestamp," // #2
            + "    %s as now_utc_ms," // #3
            + "    lease_counter" // #4
            + " FROM %s"
            + " WHERE role_id = ?"
            + " FOR READ ONLY WITH RS USE AND KEEP EXCLUSIVE LOCKS WAIT FOR OUTCOME";
    
    private static final String SQL_CURRENT_UTC_MS = 
            "(DATE_PART('EPOCH', TRUNC_TIMESTAMP(CURRENT_TIMESTAMP(3), 'SS'))*1000)"
            + " +"
            + " (DATE_PART('MILLISECOND', CURRENT_TIMESTAMP(3)) - TRUNC(DATE_PART('MILLISECOND', CURRENT_TIMESTAMP(3)),-3))";

    private final String selectSQL_Db2;

    public SQLCmdsDb2(LeaderElectorConfiguration configuration) {
        super(configuration);
        this.selectSQL_Db2 = String.format(SQL_SELECT_TEMPLATE, currentUtcMsExpression(), this.getTabName());
    }

    public String currentUtcMsExpression() {
        return SQL_CURRENT_UTC_MS;
    }

    @Override
    public String getSelectSQL() {
        return this.selectSQL_Db2;
    }

    @Override
    public PreparedStatement getInsertRoleStmt(Connection connection, String roleId) throws SQLException {
        // Db2's procedural language doesn't have try-catch, instead is has
        // so-called HANDLERs. Below we declare a CONTINUE handler which catches
        // the dup-key error. The Handler itself is empty, it does nothing. (there
        // is no statement between the 'BEGIN' and 'END' in the Handler)
        
        String sql = 
                  "BEGIN"
                + "  DECLARE CONTINUE HANDLER FOR SQLSTATE '23505' BEGIN END;"
                + "  INSERT INTO " + getTabName() + " " + COLUMN_LIST_FOR_INSERT
                + "     VALUES " + VALUES_LIST_FOR_INSERT + ";"
                + "END;";
        PreparedStatement preparedStatement = connection.prepareStatement(sql);
        preparedStatement.setString(1, roleId);
        return preparedStatement;
    }

    @Override
    public String fromNothingString() {
        return "FROM SYSIBM.SYSDUMMY1";
    }


}
