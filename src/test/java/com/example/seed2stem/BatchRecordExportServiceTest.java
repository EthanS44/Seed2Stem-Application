package com.example.seed2stem;

import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/** Fills the real PRO-014.00-F.01 template and reads the cells back. */
@ExtendWith(MockitoExtension.class)
class BatchRecordExportServiceTest {

    @Mock private BatchRecordService batchRecordService;
    @InjectMocks private BatchRecordExportService exportService;

    private final User tech = new User("t", "h", "Tess", "Tech", AccountType.TECHNICIAN);

    private BatchRecordEntry approved(BatchAction action, Integer plants, Double kg) {
        BatchRecordEntry e = new BatchRecordEntry();
        e.setAction(action);
        e.setStatus(BatchEntryStatus.APPROVED);
        e.setTechnician(tech);
        e.setRoom(Room.B1);
        e.setEntryDate(LocalDate.of(2026, 10, 8));
        e.setPlantCount(plants);
        e.setWeightKg(kg);
        return e;
    }

    @Test
    void export_fillsTemplateRowsFromApprovedTotals() throws Exception {
        Batch batch = new Batch();
        batch.setBatchCode("261008-BLD-B1");
        batch.setVariety(new Variety("Blue Dream", "BLD"));
        batch.setRoom(Room.B1);
        batch.setSource(BatchSource.CLONES);
        batch.setStartDate(LocalDate.of(2026, 10, 8));

        BatchRecordEntry debuck = approved(BatchAction.DEBUCK_DESTEM, 50, null);
        debuck.setFanLeafKg(3.2);
        debuck.setStemKg(2.1);
        BatchRecordEntry trim = approved(BatchAction.TRIMMING, null, null);
        trim.setTrimKg(1.4);
        trim.setFlowerTrimmedKg(6.0);
        BatchRecordEntry destroy = approved(BatchAction.DESTRUCTION, null, 2.1);
        destroy.setMaterial(DestructionMaterial.STEMS);
        destroy.setDestructionMethod("Ground");
        destroy.setWitness("Pat Lee");
        List<BatchRecordEntry> log = new ArrayList<>(List.of(
                approved(BatchAction.CLONING, 52, null),
                approved(BatchAction.TRANSFER_TO_FLOWER, 50, null),
                approved(BatchAction.HARVEST, 50, 25.5),
                debuck, trim,
                approved(BatchAction.DRY_WEIGHT, null, 4.8),
                destroy));
        when(batchRecordService.logFor(batch)).thenReturn(log);
        when(batchRecordService.totalsFor(batch)).thenReturn(BatchTotals.of(log));

        byte[] bytes = exportService.export(batch);

        try (Workbook wb = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            Sheet s = wb.getSheetAt(0);
            assertEquals("261008-BLD-B1", text(s, 3, 6));
            assertEquals("Cutting", text(s, 9, 1));             // template label kept
            assertEquals("Blue Dream", text(s, 9, 3));
            assertEquals(52.0, num(s, 9, 7));                   // clones cut
            assertEquals("Tess Tech", text(s, 9, 8));
            assertEquals(50.0, num(s, 11, 7));                  // flowering transfer
            assertEquals("50 plants transferred to B1 for flowering", text(s, 11, 10));
            assertEquals(25.5, num(s, 13, 7), 1e-9);            // harvest (wet) weight
            assertEquals(4.8, num(s, 14, 7), 1e-9);             // dry weight
            assertEquals(6.0, num(s, 15, 7), 1e-9);             // flower weight (trimmed)
            assertEquals(6.7, num(s, 16, 7), 1e-9);             // trim incl. fan leaves + stems
            assertEquals(2.1, num(s, 17, 7), 1e-9);             // destroyed
            assertEquals("Pat Lee", text(s, 17, 9));
            assertEquals(14.0, num(s, 18, 7), 1e-9);            // drying/processing loss
            assertEquals("54.9% of wet weight", text(s, 18, 10));
            assertEquals(LocalDate.of(2026, 10, 8), s.getRow(8).getCell(0).getLocalDateTimeCellValue().toLocalDate());
        }
    }

    private static String text(Sheet s, int excelRow, int col) {
        return s.getRow(excelRow - 1).getCell(col).getStringCellValue();
    }

    private static double num(Sheet s, int excelRow, int col) {
        return s.getRow(excelRow - 1).getCell(col).getNumericCellValue();
    }
}
