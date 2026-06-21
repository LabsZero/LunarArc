package org.json.simple;

import java.util.ArrayList;

/** Minimal stub so Vault's update-checker can load without crashing. */
public class JSONArray extends ArrayList<Object> {
    public String toJSONString() { return "[]"; }
}
