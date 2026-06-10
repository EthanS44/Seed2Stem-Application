package com.example.seed2stem;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Builds a single .xlsx workbook with one sheet per concern: tasks, runs,
 * responses, pauses, and time entries. Intentionally excludes credentials and
 * emails — only the technician's display name is included so the data is
 * still traceable to a person without leaking account identifiers.
 */
@Service
public class ExcelExportService {

    private final TaskRepository taskRepo;
    private final ChecklistRunRepository runRepo;
    private final ChecklistResponseRepository responseRepo;
    private final TaskPauseRepository pauseRepo;
    private final TimeEntryRepository timeEntryRepo;

    public ExcelExportService(TaskRepository taskRepo,
                              ChecklistRunRepository runRepo,
                              ChecklistResponseRepository responseRepo,
                              TaskPauseRepository pauseRepo,
                              TimeEntryRepository timeEntryRepo) {
        this.taskRepo = taskRepo;
        this.runRepo = runRepo;
        this.responseRepo = responseRepo;
        this.pauseRepo = pauseRepo;
        this.timeEntryRepo = timeEntryRepo;
    }

    /**
     * Build the full export workbook and serialize to bytes.
     * Read-only transactional so lazy associations (Task on Run, etc.) can be
     * traversed without LazyInitializationException.
     */
    @Transactional(readOnly = true)
    public byte[] buildWorkbook() {
        try (Workbook wb = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            CellStyle headerStyle = headerStyle(wb);
            CellStyle dateTimeStyle = dateTimeStyle(wb);

            writeTasksSheet(wb, headerStyle);
            writeRunsSheet(wb, headerStyle, dateTimeStyle);
            writeResponsesSheet(wb, headerStyle);
            writePausesSheet(wb, headerStyle, dateTimeStyle);
            writeTimeEntriesSheet(wb, headerStyle, dateTimeStyle);

            wb.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Failed to build export workbook", e);
        }
    }

    /* ---------------- Tasks ---------------- */

    private void writeTasksSheet(Workbook wb, CellStyle headerStyle) {
        Sheet sheet = wb.createSheet("Tasks");
        String[] headers = {"Task ID", "Title", "Description", "Checklist Name",
                "Item Count", "User-Created", "Created By"};
        writeHeader(sheet, headers, headerStyle);

        int rowIdx = 1;
        for (Task t : taskRepo.findAll()) {
            Row row = sheet.createRow(rowIdx++);
            int c = 0;
            row.createCell(c++).setCellValue(t.getId());
            row.createCell(c++).setCellValue(nullSafe(t.getTitle()));
            row.createCell(c++).setCellValue(nullSafe(t.getDescription()));
            row.createCell(c++).setCellValue(t.getChecklist() != null
                    ? nullSafe(t.getChecklist().getName()) : "");
            int itemCount = (t.getChecklist() != null && t.getChecklist().getItems() != null)
                    ? t.getChecklist().getItems().size() : 0;
            row.createCell(c++).setCellValue(itemCount);
            row.createCell(c++).setCellValue(t.isUserCreated() ? "Yes" : "No");
            row.createCell(c++).setCellValue(t.getCreatedBy() != null
                    ? t.getCreatedBy().getName() : "");
        }
        autoSize(sheet, headers.length);
    }

    /* ---------------- Checklist Runs ---------------- */

    private void writeRunsSheet(Workbook wb, CellStyle headerStyle, CellStyle dateTimeStyle) {
        Sheet sheet = wb.createSheet("Checklist Runs");
        String[] headers = {"Run ID", "Task Title", "Technician", "Status",
                "Start Time", "End Time", "Duration (min)",
                "Authorized By", "Authorized At", "Manager Comments"};
        writeHeader(sheet, headers, headerStyle);

        int rowIdx = 1;
        for (ChecklistRun r : runRepo.findAll()) {
            Row row = sheet.createRow(rowIdx++);
            int c = 0;
            row.createCell(c++).setCellValue(r.getId());
            row.createCell(c++).setCellValue(r.getTask() != null
                    ? nullSafe(r.getTask().getTitle()) : nullSafe(r.getChecklistName()));
            row.createCell(c++).setCellValue(r.getCompletedBy() != null
                    ? r.getCompletedBy().getName() : "");
            row.createCell(c++).setCellValue(r.getStatus() != null
                    ? r.getStatus().name() : "");
            writeDateTime(row.createCell(c++), r.getStartTime(), dateTimeStyle);
            writeDateTime(row.createCell(c++), r.getEndTime(), dateTimeStyle);
            row.createCell(c++).setCellValue(durationMinutes(r.getStartTime(), r.getEndTime()));
            row.createCell(c++).setCellValue(r.getAuthorizedBy() != null
                    ? r.getAuthorizedBy().getName() : "");
            writeDateTime(row.createCell(c++), r.getAuthorizedAt(), dateTimeStyle);
            row.createCell(c++).setCellValue(nullSafe(r.getManagerComments()));
        }
        autoSize(sheet, headers.length);
    }

    /* ---------------- Checklist Responses ---------------- */

