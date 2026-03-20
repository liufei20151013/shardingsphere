/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.shardingsphere.infra.database.core.metadata.database.datatype;

import com.cedarsoftware.util.CaseInsensitiveMap;
import org.apache.shardingsphere.infra.database.core.type.DatabaseType;
import org.apache.shardingsphere.infra.database.core.type.DatabaseTypeRegistry;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

/**
 * Data type loader.
 */
public final class DataTypeLoader {
    
    /**
     * Load data type.
     *
     * @param databaseMetaData database meta data
     * @param databaseType database type
     * @return data type map
     * @throws SQLException SQL exception
     */
    public Map<String, Integer> load(final DatabaseMetaData databaseMetaData, final DatabaseType databaseType) throws SQLException {
        Map<String, Integer> result = loadStandardDataTypes(databaseMetaData);
        result.putAll(new DatabaseTypeRegistry(databaseType).getDialectDatabaseMetaData().getExtraDataTypes());
        return result;
    }
    
    private Map<String, Integer> loadStandardDataTypes(final DatabaseMetaData databaseMetaData) throws SQLException {
        Map<String, Integer> result = new CaseInsensitiveMap<>();

        // NDS 不支持getTypeInfo 方法
//        try (ResultSet resultSet = databaseMetaData.getTypeInfo()) {
//            while (resultSet.next()) {
//                System.out.println("********loadStandardDataTypes TYPE_NAME:" + resultSet.getString("TYPE_NAME"));
//                System.out.println("********loadStandardDataTypes DATA_TYPE:" + resultSet.getString("DATA_TYPE"));
//                result.put(resultSet.getString("TYPE_NAME"), resultSet.getInt("DATA_TYPE"));
//            }
//        }

        result.put("BIGINT", -5);
        result.put("BIGINT UNSIGNED", -5);
        result.put("BINARY", -2);
        result.put("BIGINT", -5);
        result.put("BIT", -7);
        result.put("BLOB", -4);
        result.put("BOOL", 16);
        result.put("CHAR", 1);
        result.put("DATE", 91);
        result.put("DATETIME", 93);
        result.put("DECIMAL", 3);
        result.put("DOUBLE PRECISION", 8);
        result.put("DOUBLE PRECISION UNSIGNED", 8);
        result.put("DOUBLE", 8);
        result.put("DOUBLE UNSIGNED", 8);
        result.put("ENUM", 1);
        result.put("FLOAT", 7);
        result.put("INT", 4);
        result.put("INT UNSIGNED", 4);
        result.put("INTEGER", 4);
        result.put("INTEGER UNSIGNED", 4);
        result.put("LONG VARBINARY", -4);
        result.put("LONG VARCHAR", -1);
        result.put("LONGBLOB", -4);
        result.put("LONGTEXT", -1);
        result.put("MEDIUMBLOB", -4);
        result.put("MEDIUMINT", 4);
        result.put("MEDIUMINT UNSIGNED", 4);
        result.put("MEDIUMTEXT", -1);
        result.put("NUMERIC", 3);
        result.put("REAL", 8);
        result.put("SET", 1);
        result.put("SMALLINT", 5);
        result.put("SMALLINT UNSIGNED", 5);
        result.put("TEXT", -1);
        result.put("TIME", 92);
        result.put("TIMESTAMP", 93);
        result.put("TINYBLOB", -3);
        result.put("TINYINT", -6);
        result.put("TINYINT UNSIGNED", -6);
        result.put("TINYTEXT", 12);
        result.put("VARBINARY", -3);
        result.put("VARCHAR", 12);
        result.put("YEAR", 91);
        return result;
    }
}
