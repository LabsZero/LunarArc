package org.json.simple;

import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.util.Iterator;
import java.util.Map;


public final class JSONValue {
    private JSONValue() {}

    public static Object parse(String s) {
        try { return new JSONParser().parse(s); } catch (Exception ignored) { return null; }
    }

    public static Object parse(java.io.Reader reader) {
        try { return new JSONParser().parse(reader); } catch (Exception ignored) { return null; }
    }

    public static Object parseWithException(String s) throws ParseException {
        return new JSONParser().parse(s);
    }

    public static Object parseWithException(java.io.Reader reader) throws java.io.IOException, ParseException {
        return new JSONParser().parse(reader);
    }

    public static String toJSONString(Object value) {
        if (value == null) return "null";
        if (value instanceof String s) return '"' + escape(s) + '"';
        if (value instanceof Number || value instanceof Boolean) return String.valueOf(value);
        if (value instanceof Map<?, ?> map) {
            StringBuilder out = new StringBuilder("{");
            Iterator<? extends Map.Entry<?, ?>> it = map.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<?, ?> e = it.next();
                out.append(toJSONString(String.valueOf(e.getKey()))).append(':').append(toJSONString(e.getValue()));
                if (it.hasNext()) out.append(',');
            }
            return out.append('}').toString();
        }
        if (value instanceof Iterable<?> iterable) {
            StringBuilder out = new StringBuilder("[");
            Iterator<?> it = iterable.iterator();
            while (it.hasNext()) {
                out.append(toJSONString(it.next()));
                if (it.hasNext()) out.append(',');
            }
            return out.append(']').toString();
        }
        return toJSONString(String.valueOf(value));
    }

    public static String escape(String s) {
        StringBuilder out = new StringBuilder(s.length() + 16);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\b' -> out.append("\\b");
                case '\f' -> out.append("\\f");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> {
                    if (c < 0x20) out.append(String.format("\\u%04x", (int) c));
                    else out.append(c);
                }
            }
        }
        return out.toString();
    }
}
