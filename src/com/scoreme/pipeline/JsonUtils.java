package com.scoreme.pipeline;

import java.util.*;

/**
 * Lightweight, zero-dependency JSON parser and serializer for Java 17.
 * Handles parsing instance JSON files and serializing results.
 */
public class JsonUtils {

    public static String serializeResult(Map<String, Object> map) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        int count = 0;
        int size = map.size();
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            count++;
            sb.append("  \"").append(entry.getKey()).append("\": ");
            sb.append(toJsonValue(entry.getValue()));
            if (count < size) {
                sb.append(",");
            }
            sb.append("\n");
        }
        sb.append("}");
        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    private static String toJsonValue(Object obj) {
        if (obj == null) return "null";
        if (obj instanceof String) return "\"" + escape((String) obj) + "\"";
        if (obj instanceof Boolean || obj instanceof Number) return obj.toString();
        if (obj instanceof Map) {
            Map<String, Object> map = (Map<String, Object>) obj;
            StringBuilder sb = new StringBuilder("{");
            int i = 0;
            for (Map.Entry<String, Object> e : map.entrySet()) {
                if (i > 0) sb.append(", ");
                sb.append("\"").append(e.getKey()).append("\": ").append(toJsonValue(e.getValue()));
                i++;
            }
            sb.append("}");
            return sb.toString();
        }
        if (obj instanceof List) {
            List<?> list = (List<?>) obj;
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < list.size(); i++) {
                if (i > 0) sb.append(", ");
                sb.append(toJsonValue(list.get(i)));
            }
            sb.append("]");
            return sb.toString();
        }
        return "\"" + escape(obj.toString()) + "\"";
    }

    private static String escape(String s) {
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\b", "\\b")
                .replace("\f", "\\f")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    /**
     * Simple JSON Parser for Instance JSON format.
     */
    public static Map<String, Object> parseJson(String jsonStr) {
        jsonStr = jsonStr.trim();
        if (!jsonStr.startsWith("{") || !jsonStr.endsWith("}")) {
            throw new IllegalArgumentException("Invalid JSON object format");
        }
        Map<String, Object> result = new LinkedHashMap<>();
        int i = 1;
        int len = jsonStr.length() - 1;
        while (i < len) {
            i = skipWhitespace(jsonStr, i);
            if (i >= len || jsonStr.charAt(i) == '}') break;

            // Key string
            if (jsonStr.charAt(i) != '"') {
                i++;
                continue;
            }
            int keyEnd = jsonStr.indexOf('"', i + 1);
            String key = jsonStr.substring(i + 1, keyEnd);
            i = jsonStr.indexOf(':', keyEnd) + 1;
            i = skipWhitespace(jsonStr, i);

            // Parse Value
            ParseResult valRes = parseValue(jsonStr, i);
            result.put(key, valRes.value);
            i = valRes.nextIdx;

            i = skipWhitespace(jsonStr, i);
            if (i < len && jsonStr.charAt(i) == ',') {
                i++;
            }
        }
        return result;
    }

    private static int skipWhitespace(String str, int idx) {
        while (idx < str.length() && Character.isWhitespace(str.charAt(idx))) {
            idx++;
        }
        return idx;
    }

    private static class ParseResult {
        Object value;
        int nextIdx;
        ParseResult(Object value, int nextIdx) {
            this.value = value;
            this.nextIdx = nextIdx;
        }
    }

    private static ParseResult parseValue(String str, int idx) {
        idx = skipWhitespace(str, idx);
        char c = str.charAt(idx);

        if (c == '"') {
            int end = str.indexOf('"', idx + 1);
            while (end > 0 && str.charAt(end - 1) == '\\') {
                end = str.indexOf('"', end + 1);
            }
            String val = str.substring(idx + 1, end);
            return new ParseResult(val, end + 1);
        } else if (c == '[') {
            List<Object> list = new ArrayList<>();
            idx++;
            while (idx < str.length()) {
                idx = skipWhitespace(str, idx);
                if (str.charAt(idx) == ']') {
                    idx++;
                    break;
                }
                ParseResult elem = parseValue(str, idx);
                list.add(elem.value);
                idx = skipWhitespace(str, elem.nextIdx);
                if (idx < str.length() && str.charAt(idx) == ',') {
                    idx++;
                }
            }
            return new ParseResult(list, idx);
        } else if (c == '{') {
            // Nested map
            int braceCount = 1;
            int start = idx;
            idx++;
            while (idx < str.length() && braceCount > 0) {
                if (str.charAt(idx) == '{') braceCount++;
                else if (str.charAt(idx) == '}') braceCount--;
                idx++;
            }
            String sub = str.substring(start, idx);
            return new ParseResult(parseJson(sub), idx);
        } else {
            // Number or boolean
            int start = idx;
            while (idx < str.length() && str.charAt(idx) != ',' && str.charAt(idx) != ']' && str.charAt(idx) != '}' && !Character.isWhitespace(str.charAt(idx))) {
                idx++;
            }
            String valStr = str.substring(start, idx).trim();
            if (valStr.equals("true")) return new ParseResult(true, idx);
            if (valStr.equals("false")) return new ParseResult(false, idx);
            if (valStr.contains(".")) {
                return new ParseResult(Double.parseDouble(valStr), idx);
            } else {
                return new ParseResult(Long.parseLong(valStr), idx);
            }
        }
    }
}
