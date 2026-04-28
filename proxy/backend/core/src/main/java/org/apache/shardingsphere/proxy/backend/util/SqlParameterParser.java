package org.apache.shardingsphere.proxy.backend.util;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class SqlParameterParser {

    private static final Pattern STRING_PATTERN = Pattern.compile("'([^']*(?:''[^']*)*)'", Pattern.DOTALL);

    public static SqlParserInfo parse(String sql) {
        List<Object> parameters = new ArrayList<>();
        Matcher matcher = STRING_PATTERN.matcher(sql);
        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            String content = matcher.group(1);
            boolean keep = false;

            // 1. JSON路径/JSON数据 不替换
            if (content != null) {
                String trim = content.trim();
                if (trim.startsWith("$")
                        || trim.contains("[")
                        || trim.contains("]")
                        || trim.contains("{")
                        || trim.contains("}")) {
                    keep = true;
                }

                boolean isSensitiveParam = trim.toLowerCase().contains("drop database")
                        || trim.toLowerCase().contains("drop table")
                        || trim.toLowerCase().contains("alter table")
                        || trim.toLowerCase().contains("truncate");
                if (isSensitiveParam) {
                    keep = false;
                }
            }

            if (keep) {
                // 原样保留
                matcher.appendReplacement(sb, matcher.group(0));
            } else {
                // 把转义字符还原为真实格式（解决 \n \" 问题）
                String realContent = content
                        .replace("\\n", "\n")
                        .replace("\\r", "\r")
                        .replace("\\\"", "\"")
                        .replace("\\'", "'");

                // 普通字符串（包括带换行/特殊字符的）全部参数化
                parameters.add(realContent);
                matcher.appendReplacement(sb, "?");
            }
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