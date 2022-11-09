package com.utils;

import org.apache.commons.io.IOUtils;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

public class RequestHelper {



    public static JSONObject getRequestBody(final HttpServletRequest request) {
        JSONObject obj = new JSONObject();

        if ("POST".equalsIgnoreCase(request.getMethod()))
        {
            String test = "";
            try {
                test = IOUtils.toString(request.getReader());;
                obj = new JSONObject(test.replace("\r", "").replace("\n", ""));



            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return obj;
    }
}
