package com.example.seed2stem;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ChecklistRunService {

    private final ChecklistRunRepository runRepo;

    public ChecklistRunService(ChecklistRunRepository runRepo) {
        this.runRepo = runRepo;
    }

    // Technician submits checklist
    @Transactional
    public ChecklistRun submitByTechnician(ChecklistRun run) {
        run.setCompletedAt(LocalDateTime.now());
        run.setStatus(ChecklistRunStatus.PENDING);
        return runRepo.save(run);
    }

    // Manager approves checklist
    @Transactional
    public ChecklistRun authorizeByManager(Long runId, User manager) {
        ChecklistRun run = runRepo.findById(runId)
                .orElseThrow(() -> new RuntimeException("ChecklistRun not found"));

        if (run.getStatus() != ChecklistRunStatus.PENDING) {
            throw new RuntimeException("ChecklistRun is not pending approval");
        }

        run.setAuthorizedBy(manager);
        run.setAuthorizedAt(LocalDateTime.now());
        run.setStatus(ChecklistRunStatus.APPROVED);

        return runRepo.save(run);
    }

    // Manager edits responses (while still PENDING)
    @Transactional
    public ChecklistRun updateResponses(Long runId, List<ChecklistResponse> responses) {
        ChecklistRun run = runRepo.findById(runId)
                .orElseThrow(() -> new RuntimeException("Checklist not found"));

        if (run.getStatus() != ChecklistRunStatus.PENDING) {
            throw new RuntimeException("Cannot edit approved checklist");
        }

        run.getResponses().clear();
        responses.forEach(r -> r.setChecklistRun(run));
        run.getResponses().addAll(responses);

        return runRepo.save(run);
    }

    public List<ChecklistRun> getPendingChecklists() {
        return runRepo.findByStatus(ChecklistRunStatus.PENDING);
    }
}


