package org.json.simple;

import java.util.HashMap;
import java.util.Map;

/** Minimal stub so Vault's update-checker can load without crashing. */
public class JSONObject extends HashMap<Object, Object> {
    public JSONObject() {}
    public JSONObject(Map<?, ?> map) { super((Map<Object, Object>) map); }
    public String toJSONString() { return "{}"; }
}
