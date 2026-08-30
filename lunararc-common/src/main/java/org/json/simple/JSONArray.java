package org.json.simple;

import java.util.ArrayList;
import java.util.Collection;


public class JSONArray extends ArrayList<Object> {
    public JSONArray() {}
    public JSONArray(Collection<?> values) { super(values); }
    public String toJSONString() { return JSONValue.toJSONString(this); }
    @Override public String toString() { return toJSONString(); }
    public static String toJSONString(Collection<?> collection) { return JSONValue.toJSONString(collection); }
}
