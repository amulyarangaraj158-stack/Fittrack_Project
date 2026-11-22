package com.fittrack.server;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Lightweight, dependency-free JSON serializer and flat-object parser
 * for the pure Java HttpServer backend.
 */
public final class JsonHelper {

    private JsonHelper() {
    }

    public static String toJson(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof Number || value instanceof Boolean) {
            return value.toString();
        }
        if (value instanceof String s) {
            return "\"" + escape(s) + "\"";
        }
        if (value instanceof Map<?, ?> map) {
            StringBuilder sb = new StringBuilder("{");
            boolean first = true;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (!first) {
                    sb.append(",");
                }
                first = false;
                sb.append("\"").append(escape(String.valueOf(entry.getKey()))).append("\":");
                sb.append(toJson(entry.getValue()));
            }
            sb.append("}");
            return sb.toString();
        }
        if (value instanceof List<?> list) {
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < list.size(); i++) {
                if (i > 0) {
                    sb.append(",");
                }
                sb.append(toJson(list.get(i)));
            }
            sb.append("]");
            return sb.toString();
        }
        return "\"" + escape(value.toString()) + "\"";
    }

    public static Map<String, String> parseObject(String json) {
        Map<String, String> result = new LinkedHashMap<>();
        if (json == null) {
            return result;
        }
        String trimmed = json.trim();
        if (trimmed.isEmpty() || !trimmed.startsWith("{") || !trimmed.endsWith("}")) {
            return result;
        }

        int i = 1;
        int len = trimmed.length() - 1;
        while (i < len) {
            i = skipWhitespaceAndCommas(trimmed, i, len);
            if (i >= len) {
                break;
            }
            if (trimmed.charAt(i) != '"') {
                break;
            }
            StringBuilder key = new StringBuilder();
            i = readString(trimmed, i + 1, len, key);
            i = skipWhitespace(trimmed, i, len);
            if (i >= len || trimmed.charAt(i) != ':') {
                break;
            }
            i++;
            i = skipWhitespace(trimmed, i, len);
            if (i >= len) {
                break;
            }

            if (trimmed.charAt(i) == '"') {
                StringBuilder val = new StringBuilder();
                i = readString(trimmed, i + 1, len, val);
                result.put(key.toString(), val.toString());
            } else {
                int start = i;
                while (i < len && trimmed.charAt(i) != ',' && trimmed.charAt(i) != '}') {
                    i++;
                }
                String raw = trimmed.substring(start, i).trim();
                if (!"null".equals(raw)) {
                    result.put(key.toString(), raw);
                }
            }
        }
        return result;
    }

    public static String getString(Map<String, String> map, String key, String defaultVal) {
        String val = map.get(key);
        return val == null ? defaultVal : val;
    }

    public static boolean getBoolean(Map<String, String> map, String key, boolean defaultVal) {
        String val = map.get(key);
        if (val == null || val.isBlank()) {
            return defaultVal;
        }
        if ("true".equalsIgnoreCase(val.trim()) || "1".equals(val.trim())) {
            return true;
        }
        if ("false".equalsIgnoreCase(val.trim()) || "0".equals(val.trim())) {
            return false;
        }
        return defaultVal;
    }

    public static int getInt(Map<String, String> map, String key, String fieldName) {
        String val = map.get(key);
        if (val == null || val.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required.");
        }
        try {
            return Integer.parseInt(val.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(fieldName + " must be a whole number.");
        }
    }

    public static int getOptionalInt(Map<String, String> map, String key, int defaultVal) {
        String val = map.get(key);
        if (val == null || val.isBlank()) {
            return defaultVal;
        }
        try {
            return Integer.parseInt(val.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(key + " must be a whole number.");
        }
    }

    public static double getDouble(Map<String, String> map, String key, String fieldName) {
        String val = map.get(key);
        if (val == null || val.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required.");
        }
        try {
            return Double.parseDouble(val.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(fieldName + " must be a valid number.");
        }
    }

    public static double getOptionalDouble(Map<String, String> map, String key, double defaultVal) {
        String val = map.get(key);
        if (val == null || val.isBlank()) {
            return defaultVal;
        }
        try {
            return Double.parseDouble(val.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(key + " must be a valid number.");
        }
    }

    private static int skipWhitespace(String s, int i, int len) {
        while (i < len && Character.isWhitespace(s.charAt(i))) {
            i++;
        }
        return i;
    }

    private static int skipWhitespaceAndCommas(String s, int i, int len) {
        while (i < len && (Character.isWhitespace(s.charAt(i)) || s.charAt(i) == ',')) {
            i++;
        }
        return i;
    }

    private static int readString(String s, int i, int len, StringBuilder out) {
        while (i < len) {
            char c = s.charAt(i++);
            if (c == '\\' && i < len) {
                char next = s.charAt(i++);
                switch (next) {
                    case 'n' -> out.append('\n');
                    case 'r' -> out.append('\r');
                    case 't' -> out.append('\t');
                    case '"' -> out.append('"');
                    case '\\' -> out.append('\\');
                    default -> out.append(next);
                }
            } else if (c == '"') {
                return i;
            } else {
                out.append(c);
            }
        }
        return i;
    }

    private static String escape(String s) {
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
