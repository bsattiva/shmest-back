package com.utils.command;

import org.json.JSONArray;
import org.testng.Assert;

import java.util.ArrayList;
import java.util.List;

public class JsonHelper {

    public static List<String> getListFromJsonArray(final JSONArray array) {
        List<String> result = new ArrayList<>();
        for (var i = 0; i < array.length(); i++) {
            var obj = array.get(i);
                result.add(array.getString(i));
        }
        return result;
    }


}
