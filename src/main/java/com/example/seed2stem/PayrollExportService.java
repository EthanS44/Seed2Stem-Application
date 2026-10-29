package com.example.seed2stem;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.stream.Collectors;

/** Payroll workbook: a summary to key into payroll, the hours grid, and every shift behind it. */
@Service
public class PayrollExportService {

    public byte[] export(PayrollReport report) {
        try (Workbook wb = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Styles st = new Styles(wb);
            writeSummary(wb, st, report);
            writeGrid(wb, st, report);
            writeShifts(wb, st, report);

            wb.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Failed to build payroll workbook", e);
        }
    }

    private void writeSummary(Workbook wb, Styles st, PayrollReport report) {
        Sheet sheet = wb.createSheet("Summary");
        PayPeriod p = report.getPeriod();
        Row title = sheet.createRow(0);
        cell(title, 0, "Payroll · " + p.getType().getDisplayName() + " · " + p.getLabel(), st.title);
        Row range = sheet.createRow(1);
        cell(range, 0, "From " + p.getStart() + " to " + p.getEnd(), null);

        int r = 3;
        header(sheet.createRow(r++), st, "Employee", "Role", "Email", "Regular hours", "Overtime hours",
                "Total hours", "Shifts", "Days worked", "Shifts to check");
        for (PayrollReport.EmployeeHours e : report.getEmployees()) {
            Row row = sheet.createRow(r++);
            cell(row, 0, e.getUser().getName(), null);
            cell(row, 1, role(e.getUser()), null);
            cell(row, 2, e.getUser().getEmail(), null);
            num(row, 3, e.getRegularMinutes(), st.hours);
            num(row, 4, e.getOvertimeMinutes(), st.hours);
            num(row, 5, e.getTotalMinutes(), st.hours);
            row.createCell(6).setCellValue(e.getShifts().size());
            row.createCell(7).setCellValue(e.getDaysWorked());
            row.createCell(8).setCellValue(e.getFlaggedCount());
        }
        Row total = sheet.createRow(r);
        cell(total, 0, "Total", st.bold);
        num(total, 3, report.getRegularMinutes(), st.boldHours);
        num(total, 4, report.getOvertimeMinutes(), st.boldHours);
        num(total, 5, report.getTotalMinutes(), st.boldHours);
        total.createCell(6).setCellValue(report.getShiftCount());

        for (int c = 0; c < 9; c++) sheet.autoSizeColumn(c);
    }

    private void writeGrid(Workbook wb, Styles st, PayrollReport report) {
        Sheet sheet = wb.createSheet(report.getPeriod().getDays() <= PayrollService.DAILY_COLUMNS_UP_TO ? "By day" : "By week");
        Row head = sheet.createRow(0);
        cell(head, 0, "Employee", st.header);
        int c = 1;
        for (PayrollReport.Bucket b : report.getBuckets()) {
            cell(head, c++, b.getLabel() + " " + b.getSubLabel(), st.header);
        }
        cell(head, c, "Total", st.header);

        int r = 1;
        for (PayrollReport.EmployeeHours e : report.getEmployees()) {
            Row row = sheet.createRow(r++);
            cell(row, 0, e.getUser().getName(), null);
            for (int i = 0; i < report.getBuckets().size(); i++) num(row, i + 1, e.bucket(i), st.hours);
            num(row, report.getBuckets().size() + 1, e.getTotalMinutes(), st.boldHours);
        }
        Row total = sheet.createRow(r);
        cell(total, 0, "Team", st.bold);
        for (int i = 0; i < report.getBuckets().size(); i++) num(total, i + 1, report.bucketTotal(i), st.boldHours);
        num(total, report.getBuckets().size() + 1, report.getTotalMinutes(), st.boldHours);

        for (int i = 0; i <= report.getBuckets().size() + 1; i++) sheet.autoSizeColumn(i);
    }

    private void writeShifts(Workbook wb, Styles st, PayrollReport report) {
        Sheet sheet = wb.createSheet("Shifts");
        header(sheet.createRow(0), st, "Employee", "Date", "Clock in", "Clock out", "Hours",
                "Regular", "Overtime", "Flags", "Notes");
        int r = 1;
        for (PayrollReport.EmployeeHours e : report.getEmployees()) {
            for (PayrollReport.Shift s : e.getShifts()) {
                Row row = sheet.createRow(r++);
                cell(row, 0, e.getUser().getName(), null);
                Cell d = row.createCell(1);
                d.setCellValue(s.getDate());
                d.setCellStyle(st.date);
                Cell in = row.createCell(2);
                in.setCellValue(s.getClockIn());
                in.setCellStyle(st.dateTime);
                if (s.getClockOut() != null) {
                    Cell o = row.createCell(3);
                    o.setCellValue(s.getClockOut());
                    o.setCellStyle(st.dateTime);
                }
                num(row, 4, s.getMinutes(), st.hours);
                num(row, 5, s.getRegularMinutes(), st.hours);
                num(row, 6, s.getOvertimeMinutes(), st.hours);
                cell(row, 7, s.getFlags().stream().map(PayrollReport.Shift.Flag::getLabel)
                        .collect(Collectors.joining(", ")), null);
                cell(row, 8, s.getEntry().getNotes(), null);
            }
        }
        for (int c = 0; c < 9; c++) sheet.autoSizeColumn(c);
    }

    static String role(User u) {
        return switch (u.getAccountType()) {
            case TECHNICIAN -> "Technician";
            case MANAGER -> "Manager";
            case DEVELOPER -> "Developer";
            case ADMIN -> "Admin";
        };
    }

    private static void header(Row row, Styles st, String... labels) {
        for (int i = 0; i < labels.length; i++) cell(row, i, labels[i], st.header);
    }

    private static void cell(Row row, int c, String value, CellStyle style) {
        Cell cell = row.createCell(c);
        cell.setCellValue(value != null ? value : "");
        if (style != null) cell.setCellStyle(style);
    }

    /** Minutes written as decimal hours to 2 places. */
    private static void num(Row row, int c, long minutes, CellStyle style) {
        Cell cell = row.createCell(c);
        cell.setCellValue(Math.round(minutes / 60.0 * 100) / 100.0);
        cell.setCellStyle(style);
    }

    private static final class Styles {
        final CellStyle header, title, bold, hours, boldHours, date, dateTime;

        Styles(Workbook wb) {
            CreationHelper h = wb.getCreationHelper();
            Font boldFont = wb.createFont();
            boldFont.setBold(true);
            Font titleFont = wb.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 14);

            header = wb.createCellStyle();
            header.setFont(boldFont);
            header.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            header.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            title = wb.createCellStyle();
            title.setFont(titleFont);

            bold = wb.createCellStyle();
            bold.setFont(boldFont);

            hours = wb.createCellStyle();
            hours.setDataFormat(h.createDataFormat().getFormat("0.00"));

            boldHours = wb.createCellStyle();
            boldHours.setDataFormat(h.createDataFormat().getFormat("0.00"));
            boldHours.setFont(boldFont);

            date = wb.createCellStyle();
            date.setDataFormat(h.createDataFormat().getFormat("yyyy-mm-dd"));

            dateTime = wb.createCellStyle();
            dateTime.setDataFormat(h.createDataFormat().getFormat("yyyy-mm-dd hh:mm"));
        }
    }
}
