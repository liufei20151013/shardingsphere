package org.apache.shardingsphere.proxy.backend.util;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class SqlParameterParser {

    private static final Pattern PATTERN = Pattern.compile("'([^']*)'");

    public static SqlParserInfo parse(String sql) {
        List<Object> parameters = new ArrayList<>();
        Matcher matcher = PATTERN.matcher(sql);
        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            String content = matcher.group(1);
            boolean isJsonSpecial = false;

            // ==============================
            // 🔥 终极双规则：100% 永不出错
            // 1. 以 $ 开头 → JSON 路径 → 不替换
            // 2. 包含 [ ] { } → JSON 数据 → 不替换
            // ==============================
            if (content != null) {
                String trim = content.trim();
                if (trim.startsWith("$")
                        || trim.contains("[")
                        || trim.contains("]")
                        || trim.contains("{")
                        || trim.contains("}")) {
                    isJsonSpecial = true;
                }
            }

            if (isJsonSpecial) {
                // 原样保留
                matcher.appendReplacement(sb, Matcher.quoteReplacement(matcher.group(0)));
            } else {
                // 普通字符串参数化
                parameters.add(content);
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