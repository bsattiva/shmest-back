package com.utils;


import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class FileHelper {

    private static final Logger LOGGER = Logger.getLogger(FileHelper.class);
    private static final String MASK = "?";

    public static boolean saveTests(final List<String> tests, final String path) {
        var ok = false;
            File file = new File(path);
        try {
            FileUtils.writeLines(file, tests);
            ok = true;
        } catch (IOException e) {
            LOGGER.error(e.getMessage());
        }
        return ok;
    }

    public static File getResourceFile(final String fileName, final boolean test) {
        var dir = Helper.getCurrentDir();
        var path = Helper.getCurrentDir() + "/src/?/resources/" + fileName;
        var finalPath = (test) ? path.replace(MASK, "test") : path.replace(MASK, "main");
        return new File(finalPath);
    }




}
