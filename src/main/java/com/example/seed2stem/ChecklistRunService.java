package com.example.seed2stem;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class ChecklistRunService {

    private final ChecklistRunRepository runRepo;

    public ChecklistRunService(ChecklistRunRepository runRepo) {
        this.runRepo = runRepo;
    }

    public ChecklistRun getChecklistById(Long runId) {
        return runRepo.findById(runId).orElseThrow();
    }

    @Transactional
    public ChecklistRun submitByTechnician(ChecklistRun run) {
        if (run.getStatus() != ChecklistRunStatus.IN_PROGRESS
                && run.getStatus() != ChecklistRunStatus.REJECTED) {
            throw new RuntimeException("Checklist run is not in a submittable state");
        }

        validateResponses(run.getResponses());

        run.setEndTime(LocalDateTime.now());
        run.setStatus(ChecklistRunStatus.PENDING);
        return runRepo.save(run);
    }

    @Transactional
    public ChecklistRun pauseByUser(Long runId, List<ChecklistResponse> responses) {
        ChecklistRun run = runRepo.findById(runId)
                .orElseThrow(() -> new RuntimeException("Checklist run not found"));

        if (run.getStatus() != ChecklistRunStatus.IN_PROGRESS) {
            throw new RuntimeException("Can only pause an in-progress checklist run");
        }

        run.getResponses().clear();
        responses.forEach(r -> r.setChecklistRun(run));
        run.getResponses().addAll(responses);

        return runRepo.save(run);
    }

    @Transactional
    public ChecklistRun authorizeByManager(Long runId, User manager, String managerComments) {
        ChecklistRun run = runRepo.findById(runId)
                .orElseThrow(() -> new RuntimeException("ChecklistRun not found"));

        if (run.getStatus() != ChecklistRunStatus.PENDING) {
            throw new RuntimeException("ChecklistRun is not pending approval");
        }

        run.setAuthorizedBy(manager);
        run.setAuthorizedAt(LocalDateTime.now());
        run.setManagerComments(managerComments);
        run.setStatus(ChecklistRunStatus.APPROVED);

        return runRepo.save(run);
    }

    @Transactional
    public ChecklistRun rejectByManager(Long runId, User manager, String managerComments) {
        ChecklistRun run = runRepo.findById(runId)
                .orElseThrow(() -> new RuntimeException("ChecklistRun not found"));

        if (run.getStatus() != ChecklistRunStatus.PENDING) {
            throw new RuntimeException("ChecklistRun is not pending approval");
        }

        run.setAuthorizedBy(manager);
        run.setAuthorizedAt(LocalDateTime.now());
        run.setManagerComments(managerComments);
        run.setStatus(ChecklistRunStatus.REJECTED);

        return runRepo.save(run);
    }

    public List<ChecklistRun> getPendingChecklists() {
        return runRepo.findByStatus(ChecklistRunStatus.PENDING);
    }

    public List<ChecklistRun> getActiveRunsForUser(User user) {
        return runRepo.findByCompletedByAndStatus(user, ChecklistRunStatus.IN_PROGRESS);
    }

    public Optional<ChecklistRun> findExistingInProgressRun(User user, Task task) {
        return runRepo.findByCompletedByAndTaskAndStatus(user, task, ChecklistRunStatus.IN_PROGRESS);
    }

    private void validateResponses(List<ChecklistResponse> responses) {
        if (responses == null || responses.isEmpty()) {
            throw new RuntimeException("No responses provided");
        }

        for (ChecklistResponse resp : responses) {
            ChecklistItem item = resp.getChecklistItem();
            if (item == null) {
                throw new RuntimeException("Response missing checklist item reference");
            }

            ChecklistResponseType type = item.getResponseType();

            switch (type) {
                case BOOLEAN_TEXT -> {
                    if (resp.getBooleanAnswer() == null) {
                        throw new RuntimeException("Boolean answer required for item: " + item.getText());
                    }
                    resp.setNumericAnswer(null);
                }
                case TEXT -> {
                    resp.setBooleanAnswer(null);
                    resp.setNumericAnswer(null);
                }
                case INTEGER, NUMBER, DECIMAL -> {
                    if (resp.getNumericAnswer() == null) {
                        throw new RuntimeException("Numeric answer required for item: " + item.getText());
                    }
                    resp.setBooleanAnswer(null);
                    resp.setTextAnswer(null);
                }
                case NONE -> {
                    resp.setBooleanAnswer(null);
                    resp.setTextAnswer(null);
                    resp.setNumericAnswer(null);
                }
            }
        }
    }
}
