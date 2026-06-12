package com.example.seed2stem;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/developer")
public class DeveloperController {

    private final TaskRepository taskRepo;
    private final ChecklistRunRepository runRepo;
    private final RegistrationRequestRepository registrationRequestRepo;
    private final PasswordResetRequestRepository passwordResetRequestRepo;
    private final AuthService authService;
    private final ChecklistRepository checklistRepo;

    public DeveloperController(TaskRepository taskRepo,
                               ChecklistRunRepository runRepo,
                               RegistrationRequestRepository registrationRequestRepo,
                               PasswordResetRequestRepository passwordResetRequestRepo,
                               AuthService authService,
                               ChecklistRepository checklistRepo) {
        this.taskRepo = taskRepo;
        this.runRepo = runRepo;
        this.registrationRequestRepo = registrationRequestRepo;
        this.passwordResetRequestRepo = passwordResetRequestRepo;
        this.authService = authService;
        this.checklistRepo = checklistRepo;
    }

    @GetMapping("/user-created-tasks")
    public String userCreatedTasks(HttpSession session, Model model,
                                   @RequestParam(value = "showArchived", required = false,
                                                 defaultValue = "false") boolean showArchived,
                                   @RequestParam(value = "success", required = false) String success,
                                   @RequestParam(value = "error", required = false) String error) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null) return "redirect:/auth/login";
        if (user.getAccountType() != AccountType.DEVELOPER) {
            return "redirect:/dashboard/home-dashboard";
        }

        List<Task> tasks = showArchived
                ? taskRepo.findByUserCreatedTrueAndDeletedTrue()
                : taskRepo.findByUserCreatedTrueAndDeletedFalse();
        Map<Long, Long> taskRunMap = new HashMap<>();
        for (Task task : tasks) {
            runRepo.findFirstByTask(task).ifPresent(run ->
                    taskRunMap.put(task.getId(), run.getId()));
        }

        model.addAttribute("userCreatedTasks", tasks);
        model.addAttribute("taskRunMap", taskRunMap);
        model.addAttribute("showArchived", showArchived);
        model.addAttribute("success", success);
        model.addAttribute("error", error);
        return "user-created-tasks";
    }

    @GetMapping("/registration-requests")
    public String registrationRequests(HttpSession session, Model model) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null) return "redirect:/auth/login";
        if (user.getAccountType() != AccountType.DEVELOPER) {
            return "redirect:/dashboard/home-dashboard";
        }

        model.addAttribute("pendingRequests",
                registrationRequestRepo.findByStatus(RegistrationStatus.PENDING));
        return "registration-requests";
    }

    @PostMapping("/registration-requests/{id}/approve")
    public String approveRegistration(@PathVariable Long id,
                                      @RequestParam String accountType,
                                      HttpSession session,
                                      RedirectAttributes redirectAttributes) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null) return "redirect:/auth/login";
        if (user.getAccountType() != AccountType.DEVELOPER) {
            return "redirect:/dashboard/home-dashboard";
        }

        try {
            AccountType type = AccountType.valueOf(accountType);
            authService.approveRegistration(id, type);
        } catch (IllegalArgumentException e) {
            redirectAttributes.addAttribute("error", "Invalid account type selected");
        } catch (RuntimeException e) {
            redirectAttributes.addAttribute("error", e.getMessage());
        }
        return "redirect:/developer/registration-requests";
    }

    @PostMapping("/registration-requests/{id}/deny")
    public String denyRegistration(@PathVariable Long id, HttpSession session) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null) return "redirect:/auth/login";
        if (user.getAccountType() != AccountType.DEVELOPER) {
            return "redirect:/dashboard/home-dashboard";
        }

        authService.denyRegistration(id);
        return "redirect:/developer/registration-requests";
    }

    @GetMapping("/password-reset-requests")
    public String passwordResetRequests(HttpSession session, Model model,
                                        @RequestParam(value = "error", required = false) String error,
                                        @RequestParam(value = "success", required = false) String success) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null) return "redirect:/auth/login";
        if (user.getAccountType() != AccountType.DEVELOPER) {
            return "redirect:/dashboard/home-dashboard";
        }

        model.addAttribute("pendingRequests",
                passwordResetRequestRepo.findByStatus(PasswordResetStatus.PENDING));
        model.addAttribute("error", error);
        model.addAttribute("success", success);
        return "password-reset-requests";
    }

    @PostMapping("/password-reset-requests/{id}/approve")
    public String approvePasswordReset(@PathVariable Long id,
                                       @RequestParam String newPassword,
                                       HttpSession session,
                                       RedirectAttributes redirectAttributes) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null) return "redirect:/auth/login";
        if (user.getAccountType() != AccountType.DEVELOPER) {
            return "redirect:/dashboard/home-dashboard";
        }

        try {
            authService.approvePasswordReset(id, newPassword, user);
            redirectAttributes.addAttribute("success",
                    "Password reset approved. Share the new password with the user.");
        } catch (RuntimeException e) {
            redirectAttributes.addAttribute("error", e.getMessage());
        }
        return "redirect:/developer/password-reset-requests";
    }

    @PostMapping("/password-reset-requests/{id}/deny")
    public String denyPasswordReset(@PathVariable Long id,
                                    HttpSession session,
                                    RedirectAttributes redirectAttributes) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null) return "redirect:/auth/login";
        if (user.getAccountType() != AccountType.DEVELOPER) {
            return "redirect:/dashboard/home-dashboard";
        }

        try {
            authService.denyPasswordReset(id, user);
        } catch (RuntimeException e) {
            redirectAttributes.addAttribute("error", e.getMessage());
        }
        return "redirect:/developer/password-reset-requests";
    }

    /**
     * Soft-delete a task. Flips the `deleted` flag so the task is hidden
     * from the technician's Available Tasks list and from this page's
     * default view. All historical runs/responses/pauses remain intact.
     * IN_PROGRESS runs for the task are NOT disrupted — they stay in
     * Active Tasks and can be submitted/approved normally.
     *
     * Accepts a `redirect` param so we know which list to send the
     * developer back to (standard-tasks vs user-created-tasks).
     */
    @PostMapping("/tasks/{id}/archive")
    public String archiveTask(@PathVariable Long id,
                              @RequestParam(value = "redirect", required = false) String redirect,
                              HttpSession session,
                              RedirectAttributes redirectAttributes) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null) return "redirect:/auth/login";
        if (user.getAccountType() != AccountType.DEVELOPER) {
            return "redirect:/dashboard/home-dashboard";
        }

        Task task = taskRepo.findById(id).orElse(null);
        if (task == null) {
            redirectAttributes.addAttribute("error", "Task not found");
        } else if (task.isDeleted()) {
            redirectAttributes.addAttribute("error", "Task is already archived");
        } else {
            task.setDeleted(true);
            taskRepo.save(task);
            redirectAttributes.addAttribute("success",
                    "Archived task \"" + task.getTitle() + "\"");
        }
        return "redirect:" + safeRedirectTarget(redirect, task);
    }

    /** Undo an archive — flips `deleted` back to false. */
    @PostMapping("/tasks/{id}/restore")
    public String restoreTask(@PathVariable Long id,
                              @RequestParam(value = "redirect", required = false) String redirect,
                              HttpSession session,
                              RedirectAttributes redirectAttributes) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null) return "redirect:/auth/login";
        if (user.getAccountType() != AccountType.DEVELOPER) {
            return "redirect:/dashboard/home-dashboard";
        }

        Task task = taskRepo.findById(id).orElse(null);
        if (task == null) {
            redirectAttributes.addAttribute("error", "Task not found");
        } else if (!task.isDeleted()) {
            redirectAttributes.addAttribute("error", "Task is not archived");
        } else {
            task.setDeleted(false);
            taskRepo.save(task);
            redirectAttributes.addAttribute("success",
                    "Restored task \"" + task.getTitle() + "\"");
        }
        return "redirect:" + safeRedirectTarget(redirect, task);
    }

    /** Whitelist redirect targets to avoid open-redirect on a user-supplied param. */
    private String safeRedirectTarget(String redirect, Task task) {
        if ("user-created-tasks".equals(redirect)) {
            return "/developer/user-created-tasks";
        }
        if ("standard-tasks".equals(redirect)) {
            return "/developer/standard-tasks";
        }
        // Default: pick based on task type, fall back to standard-tasks
        if (task != null && task.isUserCreated()) {
            return "/developer/user-created-tasks";
        }
        return "/developer/standard-tasks";
    }

    @GetMapping("/standard-tasks")
    public String standardTasks(HttpSession session, Model model,
                                @RequestParam(value = "success", required = false) String success,
                                @RequestParam(value = "error", required = false) String error,
                                @RequestParam(value = "showArchived", required = false,
                                              defaultValue = "false") boolean showArchived) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null) return "redirect:/auth/login";
        if (user.getAccountType() != AccountType.DEVELOPER) {
            return "redirect:/dashboard/home-dashboard";
        }

        List<Task> tasks = showArchived
                ? taskRepo.findByUserCreatedFalseAndDeletedTrueOrderByTitleAsc()
                : taskRepo.findByUserCreatedFalseAndDeletedFalseOrderByTitleAsc();
        model.addAttribute("standardTasks", tasks);
        model.addAttribute("showArchived", showArchived);
        model.addAttribute("success", success);
        model.addAttribute("error", error);
        return "standard-tasks";
    }

    @GetMapping("/standard-tasks/new")
    public String newStandardTaskForm(HttpSession session, Model model,
                                      @RequestParam(value = "error", required = false) String error) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null) return "redirect:/auth/login";
        if (user.getAccountType() != AccountType.DEVELOPER) {
            return "redirect:/dashboard/home-dashboard";
        }

        model.addAttribute("responseTypes", ChecklistResponseType.selectableForBuilder());
        model.addAttribute("error", error);
        return "create-standard-task";
    }

    @PostMapping("/standard-tasks")
    public String createStandardTask(@RequestParam String title,
                                     @RequestParam(required = false) String description,
                                     @RequestParam(required = false) List<String> itemType,
                                     @RequestParam(required = false) List<String> itemText,
                                     @RequestParam(required = false) List<String> responseType,
                                     @RequestParam(value = "sopPdfFile", required = false) MultipartFile sopPdfFile,
                                     HttpSession session,
                                     RedirectAttributes redirectAttributes) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null) return "redirect:/auth/login";
        if (user.getAccountType() != AccountType.DEVELOPER) {
            return "redirect:/dashboard/home-dashboard";
        }

        if (title == null || title.trim().isEmpty()) {
            redirectAttributes.addAttribute("error", "Task name is required");
            return "redirect:/developer/standard-tasks/new";
        }
        if (itemType == null || itemType.isEmpty()) {
            redirectAttributes.addAttribute("error", "At least one checklist item is required");
            return "redirect:/developer/standard-tasks/new";
        }

        // Validate uploaded SOP PDF (optional). Reject non-PDFs by extension,
        // content-type, and magic bytes to prevent disguised uploads.
        byte[] sopBytes = null;
        String sopName = null;
        if (sopPdfFile != null && !sopPdfFile.isEmpty()) {
            String originalName = sopPdfFile.getOriginalFilename();
            String contentType = sopPdfFile.getContentType();
            if (originalName == null || !originalName.toLowerCase().endsWith(".pdf")
                    || contentType == null || !contentType.equalsIgnoreCase("application/pdf")) {
                redirectAttributes.addAttribute("error", "SOP file must be a PDF.");
                return "redirect:/developer/standard-tasks/new";
            }
            try {
                sopBytes = sopPdfFile.getBytes();
            } catch (IOException e) {
                redirectAttributes.addAttribute("error", "Failed to read uploaded SOP: " + e.getMessage());
                return "redirect:/developer/standard-tasks/new";
            }
            // PDF magic-byte check: every valid PDF starts with "%PDF".
            if (sopBytes.length < 4
                    || sopBytes[0] != '%' || sopBytes[1] != 'P'
                    || sopBytes[2] != 'D' || sopBytes[3] != 'F') {
                redirectAttributes.addAttribute("error", "Uploaded SOP is not a valid PDF document.");
                return "redirect:/developer/standard-tasks/new";
            }
            sopName = originalName;
        }

        try {
            Checklist checklist = new Checklist();
            checklist.setName(title.trim());
            checklist.setVersion(1);

            int displayOrder = 1;
            int questionOrder = 1;
            for (int i = 0; i < itemType.size(); i++) {
                String type = itemType.get(i);
                String text = (itemText != null && i < itemText.size()) ? itemText.get(i) : "";
                if (text == null || text.trim().isEmpty()) continue;

                ChecklistItem item = new ChecklistItem();
                item.setText(text.trim());
                item.setDisplayOrder(displayOrder++);
                item.setChecklist(checklist);

                if ("HEADER".equals(type)) {
                    item.setItemType(ChecklistItemType.HEADER);
                    item.setResponseType(ChecklistResponseType.NONE);
                } else {
                    item.setItemType(ChecklistItemType.QUESTION);
                    String rt = (responseType != null && i < responseType.size()) ? responseType.get(i) : "TEXT";
                    item.setResponseType(ChecklistResponseType.valueOf(
                            (rt == null || rt.isEmpty()) ? "TEXT" : rt));
                    item.setQuestionOrder(questionOrder++);
                }
                checklist.addItem(item);
            }

            if (checklist.getItems().isEmpty()) {
                redirectAttributes.addAttribute("error", "At least one non-empty checklist item is required");
                return "redirect:/developer/standard-tasks/new";
            }

            Task task = new Task();
            task.setTitle(title.trim());
            task.setDescription(description != null ? description.trim() : null);
            task.setChecklist(checklist);
            task.setUserCreated(false);
            task.setCreatedBy(user);
            if (sopBytes != null) {
                task.setSopFileName(sopName);
                task.setSopData(sopBytes);
            }

            checklistRepo.save(checklist);
            taskRepo.save(task);

            redirectAttributes.addAttribute("success", "Standard task \"" + task.getTitle() + "\" created");
            return "redirect:/developer/standard-tasks";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addAttribute("error", "Invalid response type or category: " + e.getMessage());
            return "redirect:/developer/standard-tasks/new";
        } catch (RuntimeException e) {
            redirectAttributes.addAttribute("error", "Failed to create task: " + e.getMessage());
            return "redirect:/developer/standard-tasks/new";
        }
    }

    /** Friendly redirect when a developer uploads an SOP larger than the multipart limit. */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public String handleMaxUploadSize(RedirectAttributes redirectAttributes) {
        redirectAttributes.addAttribute("error", "SOP file is too large. Maximum size is 10 MB.");
        return "redirect:/developer/standard-tasks/new";
    }
}
