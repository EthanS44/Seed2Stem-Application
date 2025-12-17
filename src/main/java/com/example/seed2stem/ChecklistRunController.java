package com.example.seed2stem;

import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/checklist-runs")
public class ChecklistRunController {

    private final ChecklistRunService service;

    public ChecklistRunController(ChecklistRunService service) {
        this.service = service;
    }

    // Technician submits checklist
    @PostMapping("/submit")
    public ChecklistRun submitChecklist(@RequestBody ChecklistRun run) {
        return service.submitByTechnician(run);
    }

    // Manager approves checklist
    @PostMapping("/authorize/{id}")
    public ChecklistRun authorizeChecklist(@PathVariable Long id, @RequestBody User manager) {
        return service.authorizeByManager(id, manager);
    }

    // Manager rejects checklist
    @PostMapping("/reject/{id}")
    public ChecklistRun rejectChecklist(@PathVariable Long id, @RequestBody User manager) {
        return service.rejectChecklist(id, manager);
    }

    // Get all pending checklists (for manager dashboard)
    @GetMapping("/pending")
    public List<ChecklistRun> getPendingChecklists() {
        return service.getPendingChecklists();
    }
}


