package org.json.simple;

import java.util.HashMap;
import java.util.Map;


public class JSONObject extends HashMap<Object, Object> {
    public JSONObject() {}
    @SuppressWarnings("unchecked")
    public JSONObject(Map<?, ?> map) { super((Map<Object, Object>) map); }
    public String toJSONString() { return JSONValue.toJSONString(this); }
    @Override public String toString() { return toJSONString(); }
    public static String toJSONString(Map<?, ?> map) { return JSONValue.toJSONString(map); }
    public static String escape(String s) { return JSONValue.escape(s); }
}
