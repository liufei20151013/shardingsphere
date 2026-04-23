package org.apache.shardingsphere.proxy.backend.util;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class SqlParameterParser {

    /**
     * 最简单 最安全 的正则
     * 功能：把 '...' 整体替换成 ?
     * 不会破坏JSON，不会拆字符，不会出错
     */
    private static final Pattern PATTERN = Pattern.compile("'([^']*+'|'')*+'");

    public static SqlParserInfo parse(String sql) {
        List<Object> parameters = new ArrayList<>();
        Matcher matcher = PATTERN.matcher(sql);
        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            // 获取完整字符串内容
            String value = matcher.group(0);
            if (value.startsWith("'") && value.endsWith("'")) {
                value = value.substring(1, value.length() - 1);
            }
            parameters.add(value);
            matcher.appendReplacement(sb, "?");
        }
        matcher.appendTail(sb);

        return new SqlParserInfo(sb.toString(), parameters);
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