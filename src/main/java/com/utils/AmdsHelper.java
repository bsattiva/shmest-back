package com.utils;

import com.utils.data.HttpClient;
import com.utils.data.Logger;
import com.utils.data.QueryHelper;
import com.utils.enums.JsonHelper;
import org.apache.commons.io.FileUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class AmdsHelper {
    public static final String PULL_LIST = "pull-list";
    public static final String PULL_STRING = "pull-string";
    public static final String PULL_TABLE = "pull-table";
    public static final String MESSAGE = "message";
    public static final String EXTENSION = ".xlsx";
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
    public static final String QUERY_TEMPLATE_ALL = "select ? from amds.?";
    public static final String SHEET_QUERY_TEMPLATE = "select ? from amds.? where id='?' user_id='?'";
    public static final String CREATE_QUERY_TEMPLATE = "insert into amds.? (?,user_id) values(<q>)";
    public static final String QUESTION_MASK = "\\?";

    public static String getSheetQuery(final String sheetId, final String userId) {
        var columns = getColumns(sheetId) + ",user_id";
        var table = getTableName(sheetId);
        return QUERY_TEMPLATE.replaceFirst(QUESTION_MASK, columns)
                .replaceFirst(QUESTION_MASK, table)
                .replaceFirst(QUESTION_MASK, userId);
    }

    public static String getUserIdByEmail(final String email) throws Exception {
        var url = Helper.getUrl("user.url") + "/id-by-email?email=" + email;
        var secret = "7js dj^ skdjsd  ds8888dssd!!!";
        Map<String,String> map = new HashMap<>();
        map.put("secret", secret);
        return HttpClient.sendGet(url, map).getString("message");
    }

    private static List<String> getListFromString(final String str) {
        return List.of(str.split(","));
    }

    private static String deductList(final String shortStr, final String longStr) {
        var shortList = getListFromString(shortStr);
        var longList = getListFromString(longStr);

        var builder = new StringBuilder();

        longList.stream().filter(col -> !shortList.contains(col)).forEach(col -> builder.append(col).append(","));
        return builder.toString().substring(0, builder.toString().length() - 1);
    }


    public static String getAdminColumns(final String sheetId) {
        final var query = "select user from amds.sheets where id=" + sheetId;
        var cols = QueryHelper.getData(query, PULL_STRING);
        var colString = (cols.has(MESSAGE)) ? cols.getString(MESSAGE) : "";
        return deductList(colString, getColumns(sheetId)).replace("row_name,", "");
    }

    public static boolean adminColumnsIntact(final JSONArray oldTable,
                                             final JSONArray newTable, final String adminCols) {
        TestHelper.sleep(10);
        for (var i = 0; i < newTable.length(); i++) {
            for (var key : newTable.getJSONObject(i).keySet()) {
                if (oldTable.getJSONObject(i).has(key)) {
                    if (!oldTable.getJSONObject(i).getString(key).equals(newTable.getJSONObject(i).getString(key))) {
                        return false;
                    }
                } else if (List.of(adminCols.split(",")).contains(key)
                        && Helper.isThing(newTable.getJSONObject(i).getString(key))) {
                    return false;
                }
            }
        }
        return true;
    }


    public static String getSheetQuery(final String sheetId) {
        var columns = getColumns(sheetId) + ",user_id";
        var table = getTableName(sheetId);
        return QUERY_TEMPLATE_ALL.replaceFirst(QUESTION_MASK, columns)
                .replaceFirst(QUESTION_MASK, table);
    }

    public static String getAmdsFilesPath(final String name) {
        return TestHelper.getSameLevelProject("files") + "/" + name + ".xlsx";
    }

    public static String getAmdsFilesPathByName(final String name, final String userId) {
        return TestHelper.getSameLevelProject("files") + "/"
                + name
                + "_"
                + userId
                + EXTENSION;
    }

    public static List<String[]> getUsers() {
        List<String[]> result = new ArrayList<>();
        var path = Helper.getCurrentDir() + "\\src\\main\\resources\\users.csv";

        var file = new File(path);
        try {
            var lines = FileUtils.readLines(file, StandardCharsets.UTF_8);
            var j = 0;
            for (var line:lines) {
                if (j != 0) {
                    var first = "";
                    var last = "";

                    var email = "";
                    var i = 0;
                    var columns = line.split(",");
                    for (var column:columns) {
                        if (i == 0) {
                            first = first + column + " ";
                        } else if (i == 1) {
                            first = first + column;
                        } else if (i == 2) {
                            last = column;
                        } else {
                            email = column;
                        }
                        i++;
                    }

                    var arr = new String[] {first, last, email};
                    result.add(arr);

                }

                j++;
            }


        } catch (IOException e) {
            System.out.println(e);
        }

        return result;
    }

    public static List<String> getAllQueries(final String userId) {
        List<String> queries = new ArrayList<>();
        var query = "";
        return queries;
    }



    public static String saveSheet(final String id, final String userId) {
        var query = getSheetQuery(id, userId);
        if (id.equals("21")) {
            System.out.println("");

        }
        var data = QueryHelper.getData(query, PULL_TABLE);
        return saveSheetByUser(id, data, userId);
    }


    public static String saveSheetByUser(final String id, final JSONObject data, final String userId) {
        final var name = getTableName(id);
        if (name.toLowerCase().contains("skills")) {
            System.out.println(name);

        }
        var userName = (Helper.isThing(userId)) ? QueryHelper.getNameById(userId) : null;
        List<String> columns = new ArrayList<>(List.of(getColumns(id).split(",")));
        columns.add("user_id");
        var dataArray = data.getJSONArray("message");
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet(name);

        Row header = sheet.createRow(0);
        var ci = 0;
        for (var col: columns) {
            Cell head = header.createCell(ci);

            head.setCellValue(col);

            ci++;
        }

//        Cell uHead = header.createCell(ci + 1);
//        uHead.setCellValue("user-id");


        for (var i = 0; i < dataArray.length(); i++) {
            Row row = sheet.createRow(i + 1);
            var object = dataArray.getJSONObject(i);
            var count = 0;
            for (var column:columns) {
                Cell cell = row.createCell(count);
                var value = (object.has(column)) ? object.getString(column) : "";
                if (value.equals("on")) {
                    value = " ";
                } else if (value.equals("y")) {
                    value = "yes";

                }
                if (count == (columns.size() - 1)) {
                    value = QueryHelper.getNameById(value);
                }
                cell.setCellValue(value);
                count++;
            }

        }
        var path = getAmdsFilesPathByName(name, userId);
        try (FileOutputStream outputStream = new FileOutputStream(path)) {
            workbook.write(outputStream);
            System.out.println("Excel file created successfully.");
        } catch (IOException e) {
            e.printStackTrace();
        }

        return path;
    }

    public static void saveSheet(final String id, final JSONObject data, final String userId) {
        final var name = getTableName(id);
        var userName = (Helper.isThing(userId)) ? QueryHelper.getNameById(userId) : null;
        List<String> columns = new ArrayList<>(List.of(getColumns(id).split(",")));
        columns.add("user_id");
        var dataArray = data.getJSONArray("message");
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet(name);

        Row header = sheet.createRow(0);
        var ci = 0;
        for (var col: columns) {
            Cell head = header.createCell(ci);

            head.setCellValue(col);

            ci++;
        }

        Cell uHead = header.createCell(ci + 1);
        uHead.setCellValue("user-id");


        for (var i = 0; i < dataArray.length(); i++) {
            Row row = sheet.createRow(i + 1);
            var object = dataArray.getJSONObject(i);
            var count = 0;
            for (var column:columns) {
                Cell cell = row.createCell(count);
                var value = (object.has(column)) ? object.getString(column) : "";
                if (count == (columns.size() - 1)) {
                    value = QueryHelper.getNameById(value);
                }
                cell.setCellValue(value);
                count++;
            }
//            Cell cell = row.createCell(count + 1);
//            cell.setCellValue(userId);
        }

        try (FileOutputStream outputStream = new FileOutputStream(getAmdsFilesPath(name))) {
            workbook.write(outputStream);
            System.out.println("Excel file created successfully.");
        } catch (IOException e) {
            e.printStackTrace();
        }


    }


    private static String maskColumns(final String columns) {
        var parts = columns.split(",");
        var builder = new StringBuilder();
        List.of(parts).forEach(str -> builder.append("'?'").append(","));
        return builder.toString() + "'?'";
    }

    public static String createSheetPopulateQuery(final String sheetId,
                                                  final String userId,
                                                  final JSONArray data) {
        var sheetName = getTableName(sheetId);
        var columns = getColumns(sheetId);
        var cols = List.of(columns.split(","));
        var name = userId;

        var query = "insert into amds." + sheetName + "(" + columns + ",user_id) values ";
        for (var i = 0; i < data.length(); i++) {
            var row = "(";
            var j = 0;

            for (var column : cols) {
                    var value =  data.getJSONObject(i).getString(column).replace('\uF06E', '\0')
                        .replace("'", "''");
                    row = row + "'" + value + "',";

                j++;
            }
            row = row + "'" + name + "')";
            if (i < data.length() - 1) {
                row = row + ",";

            }
            query = query + row;
        }
        return query;
    }
    private static JSONObject getTestObject(List<String> cols, String num) {
        var object = new JSONObject();
        for (var col : cols) {
            object.put(col, col + " " + num);
        }
        return object;
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
