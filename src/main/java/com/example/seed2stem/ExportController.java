package com.example.seed2stem;

import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.time.LocalDate;

/**
 * Streams the full task / timeline export as a single .xlsx download.
 * Developers only.
 */
@Controller
@RequestMapping("/admin")
public class ExportController {

    private static final Logger log = LoggerFactory.getLogger(ExportController.class);

    private static final MediaType XLSX_TYPE = MediaType.parseMediaType(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    private final ExcelExportService exportService;

    public ExportController(ExcelExportService exportService) {
        this.exportService = exportService;
    }

    @GetMapping("/export.xlsx")
    public ResponseEntity<byte[]> exportAllData(HttpSession session) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null) {
            return ResponseEntity.status(401).build();
        }
        if (user.getAccountType() != AccountType.DEVELOPER) {
            return ResponseEntity.status(403).build();
        }

        // Build inside try/catch so a workbook-construction failure (e.g.
        // OutOfMemoryError on small-RAM hosts like Railway's free tier)
        // returns a real 500 instead of being swallowed by
        // GlobalExceptionHandler's "redirect to home" fallback — which made
        // the failure look like the browser just refreshing the dashboard.
        try {
            byte[] bytes = exportService.buildWorkbook();
            String filename = "seed2stem-export-" + LocalDate.now() + ".xlsx";

            return ResponseEntity.ok()
                    .contentType(XLSX_TYPE)
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + filename + "\"")
                    .body(bytes);
        } catch (OutOfMemoryError oom) {
            // POI loads the whole workbook in memory. On low-RAM hosts the
            // export can OOM as data grows. Log loudly so the operator can
            // upgrade the host (or we can switch to streaming SXSSFWorkbook).
            log.error("Excel export ran out of memory", oom);
            return ResponseEntity.status(500)
                    .contentType(MediaType.TEXT_PLAIN)
                    .body(("Export failed: server ran out of memory while " +
                            "building the workbook. Consider upgrading the host or " +
                            "scoping the export.").getBytes());
        } catch (Exception ex) {
            log.error("Excel export failed", ex);
            return ResponseEntity.status(500)
                    .contentType(MediaType.TEXT_PLAIN)
                    .body(("Export failed: " + ex.getMessage()).getBytes());
        }
    }

    /**
     * Local exception handler — beats {@link GlobalExceptionHandler} for any
     * exception that escapes this controller, so binary endpoints never
     * accidentally redirect to a HTML view. Returns a 500 with a short
     * text/plain body instead.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<byte[]> handleControllerException(Exception ex) {
        log.error("Unhandled exception in ExportController", ex);
        return ResponseEntity.status(500)
                .contentType(MediaType.TEXT_PLAIN)
                .body(("Export failed: " + ex.getClass().getSimpleName()
                        + ": " + ex.getMessage()).getBytes());
    }
}
