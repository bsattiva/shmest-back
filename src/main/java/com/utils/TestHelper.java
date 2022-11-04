package com.utils;

import com.utils.command.JsonHelper;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


public class TestHelper {
    private static final String TESTS = "children";
    private static final String ARGUMENTS = "arguments";
    private static final String STEP = "step";
    private static final String MASK = "?";
    private static final String TAG = "tag";
    private static final String TEST_MASK = "<?>";
    private static final String INDENT = "  ";
    private static String indent = "";
    private static final Logger LOGGER = Logger.getLogger(TestHelper.class);

    private static String getStep (final String step, final JSONArray arguments) {
        var i = 1;
        var completeStep = step;
        for (var arg : JsonHelper.getListFromJsonArray(arguments)) {
            var mask = TEST_MASK.replace(MASK, Integer.toString(i));
            var argument = (!Helper.isInteger(arg))
                    ? "\"" + arg + "\"" : arg;
            completeStep = completeStep.replace(mask, argument);
            i++;
        }
        return completeStep;
    }


    public static String getSameLevelProject(final String name) {

        var parts = Helper.getCurrentDir().replace("\\", "/").split("/");
        return Arrays.stream(parts)
                .filter(part -> !Helper.getCurrentDir().endsWith(part)).collect(Collectors.joining("/"))
                +
                "/"
                + name;
    }


    public static List<String> getTests(final JSONObject tests) {
        indent = indent + INDENT;
        var testArray = tests.getJSONArray(TESTS);
        var arguments = tests.getJSONArray(ARGUMENTS);
        var step = tests.getString(STEP);
        var tag = tests.getString(TAG);
        List<String> result = new ArrayList<>();
        result.add(tag);
        result.add(indent + getStep(step, arguments));
        for (var i = 0; i < testArray.length(); i++) {
            var testList = getTests(testArray.getJSONObject(i), INDENT);
            result.addAll(testList);
        }
        return result;
    }

    public static List<String> getTests(final JSONObject tests, final String indent) {
        var indentValue = indent + INDENT;
        var testArray = tests.getJSONArray(TESTS);
        var arguments = tests.getJSONArray(ARGUMENTS);
        var step = tests.getString(STEP);
        List<String> result = new ArrayList<>();
        result.add(indentValue + getStep(step, arguments));
        for (var i = 0; i < testArray.length(); i++) {
            var testList = getTests(testArray.getJSONObject(i), indentValue);
            result.addAll(testList);
        }
        return result;
    }

    public static boolean saveTest(final JSONObject object, final String path) {
        var ok = false;
        try {
            FileUtils.writeLines(new File(path), getTests(object));
            ok = true;
        } catch (IOException e) {
            LOGGER.error(e.getMessage());
        }
        return ok;
    }

    public static JSONObject getStatus(final String status) {
        var object = new JSONObject();
        object.put("status", status);
        return object;
    }

    public static boolean savePages(final JSONObject object, final String path) {
        var ok = false;
        try {
            FileUtils.write(new File(path), object.toString(5), StandardCharsets.UTF_8);
            ok = true;
        } catch (IOException e) {
            LOGGER.error(e.getMessage());
        }
        return ok;
    }


    public static boolean saveConfig(final List<String> map, final String path) {
        var ok = false;
        try {
            FileUtils.writeLines(new File(path), map, false);
            ok = true;
        } catch (IOException e) {
            LOGGER.error(e.getMessage());
        }
        return ok;
    }


}
