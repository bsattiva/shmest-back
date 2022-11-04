package com.utils;


import org.apache.commons.lang3.math.NumberUtils;
import org.apache.log4j.Logger;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class Helper {
    private static final String HOME = System.getProperty("user.dir");
    private final static Logger LOGGER = Logger.getLogger(Helper.class);
    private static final String SEP = File.separator;
    private static final Random RANDOM = new Random();
    public static String getStringFromProperties(String file,String key){
        Properties properties = new Properties();
        String result = "";
        try {
            properties.load(new FileInputStream(file));
            result = properties.getProperty(key);
        } catch (IOException e) {
            LOGGER.info(e.getMessage());
        }
        return result;
    }
    public static String getUrl(String key){
        return Helper.getStringFromProperties("config.properties",key);
    }
    public static JSONObject getFailedObject(){
        JSONObject result = new JSONObject();
        result.put("message","failure");
        return result;
    }


    public static String getHomeDir(String[] tree){
        StringBuilder builder = new StringBuilder(HOME);
        Arrays.asList(tree).forEach(folder -> builder.append(SEP + folder));
        return builder.toString();
    }

    public static String completeString(final String regex, final String body, final String[] arguments) {
        var parts = body.split(regex);
        var builder = new StringBuilder();
        var i = 0;
        for (var part : parts) {
            builder.append(part);
            if (arguments.length > i) {
                builder.append(arguments[i]);
            }
            i++;
        }

        return builder.toString();
    }


    public static String getRandomString(final int length) {
        final String raw = "abcdefghijklmnopqrstwuvxyzABCDEFGHIJKLMNOPQRSTWUVXYZ0123456789";
        var builder = new StringBuilder();
        IntStream.range(0, length).forEach(i -> {
            var num = RANDOM.nextInt(raw.length() -1);
            var nextChar = raw.substring(num, num + 1);
            builder.append(nextChar);
        });
        return builder.toString();
    }

    public static String getCurrentDir() {
        return System.getProperty("user.dir");
    }

    public static boolean isInteger(final String rawString) {
        return NumberUtils.isParsable(rawString);
    }

    public static boolean isThing(final String value) {
        return value != null && !value.isEmpty();
    }

}
