package com.example.seed2stem;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Fills the PRO-014.00-F.01 Cannabis Batch Record template with one batch's
 * approved totals. Values are written into the template's existing cells so
 * its styles, merges and print setup are kept.
 */
@Service
public class BatchRecordExportService {

    static final String TEMPLATE = "excel/batch-record-template.xlsx";

    // Template columns (0-based): A Date, B Entry Type, C Batch, D Strain, E Material,
    // F Room, H Quantity, I Staff, J Witness, K Notes.
    private static final int COL_DATE = 0, COL_TYPE = 1, COL_BATCH = 2, COL_STRAIN = 3, COL_MATERIAL = 4,
            COL_ROOM = 5, COL_QTY = 7, COL_STAFF = 8, COL_WITNESS = 9, COL_NOTES = 10;

    // Template rows (1-based, as in Excel).
    private static final int ROW_CUTTING = 9, ROW_VEG_DESTROYED = 10, ROW_FLOWERING = 11, ROW_HARVESTED = 12,
            ROW_HARVEST_WEIGHT = 13, ROW_DRY_WEIGHT = 14, ROW_FLOWER_WEIGHT = 15, ROW_TRIM = 16,
            ROW_DESTROYED = 17, ROW_LOSS = 18;

    private final BatchRecordService batchRecordService;

    public BatchRecordExportService(BatchRecordService batchRecordService) {
        this.batchRecordService = batchRecordService;
    }

    @Transactional(readOnly = true)
    public byte[] export(Batch batch) {
        BatchTotals t = batchRecordService.totalsFor(batch);
        List<BatchRecordEntry> approved = batchRecordService.logFor(batch).stream()
                .filter(e -> e.getStatus() == BatchEntryStatus.APPROVED).toList();

        try (InputStream in = new ClassPathResource(TEMPLATE).getInputStream();
             Workbook wb = new XSSFWorkbook(in);
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.getSheetAt(0);

            set(sheet, 3, 2, LocalDate.now());          // C3: date
            set(sheet, 3, 6, batch.getBatchCode());     // G3: batch

            for (int r = ROW_CUTTING; r <= ROW_LOSS; r++) {
                set(sheet, r, COL_BATCH, batch.getBatchCode());
                set(sheet, r, COL_STRAIN, batch.getVariety().getName());
                set(sheet, r, COL_ROOM, batch.getRoom().getDisplayName());
            }

            List<BatchRecordEntry> started = of(approved, e -> e.getAction().isCreatesBatch());
            fill(sheet, ROW_CUTTING, started, t.getPlantsStarted(),
                    batch.getSource() == BatchSource.SEEDS ? "Seeds received" : null);

            LocalDate flowerStart = t.getFirstTransferDate();
            List<BatchRecordEntry> plantDestructions = of(approved, e -> e.getAction() == BatchAction.DESTRUCTION
                    && e.getMaterial() == DestructionMaterial.WHOLE_PLANTS);
            List<BatchRecordEntry> vegDestroyed = of(plantDestructions,
                    e -> flowerStart == null || e.getEntryDate().isBefore(flowerStart));
            fill(sheet, ROW_VEG_DESTROYED, vegDestroyed,
                    vegDestroyed.stream().mapToInt(e -> e.getPlantCount() != null ? e.getPlantCount() : 0).sum(), null);

            List<BatchRecordEntry> transfers = of(approved, e -> e.getAction() == BatchAction.TRANSFER_TO_FLOWER);
            fill(sheet, ROW_FLOWERING, transfers, t.getPlantsTransferred(), t.getPlantsTransferred() > 0
                    ? t.getPlantsTransferred() + " plants transferred to " + batch.getRoom().getDisplayName() + " for flowering"
                    : null);

            List<BatchRecordEntry> harvests = of(approved, e -> e.getAction() == BatchAction.HARVEST);
            fill(sheet, ROW_HARVESTED, harvests, t.getPlantsHarvested(), null);
            fillKg(sheet, ROW_HARVEST_WEIGHT, harvests, t.getWetKg(), null);

            List<BatchRecordEntry> dry = of(approved, e -> e.getAction() == BatchAction.DRY_WEIGHT);
            fillKg(sheet, ROW_DRY_WEIGHT, dry, t.getDryKg(), null);

            List<BatchRecordEntry> trims = of(approved, e -> e.getAction() == BatchAction.TRIMMING);
            fillKg(sheet, ROW_FLOWER_WEIGHT, trims, t.getFlowerTrimmedKg(), "Flower trimmed");

            List<BatchRecordEntry> trimAll = of(approved, e -> e.getAction() == BatchAction.TRIMMING
                    || e.getAction() == BatchAction.DEBUCK_DESTEM);
            double trimKg = BatchTotals.round3(t.getFanLeafKg() + t.getStemKg() + t.getTrimKg());
            fillKg(sheet, ROW_TRIM, trimAll, trimKg, trimKg > 0
                    ? String.format("Fan leaves %.3f, stems %.3f, sugar leaves %.3f kg", t.getFanLeafKg(), t.getStemKg(), t.getTrimKg())
                    : null);

            // Row 17 is labelled "Veg. Destroyed" in the template; used here for all destroyed material by mass.
            List<BatchRecordEntry> destroyed = of(approved, e -> e.getAction() == BatchAction.DESTRUCTION);
            set(sheet, ROW_DESTROYED, COL_TYPE, "Destroyed");
            set(sheet, ROW_DESTROYED, COL_MATERIAL, "Mass (kg)");
            fillKg(sheet, ROW_DESTROYED, destroyed, t.getDestroyedKg(), destroyed.isEmpty() ? null
                    : destroyed.stream().map(e -> e.getMaterial().getDisplayName() + " " + String.format("%.3f", e.getWeightKg())
                            + " kg (" + e.getDestructionMethod() + ")").collect(Collectors.joining("; ")));
            set(sheet, ROW_DESTROYED, COL_WITNESS, join(destroyed.stream().map(BatchRecordEntry::getWitness).toList()));

            set(sheet, ROW_LOSS, COL_TYPE, "Drying/Processing Loss");
            set(sheet, ROW_LOSS, COL_MATERIAL, "Mass (kg)");
            if (t.getLossKg() != null) {
                set(sheet, ROW_LOSS, COL_DATE, dry.isEmpty() ? null : dry.get(dry.size() - 1).getEntryDate());
                set(sheet, ROW_LOSS, COL_QTY, t.getLossKg());
                if (t.getLossPercent() != null) set(sheet, ROW_LOSS, COL_NOTES, t.getLossPercent() + "% of wet weight");
            }

            wb.setForceFormulaRecalculation(true);
            wb.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Could not build the batch record workbook", e);
        }
    }

