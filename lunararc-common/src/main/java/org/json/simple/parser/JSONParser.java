package org.json.simple.parser;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.io.IOException;
import java.io.Reader;


public class JSONParser {
    private String input;
    private int pos;

    public Object parse(String s) throws ParseException {
        this.input = s == null ? "" : s;
        this.pos = 0;
        Object value = readValue();
        skipWs();
        if (pos != input.length()) throw error(ParseException.ERROR_UNEXPECTED_CHAR, input.charAt(pos));
        return value;
    }

    public Object parse(Reader reader) throws IOException, ParseException {
        StringBuilder sb = new StringBuilder();
        char[] buf = new char[1024];
        int read;
        while ((read = reader.read(buf)) != -1) sb.append(buf, 0, read);
        return parse(sb.toString());
    }

    private Object readValue() throws ParseException {
        skipWs();
        if (pos >= input.length()) throw error(ParseException.ERROR_UNEXPECTED_TOKEN, "EOF");
        char c = input.charAt(pos);
        return switch (c) {
            case '{' -> readObject();
            case '[' -> readArray();
            case '"' -> readString();
            case 't' -> { expect("true"); yield Boolean.TRUE; }
            case 'f' -> { expect("false"); yield Boolean.FALSE; }
            case 'n' -> { expect("null"); yield null; }
            default -> {
                if (c == '-' || Character.isDigit(c)) yield readNumber();
                throw error(ParseException.ERROR_UNEXPECTED_CHAR, c);
            }
        };
    }

    private JSONObject readObject() throws ParseException {
        JSONObject object = new JSONObject();
        pos++; skipWs();
        if (consume('}')) return object;
        do {
            skipWs();
            if (pos >= input.length() || input.charAt(pos) != '"') throw error(ParseException.ERROR_UNEXPECTED_TOKEN, "object key");
            String key = readString();
            skipWs(); expect(':');
            object.put(key, readValue());
            skipWs();
        } while (consume(','));
        expect('}');
        return object;
    }

    private JSONArray readArray() throws ParseException {
        JSONArray array = new JSONArray();
        pos++; skipWs();
        if (consume(']')) return array;
        do {
            array.add(readValue());
            skipWs();
        } while (consume(','));
        expect(']');
        return array;
    }

    private String readString() throws ParseException {
        expect('"');
        StringBuilder out = new StringBuilder();
        while (pos < input.length()) {
            char c = input.charAt(pos++);
            if (c == '"') return out.toString();
            if (c != '\\') { out.append(c); continue; }
            if (pos >= input.length()) throw error(ParseException.ERROR_UNEXPECTED_TOKEN, "escape");
            char e = input.charAt(pos++);
            switch (e) {
                case '"' -> out.append('"');
                case '\\' -> out.append('\\');
                case '/' -> out.append('/');
                case 'b' -> out.append('\b');
                case 'f' -> out.append('\f');
                case 'n' -> out.append('\n');
                case 'r' -> out.append('\r');
                case 't' -> out.append('\t');
                case 'u' -> {
                    if (pos + 4 > input.length()) throw error(ParseException.ERROR_UNEXPECTED_TOKEN, "unicode");
                    try { out.append((char) Integer.parseInt(input.substring(pos, pos + 4), 16)); }
                    catch (NumberFormatException ex) { throw error(ParseException.ERROR_UNEXPECTED_TOKEN, "unicode"); }
                    pos += 4;
                }
                default -> throw error(ParseException.ERROR_UNEXPECTED_CHAR, e);
            }
        }
        throw error(ParseException.ERROR_UNEXPECTED_TOKEN, "string");
    }

    private Number readNumber() throws ParseException {
        int start = pos;
        if (consume('-')) {}
        while (pos < input.length() && Character.isDigit(input.charAt(pos))) pos++;
        boolean floating = false;
        if (consume('.')) { floating = true; while (pos < input.length() && Character.isDigit(input.charAt(pos))) pos++; }
        if (pos < input.length() && (input.charAt(pos) == 'e' || input.charAt(pos) == 'E')) {
            floating = true; pos++; if (pos < input.length() && (input.charAt(pos) == '+' || input.charAt(pos) == '-')) pos++;
            while (pos < input.length() && Character.isDigit(input.charAt(pos))) pos++;
        }
        String raw = input.substring(start, pos);
        try {
            if (floating) return Double.valueOf(raw);
            long l = Long.parseLong(raw);
            return (l >= Integer.MIN_VALUE && l <= Integer.MAX_VALUE) ? Integer.valueOf((int) l) : Long.valueOf(l);
        } catch (NumberFormatException ex) {
            throw error(ParseException.ERROR_UNEXPECTED_TOKEN, raw);
        }
    }

    private void expect(String s) throws ParseException { for (int i = 0; i < s.length(); i++) expect(s.charAt(i)); }
    private void expect(char c) throws ParseException { if (!consume(c)) throw error(ParseException.ERROR_UNEXPECTED_CHAR, pos < input.length() ? input.charAt(pos) : "EOF"); }
    private boolean consume(char c) { if (pos < input.length() && input.charAt(pos) == c) { pos++; return true; } return false; }
    private void skipWs() { while (pos < input.length() && Character.isWhitespace(input.charAt(pos))) pos++; }
    private ParseException error(int type, Object unexpected) { return new ParseException(pos, type, unexpected); }
}
