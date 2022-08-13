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
package net.lbruun.dbleaderelect.internal.sqltexts;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import net.lbruun.dbleaderelect.LeaderElectorConfiguration;

/**
 *
 */
public class SQLTextsPostgreSQL extends SQLTexts {
    
    private static final String SQL_CURRENT_UTC_MS = 
            "CAST(EXTRACT(EPOCH FROM CURRENT_TIMESTAMP)*1000 AS bigint)";

    public SQLTextsPostgreSQL(LeaderElectorConfiguration configuration) {
        super(configuration);
    }

    public String currentUtcMsExpression() {
        return SQL_CURRENT_UTC_MS;
    }

    @Override
    public PreparedStatement getInsertRoleSQL(Connection connection, String roleId) throws SQLException {
        String sql = "INSERT INTO " + getTabName() +  " " + getInsertColumnList()
                   +  " VALUES " + getInsertValuesListWithDefaults()
                   +  " ON CONFLICT DO NOTHING";
        PreparedStatement preparedStatement = connection.prepareStatement(sql);
        preparedStatement.setString(1, roleId);
        return preparedStatement;
    }
}
