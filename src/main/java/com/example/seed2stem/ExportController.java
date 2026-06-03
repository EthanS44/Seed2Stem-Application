package com.example.seed2stem;

import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.time.LocalDate;

/**
 * Streams the full task / timeline export as a single .xlsx download. Managers
 * and developers only — technicians don't get to pull org-wide data.
 */
@Controller
@RequestMapping("/admin")
public class ExportController {

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
            // 401 — browser will show a generic error; the admin panel link
            // shouldn't even be visible to unauthenticated users.
            return ResponseEntity.status(401).build();
        }
        if (user.getAccountType() != AccountType.DEVELOPER) {
            return ResponseEntity.status(403).build();
        }

        byte[] bytes = exportService.buildWorkbook();
        String filename = "seed2stem-export-" + LocalDate.now() + ".xlsx";

        return ResponseEntity.ok()
                .contentType(XLSX_TYPE)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + filename + "\"")
                .body(bytes);
    }
}
