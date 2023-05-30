package com.utils.enums;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class JsonHelper {

    public static List<String> getListFromJsonArray(final JSONArray array) {
        List<String> list = new ArrayList<>();
        array.forEach(str -> list.add((String) str));
        return list;
    }

    public static List<Object> getListObjectFromJsonArray(final JSONArray array) {
        List<Object> list = new ArrayList<>();
        array.forEach(list::add);
        return list;
    }


    public static JSONArray getArrayFromList(final List<String> list) {
        var array = new JSONArray();
        list.forEach(array::put);
        return array;
    }
}
