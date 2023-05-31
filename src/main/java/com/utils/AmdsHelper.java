package com.utils;

import com.utils.data.QueryHelper;
import com.utils.enums.JsonHelper;
import org.json.JSONObject;

import java.util.List;

public class AmdsHelper {
    public static final String PULL_LIST = "pull-list";
    public static final String PULL_STRING = "pull-string";
    public static final String MESSAGE = "message";
    public static String getTableName(final String tableId) {
        final String query = "select name from amds.sheets where id='" + tableId + "'";
        return normalizeTable(QueryHelper.getData(query, PULL_STRING).getString(MESSAGE));
    }

    public static String getColumns(final String tableId) {
        final String query = "select properties from amds.sheets where id='" + tableId + "'";
        return "row_name," + QueryHelper.getData(query, PULL_STRING).getString(MESSAGE);
    }

    private static String normalizeTable(final String name) {
        return name.toLowerCase().replace(" - ", "_")
                .replace(" ", "_")
                .replace("(", "_")
                .replace(")", "_");

    }

    public static final String QUERY_TEMPLATE = "select ? from amds.? where user_id='?'";
    public static final String SHEET_QUERY_TEMPLATE = "select ? from amds.? where id='?' user_id='?'";
    public static final String CREATE_QUERY_TEMPLATE = "insert into amds.? (?,user_id) values(<q>)";
    public static final String QUESTION_MASK = "\\?";

    public static String getSheetQuery(final String sheetId, final String userId) {
        var columns = getColumns(sheetId);
        var table = getTableName(sheetId);
        return QUERY_TEMPLATE.replaceFirst(QUESTION_MASK, columns)
                .replaceFirst(QUESTION_MASK, table)
                .replaceFirst(QUESTION_MASK, userId);
    }


    private static String maskColumns(final String columns) {
        var parts = columns.split(",");
        var builder = new StringBuilder();
        List.of(parts).forEach(str -> builder.append("'?'").append(","));
        return builder.toString() + "'?'";
    }

    public static String getCreateSheetQuery(final String sheetId, final String userId, final JSONObject row) {
        var columns = getColumns(sheetId);
        var table = getTableName(sheetId);
        var query = CREATE_QUERY_TEMPLATE.replace("<q>", maskColumns(columns))
                .replaceFirst(QUESTION_MASK, table)
                .replaceFirst(QUESTION_MASK, columns);
        for (var col : columns.split(",")) {
            query = query.replaceFirst(QUESTION_MASK, row.getString(col));
        }
        query = query.replaceFirst(QUESTION_MASK, userId);
        return query;
    }

    public static String enrichColumns(final String columns) {
        var builder = new StringBuilder();
       for (var column : columns.split(",")) {
           if (!column.contains("comment") && !column.contains("detail")) {
               builder.append(normalizeColumn(column) + " varchar(100)").append(",");
           } else {
               builder.append(normalizeColumn(column) + " varchar(1000)").append(",");
           }
       }
        return builder.toString() + "user_id varchar(45)";
    }

    public static String normalizeColumn(final String column) {
        var col = column.trim();
        return col.replace(" ", "_");
    }
    public static void createSheetsTables() {
        final String namesQuery = "select name from amds.sheets";
        final String colsQuery = "select properties from amds.sheets";
        var names = JsonHelper
                .getListFromJsonArray(QueryHelper.getData(namesQuery, PULL_LIST).getJSONArray(MESSAGE));
        var columnList = JsonHelper
                .getListFromJsonArray(QueryHelper.getData(colsQuery, PULL_LIST).getJSONArray(MESSAGE));;

        for (var i = 0; i < names.size(); i++) {
            var name = names.get(i);
            var columns = columnList.get(i);

            if (!name.equals("MRI Clinical Skills")) {
                var query = "create table amds." + normalizeTable(name) + "(row_name varchar(300)," + enrichColumns(columns) + ")";
                var status = QueryHelper.getData(query, "execute");
                if (status.getString(MESSAGE).contains("SQL syntax")) {
                    System.out.println(name + "::::::::::" + status);

                }

            }

        }

    }
}
