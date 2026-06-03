package com.example.seed2stem;

import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExcelExportServiceTest {

    @Mock private TaskRepository taskRepo;
    @Mock private ChecklistRunRepository runRepo;
    @Mock private ChecklistResponseRepository responseRepo;
    @Mock private TaskPauseRepository pauseRepo;
    @Mock private TimeEntryRepository timeEntryRepo;

    @InjectMocks
    private ExcelExportService exportService;

    private User tech;
    private Task task;
    private Checklist checklist;
    private ChecklistItem question;

    @BeforeEach
    void setUp() {
        tech = new User("tech@s2s.com", "hashed", "Tech", "User", AccountType.TECHNICIAN);
        tech.setId(1L);

        checklist = new Checklist();
        checklist.setId(10L);
        checklist.setName("AM Inspection");
        question = new ChecklistItem();
        question.setId(100L);
        question.setText("Is temp OK?");
        question.setItemType(ChecklistItemType.QUESTION);
        question.setResponseType(ChecklistResponseType.BOOLEAN_TEXT);
        checklist.setItems(List.of(question));

        task = new Task();
        task.setId(50L);
        task.setTitle("Morning Inspection");
        task.setDescription("Run every morning");
        task.setChecklist(checklist);
        task.setCreatedBy(tech);
    }

    @Test
    void buildWorkbook_emptyData_producesFiveSheetsWithHeaderRowOnly() throws Exception {
        when(taskRepo.findAll()).thenReturn(List.of());
        when(runRepo.findAll()).thenReturn(List.of());
        when(responseRepo.findAll()).thenReturn(List.of());
        when(pauseRepo.findAll()).thenReturn(List.of());
        when(timeEntryRepo.findAll()).thenReturn(List.of());

        byte[] bytes = exportService.buildWorkbook();
        assertNotNull(bytes);
        assertTrue(bytes.length > 0);

        try (Workbook wb = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            assertEquals(5, wb.getNumberOfSheets());
            assertNotNull(wb.getSheet("Tasks"));
            assertNotNull(wb.getSheet("Checklist Runs"));
            assertNotNull(wb.getSheet("Checklist Responses"));
            assertNotNull(wb.getSheet("Task Pauses"));
            assertNotNull(wb.getSheet("Time Entries"));

            for (int i = 0; i < wb.getNumberOfSheets(); i++) {
                Sheet sheet = wb.getSheetAt(i);
                assertEquals(0, sheet.getLastRowNum(), "expected only header row on " + sheet.getSheetName());
                assertNotNull(sheet.getRow(0));
            }
        }
    }

    @Test
    void buildWorkbook_populatedData_writesRowsToCorrectSheets() throws Exception {
        ChecklistRun run = new ChecklistRun();
        run.setId(200L);
        run.setTask(task);
        run.setCompletedBy(tech);
        run.setStatus(ChecklistRunStatus.APPROVED);
        run.setStartTime(LocalDateTime.of(2026, 5, 12, 9, 0));
        run.setEndTime(LocalDateTime.of(2026, 5, 12, 9, 45));
        run.setManagerComments("Looks good");

        ChecklistResponse resp = new ChecklistResponse();
        resp.setBooleanAnswer(true);
        resp.setTextAnswer("All clear");
        resp.setChecklistRun(run);
        resp.setChecklistItem(question);

        TaskPause pause = new TaskPause();
        pause.setId(300L);
        pause.setChecklistRun(run);
        pause.setStartTime(LocalDateTime.of(2026, 5, 12, 9, 15));
        pause.setEndTime(LocalDateTime.of(2026, 5, 12, 9, 20));
        pause.setReason("Lunch");

        TimeEntry entry = new TimeEntry();
        entry.setId(400L);
        entry.setUser(tech);
        entry.setDate(LocalDate.of(2026, 5, 12));
        entry.setClockInTime(LocalDateTime.of(2026, 5, 12, 8, 0));
        entry.setClockOutTime(LocalDateTime.of(2026, 5, 12, 17, 0));
        entry.setTotalHours(9.0);

        when(taskRepo.findAll()).thenReturn(List.of(task));
        when(runRepo.findAll()).thenReturn(List.of(run));
        when(responseRepo.findAll()).thenReturn(List.of(resp));
        when(pauseRepo.findAll()).thenReturn(List.of(pause));
        when(timeEntryRepo.findAll()).thenReturn(List.of(entry));

        byte[] bytes = exportService.buildWorkbook();

        try (Workbook wb = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            Sheet tasks = wb.getSheet("Tasks");
            assertEquals(1, tasks.getLastRowNum());
            assertEquals("Morning Inspection", tasks.getRow(1).getCell(1).getStringCellValue());

            Sheet runs = wb.getSheet("Checklist Runs");
            assertEquals(1, runs.getLastRowNum());
            assertEquals("Morning Inspection", runs.getRow(1).getCell(1).getStringCellValue());
            assertEquals("Tech User", runs.getRow(1).getCell(2).getStringCellValue());
            assertEquals("APPROVED", runs.getRow(1).getCell(3).getStringCellValue());
            assertEquals(45.0, runs.getRow(1).getCell(6).getNumericCellValue(), 0.01);

            Sheet responses = wb.getSheet("Checklist Responses");
            assertEquals(1, responses.getLastRowNum());
            assertEquals("Tech User", responses.getRow(1).getCell(2).getStringCellValue());
            assertEquals("Is temp OK?", responses.getRow(1).getCell(3).getStringCellValue());
            assertEquals("true", responses.getRow(1).getCell(5).getStringCellValue());
            assertEquals("All clear", responses.getRow(1).getCell(6).getStringCellValue());

            Sheet pauses = wb.getSheet("Task Pauses");
            assertEquals(1, pauses.getLastRowNum());
            assertEquals("Lunch", pauses.getRow(1).getCell(7).getStringCellValue());
            assertEquals(5.0, pauses.getRow(1).getCell(6).getNumericCellValue(), 0.01);

            Sheet timeEntries = wb.getSheet("Time Entries");
            assertEquals(1, timeEntries.getLastRowNum());
            assertEquals("Tech User", timeEntries.getRow(1).getCell(1).getStringCellValue());
            assertEquals(9.0, timeEntries.getRow(1).getCell(5).getNumericCellValue(), 0.01);
        }
    }

    @Test
    void buildWorkbook_doesNotLeakEmails() throws Exception {
        when(taskRepo.findAll()).thenReturn(List.of(task));
        when(runRepo.findAll()).thenReturn(List.of());
        when(responseRepo.findAll()).thenReturn(List.of());
        when(pauseRepo.findAll()).thenReturn(List.of());
        when(timeEntryRepo.findAll()).thenReturn(List.of());

        byte[] bytes = exportService.buildWorkbook();
        // The user has email "tech@s2s.com" — make sure it doesn't appear
        // anywhere in the workbook. Search the raw byte stream as a
        // worst-case-leak detector that doesn't depend on sheet structure.
        String haystack = new String(bytes);
        assertFalse(haystack.contains("tech@s2s.com"),
                "Workbook should not contain user email addresses");
    }
}
