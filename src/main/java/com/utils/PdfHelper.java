package com.utils;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPCellEvent;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;

import com.utils.data.QueryHelper;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static com.utils.AmdsHelper.saveSheet;

public class PdfHelper {


    public static List<String> saveAllXls(final String userId) {
        final var tableIds = QueryHelper.getEnabledAmdsSheets(userId);
        List<String> paths = new ArrayList<>();
        for (var id : tableIds) {
            paths.add(saveSheet(id, userId));
        }
        return paths;
    }

    public static String capitalizeStart(final String value) {
        final var chars = value.toCharArray();
        var resultBuilder = new StringBuilder();
        var started = false;
        for (var ch : chars) {
            if (!started) {
                resultBuilder.append(Character.toUpperCase(ch));
            } else {
                resultBuilder.append(Character.toLowerCase(ch));

            }
            started = true;
        }
        return resultBuilder.toString();
    }

    public static String normalizeHeader(final String value, final String id) {
        final var trimmed = (value.trim().startsWith("_")) ? value.trim().substring(1) : value.trim();
        final var prepared = (trimmed.endsWith("_")) ? trimmed.substring(0, trimmed.length() - 1) : trimmed;
        var result = new StringBuilder();
        var parts = prepared.toLowerCase().split("_");
        Stream.of(parts).map(PdfHelper::capitalizeStart).forEach(part -> result.append(part).append(" "));
        return result.toString();
    }

    private static String parseTableName(final String name, final String id) {
        var regex = "files/(.*?)_" + id + ".xlsx$";
        return normalizeHeader(Helper.parseStringWithRegex(regex, name, 1), id);
    }

    private static float[] getColumnWidths(final int colNum, final float first, final float others) {
        var result = new float[colNum];
        var i  = 0;
        for (var item : result) {
            if (i == 0) {
                result[i] = first;
            } else {
                result[i] = others;
            }
            i++;
        }
        return result;
    }

    public static void cleanUp(final String id) {
        var maskXlsx = "_" + id + ".xlsx";
        var maskPdf = "_" + id + ".pdf";

        var path = TestHelper.getSameLevelProject("files");
        var dir = new File(path);
        var files = dir.listFiles();
        for (var file : files) {
            if (file.getName().contains(maskXlsx) || file.getName().contains(maskPdf)) {
                file.delete();
            }

        }

    }


    public static void saveUserPdf(final String userId, final String fileId) {
        cleanUp(userId);
            var name = QueryHelper.getNameById(userId);
        var files = saveAllXls(userId);
        var document = new Document(PageSize.A4.rotate());

        try {
            var path = TestHelper.getSameLevelProject("files") + "/output_" + fileId + "_" + userId + ".pdf";
            PdfWriter.getInstance(document, new FileOutputStream(path));
            document.open();
            Paragraph titleParagraph = new Paragraph("Self assessment form for: " + name);
            titleParagraph.setSpacingAfter(5);
            titleParagraph.setAlignment(Element.ALIGN_LEFT);
            document.add(titleParagraph);

            // Create the navbar-like stripe table
            PdfPTable stripeTable = new PdfPTable(1);
            stripeTable.setWidthPercentage(100);
            stripeTable.getDefaultCell().setPadding(0);
            stripeTable.getDefaultCell().setBorder(Rectangle.NO_BORDER);

            // Create the stripe cell
            PdfPCell stripeCell = new PdfPCell();
            stripeCell.setBackgroundColor(new BaseColor(0, 0, 128)); // Set the background color to dark navy
            stripeCell.setFixedHeight(90); // Set the height of the stripe
            stripeCell.setVerticalAlignment(Element.ALIGN_MIDDLE);

            // Create a nested table for the image and text
            PdfPTable nestedTable = new PdfPTable(2);
            nestedTable.setWidthPercentage(100);
            nestedTable.setSpacingBefore(10);
            nestedTable.setWidths(new float[]{4, 96}); // Adjust the widths as needed

            PdfPCell imageCell = new PdfPCell();
            imageCell.setBorder(Rectangle.NO_BORDER);
            imageCell.setPaddingTop(120);
           // imageCell.setPaddingLeft(50);

            String imagePath = "logo.png";
            com.itextpdf.text.Image image = com.itextpdf.text.Image.getInstance(imagePath);
             image.scaleToFit(220, 160);

           imageCell.addElement(image);

          //  nestedTable.addCell(image);
            stripeCell.addElement(image);

            stripeTable.addCell(stripeCell);
        //    document.add(image);
            document.add(stripeTable);
            //  document.add(imageCell);

            for (var file : files) {
                var header = parseTableName(file, userId);
                Workbook workbook = new XSSFWorkbook(file);

                com.itextpdf.text.Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14);
                Paragraph headingParagraph = new Paragraph(header, headerFont);
                headingParagraph.setAlignment(Element.ALIGN_CENTER);
                document.add(headingParagraph);
                Paragraph spaceParagraph = new Paragraph(10);
                document.add(spaceParagraph);
                document.add(spaceParagraph);
                document.add(spaceParagraph);
                document.add(new Paragraph("\n"));
                Sheet sheet = workbook.getSheetAt(0);
                var columnCount = sheet.getRow(0).getLastCellNum();


                PdfPTable table = new PdfPTable(sheet.getRow(0).getLastCellNum());
                table.setWidths(getColumnWidths(columnCount, 6f, 2f));

                CellStyle cellStyle = workbook.createCellStyle();
                cellStyle.setFillForegroundColor(IndexedColors.BLUE.getIndex());
                cellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

                var rows = 0;
                for (Row row : sheet) {
                    for (var i = 0; i < columnCount; i ++) {
                        Cell cell = row.getCell(i);
                        if (rows == 0) {
                            cell.setCellStyle(cellStyle);
                        }
                        if (cell != null) {
                            table.addCell(cell.toString());
                        } else {
                            table.addCell("");

                        }


                    }

                    rows++;
                }


                document.add(table);

                document.add(new Paragraph("\n"));

                workbook.close();
            }
            

        } catch (DocumentException | IOException e) {
            e.printStackTrace();
            //Logger logger = new Logger(PdfHelper.class);
           // logger.log(e.getMessage(), "ERROR IN PDF HELPER");
        }

        document.close();

    }


}
