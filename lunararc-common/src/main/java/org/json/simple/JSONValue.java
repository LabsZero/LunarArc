package org.json.simple;

/** Minimal stub so Vault's update-checker can load without crashing. */
public class JSONValue {
    public static Object parse(String s) { return null; }
    public static Object parseWithException(String s) { return null; }
    public static String toJSONString(Object value) { return "null"; }
}