    private void writeResponsesSheet(Workbook wb, CellStyle headerStyle) {
        Sheet sheet = wb.createSheet("Checklist Responses");
        String[] headers = {"Run ID", "Task Title", "Technician", "Question",
                "Response Type", "Boolean Answer", "Text Answer", "Numeric Answer"};
        writeHeader(sheet, headers, headerStyle);

        int rowIdx = 1;
        for (ChecklistResponse resp : responseRepo.findAll()) {
            Row row = sheet.createRow(rowIdx++);
            int c = 0;
            ChecklistRun run = resp.getChecklistRun();
            ChecklistItem item = resp.getChecklistItem();

            row.createCell(c++).setCellValue(run != null ? String.valueOf(run.getId()) : "");
            row.createCell(c++).setCellValue(run != null && run.getTask() != null
                    ? nullSafe(run.getTask().getTitle()) : "");
            row.createCell(c++).setCellValue(run != null && run.getCompletedBy() != null
                    ? run.getCompletedBy().getName() : "");
            row.createCell(c++).setCellValue(item != null ? nullSafe(item.getText()) : "");
            row.createCell(c++).setCellValue(item != null && item.getResponseType() != null
                    ? item.getResponseType().name() : "");
            row.createCell(c++).setCellValue(resp.getBooleanAnswer() != null
                    ? resp.getBooleanAnswer().toString() : "");
            row.createCell(c++).setCellValue(nullSafe(resp.getTextAnswer()));
            if (resp.getNumericAnswer() != null) {
                row.createCell(c++).setCellValue(resp.getNumericAnswer());
            } else {
                row.createCell(c++).setCellValue("");
            }
        }
        autoSize(sheet, headers.length);
    }

    /* ---------------- Task Pauses ---------------- */

    private void writePausesSheet(Workbook wb, CellStyle headerStyle, CellStyle dateTimeStyle) {
        Sheet sheet = wb.createSheet("Task Pauses");
        String[] headers = {"Pause ID", "Run ID", "Task Title", "Technician",
                "Pause Start", "Pause End", "Duration (min)", "Reason"};
        writeHeader(sheet, headers, headerStyle);

        int rowIdx = 1;
        for (TaskPause p : pauseRepo.findAll()) {
            Row row = sheet.createRow(rowIdx++);
            int c = 0;
            ChecklistRun run = p.getChecklistRun();

            row.createCell(c++).setCellValue(p.getId());
            row.createCell(c++).setCellValue(run != null ? String.valueOf(run.getId()) : "");
            row.createCell(c++).setCellValue(run != null && run.getTask() != null
                    ? nullSafe(run.getTask().getTitle()) : "");
            row.createCell(c++).setCellValue(run != null && run.getCompletedBy() != null
                    ? run.getCompletedBy().getName() : "");
            writeDateTime(row.createCell(c++), p.getStartTime(), dateTimeStyle);
            writeDateTime(row.createCell(c++), p.getEndTime(), dateTimeStyle);
            row.createCell(c++).setCellValue(durationMinutes(p.getStartTime(), p.getEndTime()));
            row.createCell(c++).setCellValue(nullSafe(p.getReason()));
        }
        autoSize(sheet, headers.length);
    }

    /* ---------------- Time Entries ---------------- */

    private void writeTimeEntriesSheet(Workbook wb, CellStyle headerStyle, CellStyle dateTimeStyle) {
        Sheet sheet = wb.createSheet("Time Entries");
        String[] headers = {"Entry ID", "Technician", "Date", "Clock In",
                "Clock Out", "Total Hours", "Notes"};
        writeHeader(sheet, headers, headerStyle);

        int rowIdx = 1;
        for (TimeEntry e : timeEntryRepo.findAll()) {
            Row row = sheet.createRow(rowIdx++);
            int c = 0;
            row.createCell(c++).setCellValue(e.getId());
            row.createCell(c++).setCellValue(e.getUser() != null ? e.getUser().getName() : "");
            row.createCell(c++).setCellValue(e.getDate() != null ? e.getDate().toString() : "");
            writeDateTime(row.createCell(c++), e.getClockInTime(), dateTimeStyle);
            writeDateTime(row.createCell(c++), e.getClockOutTime(), dateTimeStyle);
            if (e.getTotalHours() != null) {
                row.createCell(c++).setCellValue(e.getTotalHours());
            } else {
                row.createCell(c++).setCellValue("");
            }
            row.createCell(c++).setCellValue(nullSafe(e.getNotes()));
        }
        autoSize(sheet, headers.length);
    }

    /* ---------------- helpers ---------------- */

    private static String nullSafe(String s) {
        return s == null ? "" : s;
    }

    private static long durationMinutes(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) return 0;
        return ChronoUnit.MINUTES.between(start, end);
    }

    private static void writeHeader(Sheet sheet, String[] headers, CellStyle style) {
        Row row = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = row.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(style);
        }
    }

    private static void writeDateTime(Cell cell, LocalDateTime ts, CellStyle style) {
        if (ts != null) {
            cell.setCellValue(ts);
            cell.setCellStyle(style);
        }
    }

    private static void autoSize(Sheet sheet, int columnCount) {
        for (int i = 0; i < columnCount; i++) {
            try {
                sheet.autoSizeColumn(i);
            } catch (Throwable ignored) {
                // autoSizeColumn calls into AWT's font system to measure text.
                // On stripped-down containers (Railway, many Docker base
                // images) there's no fontconfig / X11, so the font manager
                // fails to even load -> NoClassDefFoundError. That's an
                // Error not an Exception, so catch Throwable here. We can
                // live with default column widths in this case.
            }
        }
    }

    private static CellStyle headerStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }

    private static CellStyle dateTimeStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        CreationHelper helper = wb.getCreationHelper();
        style.setDataFormat(helper.createDataFormat().getFormat("yyyy-mm-dd hh:mm"));
        return style;
    }

    /** Visible-for-test accessors. */
    List<Task> tasksForTest() { return taskRepo.findAll(); }
    List<ChecklistRun> runsForTest() { return runRepo.findAll(); }
}
