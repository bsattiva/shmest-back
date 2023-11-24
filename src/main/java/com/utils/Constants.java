package com.utils;

import java.util.ArrayList;
import java.util.List;

public class Constants {
    public static final List<String> DATE_COLUMNS = new ArrayList<>() {{
        add("date");
        add("observed");
        add("to");
        add("from");
    }};
    public static final String DATE_FORMAT = "yyyy-MM-dd";
}
