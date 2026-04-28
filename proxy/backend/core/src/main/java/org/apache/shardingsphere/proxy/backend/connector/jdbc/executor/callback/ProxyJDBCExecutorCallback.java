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

package org.apache.shardingsphere.proxy.backend.connector.jdbc.executor.callback;

import javax.sql.rowset.CachedRowSet;
import javax.sql.rowset.RowSetProvider;
import org.apache.shardingsphere.infra.config.props.ConfigurationPropertyKey;
import org.apache.shardingsphere.infra.database.core.type.DatabaseType;
import org.apache.shardingsphere.infra.executor.sql.execute.engine.ConnectionMode;
import org.apache.shardingsphere.infra.executor.sql.execute.engine.driver.jdbc.JDBCExecutorCallback;
import org.apache.shardingsphere.infra.executor.sql.execute.result.ExecuteResult;
import org.apache.shardingsphere.infra.executor.sql.execute.result.query.QueryResult;
import org.apache.shardingsphere.infra.executor.sql.execute.result.query.impl.driver.jdbc.type.memory.JDBCMemoryQueryResult;
import org.apache.shardingsphere.infra.executor.sql.execute.result.query.impl.driver.jdbc.type.stream.JDBCStreamQueryResult;
import org.apache.shardingsphere.infra.executor.sql.execute.result.update.UpdateResult;
import org.apache.shardingsphere.infra.metadata.database.resource.ResourceMetaData;
import org.apache.shardingsphere.infra.spi.type.typed.TypedSPILoader;
import org.apache.shardingsphere.proxy.backend.connector.DatabaseConnector;
import org.apache.shardingsphere.proxy.backend.connector.sane.SaneQueryResultEngine;
import org.apache.shardingsphere.proxy.backend.context.ProxyContext;
import org.apache.shardingsphere.proxy.backend.util.SqlParameterParser;
import org.apache.shardingsphere.proxy.backend.util.SqlParameterParser.SqlParserInfo;
import org.apache.shardingsphere.sql.parser.statement.core.statement.SQLStatement;

import java.sql.*;
import java.util.List;
import java.util.Optional;

/**
 * JDBC executor callback for proxy.
 */
public abstract class ProxyJDBCExecutorCallback extends JDBCExecutorCallback<ExecuteResult> {
    
    private final DatabaseConnector databaseConnector;
    
    private final boolean isReturnGeneratedKeys;
    
    private final boolean fetchMetaData;
    
    private boolean hasMetaData;
    
    protected ProxyJDBCExecutorCallback(final DatabaseType protocolType, final ResourceMetaData resourceMetaData, final SQLStatement sqlStatement,
                                        final DatabaseConnector databaseConnector,
                                        final boolean isReturnGeneratedKeys, final boolean isExceptionThrown, final boolean fetchMetaData) {
        super(protocolType, resourceMetaData, sqlStatement, isExceptionThrown);
        this.databaseConnector = databaseConnector;
        this.isReturnGeneratedKeys = isReturnGeneratedKeys;
        this.fetchMetaData = fetchMetaData;
    }

    @Override
    @SuppressWarnings("SqlInjection")
    public ExecuteResult executeSQL(final String sql, final Statement statement,
                                    final ConnectionMode connectionMode,
                                    final DatabaseType storageType) throws SQLException {
        System.out.println("*******executeSQL3 原始SQL: " + sql);

        hasMetaData = fetchMetaData && !hasMetaData;
        databaseConnector.add(statement);

        // ====================== 只处理含敏感关键字的SQL ======================
        boolean isSensitiveSql = sql.toLowerCase().contains("drop database")
                || sql.toLowerCase().contains("drop table")
                || sql.toLowerCase().contains("alter table")
                || sql.toLowerCase().contains("truncate");

        if (isSensitiveSql) {
            SqlParameterParser.SqlParserInfo parserInfo = SqlParameterParser.parse(sql);
            String targetSql = parserInfo.getParameterSql();
            List<Object> params = parserInfo.getParameters();

            System.out.println("===== 参数化后SQL: " + targetSql);
            System.out.println("===== 提取参数: " + params);

            if (params.isEmpty()) {
                // 无参数直接执行
                boolean isResult = statement.execute(sql);
                if (isResult) {
                    ResultSet resultSet = statement.getResultSet();
                    databaseConnector.add(resultSet);
                    return createQueryResult(resultSet, connectionMode, storageType);
                }
                return new UpdateResult(
                        Math.max(statement.getUpdateCount(), 0),
                        isReturnGeneratedKeys ? getGeneratedKey(statement) : 0L
                );
            }

            try (PreparedStatement pstmt = statement.getConnection().prepareStatement(targetSql)) {
                for (int i = 0; i < params.size(); i++) {
                    Object value = params.get(i);
                    if (value == null) {
                        pstmt.setString(i + 1, null);
                    } else {
                        pstmt.setString(i + 1, value.toString());
                    }
                }

                boolean isResult = pstmt.execute();
                if (isResult) {
                    ResultSet resultSet = pstmt.getResultSet();
                    databaseConnector.add(resultSet);
                    return createQueryResult(resultSet, connectionMode, storageType);
                }

                return new UpdateResult(
                        Math.max(pstmt.getUpdateCount(), 0),
                        isReturnGeneratedKeys ? getGeneratedKey(pstmt) : 0L
                );
            }
        }

        // 普通业务SQL原样执行
        boolean isResult = statement.execute(sql);
        if (isResult) {
            ResultSet resultSet = statement.getResultSet();
            databaseConnector.add(resultSet);
            return createQueryResult(resultSet, connectionMode, storageType);
        }

        return new UpdateResult(
                Math.max(statement.getUpdateCount(), 0),
                isReturnGeneratedKeys ? getGeneratedKey(statement) : 0L
        );
    }


//    @Override
//    public ExecuteResult executeSQL(final String sql, final Statement statement, final ConnectionMode connectionMode, final DatabaseType storageType) throws SQLException {
//        System.out.println("*******executeSQL3:" + sql);
//        hasMetaData = fetchMetaData && !hasMetaData;
//        databaseConnector.add(statement);
//
//        try {
//            System.out.println("*******executeSQL3: executeQuery");
//            ResultSet resultSet = statement.executeQuery(sql);
////            logResultSet(resultSet, sql);
//            databaseConnector.add(resultSet);
//            return createQueryResult(resultSet, connectionMode, storageType);
//        } catch (SQLException e) {
//            if (execute(sql, statement)) {
//                System.out.println("*******executeSQL3: execute");
//                ResultSet resultSet = statement.getResultSet();
////                logResultSet(resultSet, sql);
//                databaseConnector.add(resultSet);
//                return createQueryResult(resultSet, connectionMode, storageType);
//            }
//            System.out.println("*******executeSQL3: UpdateResult");
//            return new UpdateResult(Math.max(statement.getUpdateCount(), 0), isReturnGeneratedKeys ? getGeneratedKey(statement) : 0L);
//        }
//    }


