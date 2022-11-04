package com.utils.command;

import java.io.File;
import java.util.Locale;

public class CommandHelper {
    private static String[] command;
    public static String[] getCommand(final String path) {

        if (System.getProperty("os.name").toLowerCase(Locale.ENGLISH).contains("windows")) {
            command = new String[]{"powershell", path + "/bin/start.bat -c "};
        } else {

        }
        return command;
    }
}
