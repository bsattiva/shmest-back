package com.utils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class DateHelper {
    public static final String DATE_FORMAT = "yyyy-MM-dd";
    public static final String ALT_FORMAT = "dd-MMM-yyyy";

    public static UsefulBoolean formatOk(final String date) {
        if (!Helper.isThing(date))
            return new UsefulBoolean(true, "");
        try {
            var formatter = DateTimeFormatter.ofPattern(DATE_FORMAT);
            formatter.parse(date);
            return new UsefulBoolean(true, date);
        } catch (DateTimeParseException e) {
            return formatOk(date, ALT_FORMAT);
        }

    }

    public static UsefulBoolean formatOk(final String date, final String format) {

        try {
            var formatter = DateTimeFormatter.ofPattern(format);
            formatter.parse(date);
            var localDate = LocalDate.parse(date, formatter);
            return new UsefulBoolean(true,
                    localDate.format(DateTimeFormatter.ofPattern(DATE_FORMAT)));
        } catch (DateTimeParseException e) {
            return new UsefulBoolean(false, "");
        }

    }


}
