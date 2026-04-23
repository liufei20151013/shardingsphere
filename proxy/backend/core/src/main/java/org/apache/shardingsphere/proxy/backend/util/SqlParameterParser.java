package org.apache.shardingsphere.proxy.backend.util;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 纯手工 SQL 参数化工具：把 'xxx' 字符串值 替换为 ?
 * 无任何第三方依赖，兼容所有版本
 */
public final class SqlParameterParser {

    private static final Pattern STRING_PATTERN = Pattern.compile("'([^']*)'");

    private SqlParameterParser() {
    }

    public static SqlParserInfo parse(final String originalSql) {
        try {
            List<Object> parameters = new ArrayList<>();
            StringBuilder resultSql = new StringBuilder();
            Matcher matcher = STRING_PATTERN.matcher(originalSql);
            int index = 0;

            while (matcher.find()) {
                String value = matcher.group(1);
                parameters.add(value);
                resultSql.append(originalSql, index, matcher.start());
                resultSql.append("?");
                index = matcher.end();
            }

            resultSql.append(originalSql.substring(index));
            return new SqlParserInfo(resultSql.toString(), parameters);
        } catch (Throwable e) {
            return new SqlParserInfo(originalSql, new ArrayList<>());
        }
    }

    public static class SqlParserInfo {
        private final String parameterSql;
        private final List<Object> parameters;

        public SqlParserInfo(String parameterSql, List<Object> parameters) {
            this.parameterSql = parameterSql;
            this.parameters = parameters;
        }

        public String getParameterSql() {
            return parameterSql;
        }

        public List<Object> getParameters() {
            return parameters;
        }
    }
}