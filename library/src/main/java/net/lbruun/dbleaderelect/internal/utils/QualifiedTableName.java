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
package net.lbruun.dbleaderelect.internal.utils;

import java.util.Objects;

/**
 * Table name with an optional schema name.
 *
 */
public class QualifiedTableName {

    private final String schemaName;
    private final String tableName;

    private QualifiedTableName(String schemaName, String tableName) {
        this.schemaName = schemaName;
        this.tableName = tableName;
    }

    public String getSchemaName() {
        return schemaName;
    }

    public String getTableName() {
        return tableName;
    }

    public static void verifyTableName(String qualifiedTableName) throws IllegalArgumentException {
        Objects.requireNonNull(qualifiedTableName, "qualifiedTableName cannot be null");

        long noOfPeriods = qualifiedTableName.chars().filter(ch -> ch == '.').count();
        if (noOfPeriods > 1) {
            throw new IllegalArgumentException("Invalid table name value. Only supported format is: <schemaName.tableName>");
        }
        int pos = qualifiedTableName.indexOf('.');
        if ((pos == 0) || (pos == (qualifiedTableName.length() - 1))) {
            throw new IllegalArgumentException("Invalid table name value. Must not start or end with a period character");
        }
    }

    /**
     * Splits a qualified database table name into its constituents: table name
     * and schema name.
     *
     * @param qualifiedTableName
     * @return
     * @throws IllegalArgumentException
     */
    public static QualifiedTableName get(String qualifiedTableName) throws IllegalArgumentException {
        verifyTableName(qualifiedTableName);

        if (qualifiedTableName.indexOf('.') != -1) {
            String[] split = qualifiedTableName.split("\\.");
            return new QualifiedTableName(split[0], split[1]);
        }
        return new QualifiedTableName(null, qualifiedTableName);
    }
}