    private static List<BatchRecordEntry> of(List<BatchRecordEntry> entries, Predicate<BatchRecordEntry> test) {
        return entries.stream().filter(test).toList();
    }

    /** Count row: first date, total, the technicians who did the work. */
    private static void fill(Sheet sheet, int row, List<BatchRecordEntry> entries, int quantity, String notes) {
        if (entries.isEmpty() || quantity <= 0) return;
        set(sheet, row, COL_DATE, entries.get(0).getEntryDate());
        set(sheet, row, COL_QTY, quantity);
        set(sheet, row, COL_STAFF, staff(entries));
        if (notes != null) set(sheet, row, COL_NOTES, notes);
    }

    private static void fillKg(Sheet sheet, int row, List<BatchRecordEntry> entries, double kg, String notes) {
        if (entries.isEmpty() || kg <= 0) return;
        set(sheet, row, COL_DATE, entries.get(0).getEntryDate());
        set(sheet, row, COL_QTY, kg);
        set(sheet, row, COL_STAFF, staff(entries));
        if (notes != null) set(sheet, row, COL_NOTES, notes);
    }

    private static String staff(List<BatchRecordEntry> entries) {
        return join(entries.stream().map(e -> e.getTechnician().getName()).toList());
    }

    private static String join(List<String> values) {
        Set<String> unique = new LinkedHashSet<>();
        values.stream().filter(Objects::nonNull).forEach(unique::add);
        return String.join(", ", unique);
    }

    private static void set(Sheet sheet, int excelRow, int col, Object value) {
        if (value == null) return;
        Row row = sheet.getRow(excelRow - 1);
        if (row == null) row = sheet.createRow(excelRow - 1);
        Cell cell = row.getCell(col);
        if (cell == null) cell = row.createCell(col);
        if (value instanceof LocalDate d) cell.setCellValue(d);
        else if (value instanceof Number n) cell.setCellValue(n.doubleValue());
        else cell.setCellValue(value.toString());
    }
}
