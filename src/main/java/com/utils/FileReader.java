package com.utils;

import com.itextpdf.text.Element;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.Paragraph;
import com.utils.data.QueryHelper;
import io.cucumber.java.sl.In;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.utils.PdfHelper.normalizeHeader;

public class FileReader {
    public static final String MESSAGE = "message";
    public static final String ROW_NAME = "row_name";
    public static final String INFO_ROW = "info_row";
    public static final List<String> DATE_COLUMNS = new ArrayList<>() {{
        add("date");
        add("observer");
        add("to");
        add("from");
    }};
    public static JSONObject persistExcel(final String fileName, final int sheetId, final String userId) throws IOException {
        var array = new JSONArray();
        var rowModel = QueryHelper.getRowsModel(sheetId);

        if (rowModel.has(MESSAGE) && rowModel.getJSONArray(MESSAGE).length() > 0) {
            var model = rowModel.getJSONArray(MESSAGE);
            Workbook workbook = new XSSFWorkbook(fileName);
            Sheet sheet = workbook.getSheetAt(0);
            var columns = AmdsHelper.getColumns(Integer.toString(sheetId)).split(",");
            var stat = schemaOk(columns, sheet);
            if (!stat.isOk()) {
                throw new RuntimeException(stat.getMessage() + sheetId);
            }

            for (var i = 0; i < model.length(); i++) {

                if (i==137)
                    System.out.println();
                if (model.getJSONObject(i).getString(INFO_ROW).equals("0")) {
                    var rowName = model.getJSONObject(i).getString(ROW_NAME);

                    var row = sheet.getRow(i);
                    var j = 0;
                    var object = new JSONObject();
                    for (var col : columns) {

                        Cell cell = row.getCell(j);
                        var content = cell.toString();
                        if (DATE_COLUMNS.contains(col)) {
                            var format = DateHelper
                                                .formatOk(content);
                            if (!format.isOk()) {
                                throw new RuntimeException(format.getMessage());
                            }

                        }
                        object.put(col, content);

                         j++;
                    }

//                    for (var j = Integer.parseInt(stat.getMessage()); j < sheet.getLastRowNum(); j++) {
//                        if (sheet.getRow(j).getCell(0).toString().equals(rowName)) {
//                            var object = new JSONObject();
//                            var count = 0;
//                            for (var column : columns) {
//                                if (!column.equals("user_id")) {
//                                    if (column.contains("date") || column.contains("observed")) {
//                                        var con = sheet.getRow(j).getCell(count).toString();
//                                        var format = DateHelper
//                                                .formatOk(sheet.getRow(j).getCell(count).toString());
//                                        if (!format.isOk()) {
//                                            return new JSONObject() {{put(MESSAGE,
//                                                    "wrong date format. shoud be 'yyyy-MM-dd'");}};
//                                        } else {
//                                            object.put(column, format.getMessage());
//                                        }
//
//                                    } else {
//                                        object.put(column, sheet.getRow(j).getCell(count).toString());
//                                    }
//
//                                } else {
//                                    object.put(column, userId);
//                                }
//
//                                count++;
//                            }
//                            array.put(object);
//                        }
//
//                }
                    array.put(object);

                }

            }
            return new JSONObject() {{put(MESSAGE, array);}};
        }
        return new JSONObject() {{put("message", "unknown table");}};
    }


    private static UsefulBoolean schemaOk(final String[] columns, final Sheet sheet) {

        var error = "";
        var usefuleRow = 0;
        for (var row : sheet) {
            if (row.getCell(0).toString().equals(ROW_NAME)) {
                for (var column : columns) {
                    var col = 0;
                    var found = false;
                    while (row.cellIterator().hasNext()) {
                        if (row.getCell(col).toString().equals(column)) {
                            found = true;
                            break;
                        }
                        col++;
                    }
                    if (!found) {
                        error = "column ? is not found in the spreadsheet ";
                        return new UsefulBoolean(false, error);

                    }
                }
                usefuleRow++;
            }

        }
        return new UsefulBoolean(true, Integer.toString(usefuleRow));

    }

    private static String parseExcelTableName(final String name, final String id) {
        var regex = "files/uploads/(.*?)_" + id + ".xlsx$";
        return normalizeHeader(Helper.parseStringWithRegex(regex, name, 1), id);
    }

}