    private void logResultSet(ResultSet resultSet, String sql) throws SQLException {
        ResultSet newResultSet = resultSet;
        ResultSetMetaData metaData = newResultSet.getMetaData();
        int columnCount = metaData.getColumnCount();
        int rowNum = 0;
        while (newResultSet.next()) {
            rowNum++;
            StringBuilder row = new StringBuilder("Row " + rowNum + ": ");
            for (int i = 1; i <= columnCount; i++) {
                Object value = newResultSet.getObject(i);
                row.append(metaData.getColumnName(i)).append("=").append(value).append(" | ");
            }
            System.out.println(row.toString());
        }
    }

//    private void logResultSet(ResultSet resultSet, String sql) throws SQLException {
//        ResultSetMetaData metaData = resultSet.getMetaData();
//        int columnCount = metaData.getColumnCount();
//
//        System.out.println("=== Executed SQL: {} ===" + sql);
//        System.out.println("=== ResultSet Columns (count: {}) ===" + columnCount);
//        // 打印列名
//        StringBuilder colNames = new StringBuilder("Columns: ");
//        for (int i = 1; i <= columnCount; i++) {
//            colNames.append(metaData.getColumnName(i)).append(" | ");
//        }
//        System.out.println(colNames.toString());
//
//        // 打印数据行（注意：遍历后 resultSet 游标会到末尾，若后续还要读取需先复制）
////        resultSet.beforeFirst(); // 重置游标到开头
////        int rowNum = 0;
////        while (resultSet.next()) {
////            rowNum++;
////            StringBuilder row = new StringBuilder("Row " + rowNum + ": ");
////            for (int i = 1; i <= columnCount; i++) {
////                Object value = resultSet.getObject(i);
////                row.append(metaData.getColumnName(i)).append("=").append(value).append(" | ");
////            }
////            System.out.println(row.toString());
////        }
////        resultSet.beforeFirst(); // 再次重置游标，保证后续 createQueryResult 能正常读取
//    }
    
    protected abstract boolean execute(String sql, Statement statement, boolean isReturnGeneratedKeys) throws SQLException;

    protected boolean execute(String sql, Statement statement) throws SQLException {
        return statement.execute(sql);
    }
    
    private QueryResult createQueryResult(final ResultSet resultSet, final ConnectionMode connectionMode, final DatabaseType storageType) throws SQLException {
//        return ConnectionMode.MEMORY_STRICTLY == connectionMode ? new JDBCStreamQueryResult(resultSet) : new JDBCMemoryQueryResult(resultSet, storageType);
//        return new JDBCStreamQueryResult(resultSet);
        return new JDBCMemoryQueryResult(resultSet, storageType);
    }

    private long getGeneratedKey(final Statement statement) throws SQLException {
        try (ResultSet resultSet = statement.executeQuery("SELECT LAST_INSERT_ID()")) {
            return resultSet.next() ? resultSet.getLong(1) : 0L;
        } catch (final SQLFeatureNotSupportedException ignore) {
            return 0L;
        }
    }

    private long getGeneratedKeyIfInteger(final ResultSet resultSet) throws SQLException {
        switch (resultSet.getMetaData().getColumnType(1)) {
            case Types.SMALLINT:
            case Types.INTEGER:
            case Types.BIGINT:
                return resultSet.getLong(1);
            default:
                return 0L;
        }
    }
    
    @Override
    protected final Optional<ExecuteResult> getSaneResult(final SQLStatement sqlStatement, final SQLException ex) {
        return new SaneQueryResultEngine(getProtocolTypeType()).getSaneQueryResult(sqlStatement, ex);
    }
    
    private DatabaseType getProtocolTypeType() {
        DatabaseType configuredDatabaseType = ProxyContext.getInstance()
                .getContextManager().getMetaDataContexts().getMetaData().getProps().getValue(ConfigurationPropertyKey.PROXY_FRONTEND_DATABASE_PROTOCOL_TYPE);
        if (null != configuredDatabaseType) {
            return configuredDatabaseType;
        }
        if (ProxyContext.getInstance().getContextManager().getMetaDataContexts().getMetaData().getDatabases().isEmpty()) {
            return TypedSPILoader.getService(DatabaseType.class, "MySQL");
        }
        return ProxyContext.getInstance().getContextManager().getMetaDataContexts().getMetaData().getDatabases().values().iterator().next().getProtocolType();
    }
}
