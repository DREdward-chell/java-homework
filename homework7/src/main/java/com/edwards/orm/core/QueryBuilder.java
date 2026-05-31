package com.edwards.orm.core;

import com.edwards.orm.annotation.Param;

import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class QueryBuilder {
    private static final Pattern PLACEHOLDER = Pattern.compile(":(\\w+)");

    private QueryBuilder() {}

    public record ParsedQuery(String sql, Object[] params) {}

    private static String parseParamName(Parameter parameter) {
        Param param = parameter.getAnnotation(Param.class);
        return param != null ? param.value() : parameter.getName();
    }

    public static ParsedQuery queryFromTemplate(String queryTemplate, Object[] args, Parameter[] parameters, String tableName) {
        Map<String, Object> named = new HashMap<>();
        named.put("tableName", tableName);
        for (int i = 0; i < parameters.length; i++) {
            named.put(parseParamName(parameters[i]), args[i]);
        }

        List<Object> ordered = new ArrayList<>();
        Matcher m = PLACEHOLDER.matcher(queryTemplate);
        StringBuilder sql = new StringBuilder();
        while (m.find()) {
            String name = m.group(1);
            if (!named.containsKey(name)) {
                throw new IllegalArgumentException("Unbound query parameter: :" + name);
            }
            if ("tableName".equals(name)) {
                m.appendReplacement(sql, Matcher.quoteReplacement(tableName));
            } else {
                ordered.add(named.get(name));
                m.appendReplacement(sql, "?");
            }
        }
        m.appendTail(sql);
        return new ParsedQuery(sql.toString(), ordered.toArray());
    }
}
