package com.example.seed2stem;

import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.ArrayList;
import java.util.List;

/** Batch records: the list of batches and each batch's running log. Managers and developers only. */
@Controller
@RequestMapping("/batch-records")
public class BatchRecordController {

    /** Plants stay in veg about 3 weeks after cloning (assumed for now). */
    static final int VEG_DAYS = 21;
    /** About 3 months from transfer to harvest. */
    static final int FLOWER_DAYS = 90;

    private final BatchRecordService batchRecordService;
    private final BatchRecordExportService exportService;

    public BatchRecordController(BatchRecordService batchRecordService,
                                 BatchRecordExportService exportService) {
        this.batchRecordService = batchRecordService;
        this.exportService = exportService;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public String list(HttpSession session, Model model) {
        if (!canView(session)) return redirectFor(session);

        List<BatchRow> rows = new ArrayList<>();
        for (Batch b : batchRecordService.allBatches()) {
            rows.add(new BatchRow(b, batchRecordService.totalsFor(b)));
        }
        model.addAttribute("rows", rows);
        model.addAttribute("pendingNewBatches", batchRecordService.pendingNewBatches());
        model.addAttribute("filterStatuses", batchRecordService.filterStatuses().values());
        return "batch-records";
    }

    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public String detail(@PathVariable Long id, HttpSession session, Model model) {
        if (!canView(session)) return redirectFor(session);

        Batch batch = batchRecordService.findBatch(id).orElse(null);
        if (batch == null) return "redirect:/batch-records";

        BatchTotals totals = batchRecordService.totalsFor(batch);
        model.addAttribute("batch", batch);
        model.addAttribute("totals", totals);
        model.addAttribute("log", batchRecordService.logFor(batch));
        model.addAttribute("filterStatus", batchRecordService.filterStatus(batch.getRoom()));
        model.addAttribute("expectedTransfer", batch.getStartDate().plusDays(VEG_DAYS));
        model.addAttribute("expectedHarvest", totals.getFirstTransferDate() != null
                ? totals.getFirstTransferDate().plusDays(FLOWER_DAYS) : null);
        return "batch-record-detail";
    }

    @GetMapping("/{id}/export.xlsx")
    public ResponseEntity<byte[]> export(@PathVariable Long id, HttpSession session) {
        if (!canView(session)) return ResponseEntity.status(403).build();
        Batch batch = batchRecordService.findBatch(id).orElse(null);
        if (batch == null) return ResponseEntity.notFound().build();

        byte[] bytes = exportService.export(batch);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"Batch-Record-" + batch.getBatchCode() + ".xlsx\"")
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(bytes);
    }

    private static boolean canView(HttpSession session) {
        User user = (User) session.getAttribute("loggedInUser");
        return user != null && (user.getAccountType() == AccountType.MANAGER
                || user.getAccountType() == AccountType.DEVELOPER);
    }

    private static String redirectFor(HttpSession session) {
        return session.getAttribute("loggedInUser") == null
                ? "redirect:/auth/login" : "redirect:/dashboard/home-dashboard";
    }

    public static class BatchRow {
        private final Batch batch;
        private final BatchTotals totals;

        BatchRow(Batch batch, BatchTotals totals) {
            this.batch = batch;
            this.totals = totals;
        }

        public Batch getBatch() { return batch; }
        public BatchTotals getTotals() { return totals; }
    }
}
