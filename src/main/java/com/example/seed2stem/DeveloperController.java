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

        addBuilderModel(model, null, error);
        return "create-standard-task";
    }

    @PostMapping("/standard-tasks")
    public String createStandardTask(@RequestParam String title,
                                     @RequestParam(required = false) String description,
                                     @RequestParam(required = false) List<String> itemType,
                                     @RequestParam(required = false) List<String> itemText,
                                     @RequestParam(required = false) List<String> responseType,
                                     @RequestParam(required = false) List<String> itemCategory,
                                     @RequestParam(value = "sopPdfFile", required = false) MultipartFile sopPdfFile,
                                     @RequestParam(value = "batchAction", required = false) String batchAction,
                                     HttpSession session,
                                     RedirectAttributes redirectAttributes) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null) return "redirect:/auth/login";
        if (user.getAccountType() != AccountType.DEVELOPER) {
            return "redirect:/dashboard/home-dashboard";
        }
        String back = "redirect:/developer/standard-tasks/new";

        try {
            TaskForm form = parseForm(title, batchAction, itemType, itemText, responseType, itemCategory, sopPdfFile);

            Checklist checklist = newChecklist(form.title(), 1, form.items());
            Task task = new Task();
            task.setTitle(form.title());
            task.setDescription(description != null ? description.trim() : null);
            task.setChecklist(checklist);
            task.setUserCreated(false);
            task.setCreatedBy(user);
            task.setBatchAction(form.action());
            if (form.sop() != null) {
                task.setSopFileName(form.sop().name());
                task.setSopData(form.sop().bytes());
            }

            checklistRepo.save(checklist);
            taskRepo.save(task);

            redirectAttributes.addAttribute("success", "Standard task \"" + task.getTitle() + "\" created");
            return "redirect:/developer/standard-tasks";
        } catch (InvalidTaskForm e) {
            redirectAttributes.addAttribute("error", e.getMessage());
            return back;
        } catch (RuntimeException e) {
            redirectAttributes.addAttribute("error", "Failed to create task: " + e.getMessage());
            return back;
        }
    }

    @GetMapping("/standard-tasks/{id}/edit")
    public String editStandardTaskForm(@PathVariable Long id, HttpSession session, Model model,
                                       @RequestParam(value = "error", required = false) String error) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null) return "redirect:/auth/login";
        if (user.getAccountType() != AccountType.DEVELOPER) {
            return "redirect:/dashboard/home-dashboard";
        }

        Task task = taskRepo.findById(id).orElse(null);
        if (task == null || task.isUserCreated()) {
            return "redirect:/developer/standard-tasks";
        }

        addBuilderModel(model, task, error);
        return "create-standard-task";
    }

    /**
     * Saves edits to a standard task. When the checklist items change, they are
     * saved as a new checklist version and the old one is kept, so past runs
     * still show the questions they were actually answered against.
     */
    @PostMapping("/standard-tasks/{id}")
    public String updateStandardTask(@PathVariable Long id,
                                     @RequestParam String title,
                                     @RequestParam(required = false) String description,
                                     @RequestParam(required = false) List<String> itemType,
                                     @RequestParam(required = false) List<String> itemText,
                                     @RequestParam(required = false) List<String> responseType,
                                     @RequestParam(required = false) List<String> itemCategory,
                                     @RequestParam(value = "sopPdfFile", required = false) MultipartFile sopPdfFile,
                                     @RequestParam(value = "removeSop", required = false, defaultValue = "false") boolean removeSop,
                                     @RequestParam(value = "batchAction", required = false) String batchAction,
                                     HttpSession session,
                                     RedirectAttributes redirectAttributes) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null) return "redirect:/auth/login";
        if (user.getAccountType() != AccountType.DEVELOPER) {
            return "redirect:/dashboard/home-dashboard";
        }

        Task task = taskRepo.findById(id).orElse(null);
        if (task == null || task.isUserCreated()) {
            return "redirect:/developer/standard-tasks";
        }
        String back = "redirect:/developer/standard-tasks/" + id + "/edit";

        try {
            TaskForm form = parseForm(title, batchAction, itemType, itemText, responseType, itemCategory, sopPdfFile);

            Checklist current = task.getChecklist();
            if (current == null || !sameItems(current.getItems(), form.items())) {
                int version = current != null && current.getVersion() != null ? current.getVersion() + 1 : 1;
                Checklist next = newChecklist(form.title(), version, form.items());
                checklistRepo.save(next);
                task.setChecklist(next);
            } else if (!form.title().equals(current.getName())) {
                current.setName(form.title());
                checklistRepo.save(current);
            }

            task.setTitle(form.title());
            task.setDescription(description != null ? description.trim() : null);
            task.setBatchAction(form.action());
            if (form.sop() != null) {
                task.setSopFileName(form.sop().name());
                task.setSopData(form.sop().bytes());
            } else if (removeSop) {
                task.setSopFileName(null);
                task.setSopData(null);
            }
            taskRepo.save(task);

            redirectAttributes.addAttribute("success", "Standard task \"" + task.getTitle() + "\" saved");
            return "redirect:/developer/standard-tasks" + (task.isDeleted() ? "?showArchived=true" : "");
        } catch (InvalidTaskForm e) {
            redirectAttributes.addAttribute("error", e.getMessage());
            return back;
        } catch (RuntimeException e) {
            redirectAttributes.addAttribute("error", "Failed to save task: " + e.getMessage());
            return back;
        }
    }

    // ------------------------------------------------------------ task builder helpers

    /** Model for the builder page; {@code task} is null when creating. */
    private void addBuilderModel(Model model, Task task, String error) {
        model.addAttribute("responseTypes", ChecklistResponseType.selectableForBuilder());
        model.addAttribute("batchActions", BatchAction.values());
        model.addAttribute("task", task);
        List<Map<String, String>> items = new java.util.ArrayList<>();
        if (task != null && task.getChecklist() != null) {
            for (ChecklistItem item : task.getChecklist().getItems()) {
                Map<String, String> row = new HashMap<>();
                row.put("type", item.getItemType().name());
                row.put("text", item.getText());
                row.put("responseType", item.getResponseType().name());
                row.put("responseLabel", item.getResponseType().getDisplayName());
                row.put("category", item.getCategory() != null ? item.getCategory().name() : "");
                items.add(row);
            }
        }
        model.addAttribute("existingItems", items);
        model.addAttribute("error", error);
    }

    private record SopUpload(String name, byte[] bytes) {}

    /** category is kept from existing items (seeded checklists use it for short review labels); null for new ones. */
    private record ItemSpec(ChecklistItemType type, String text, ChecklistResponseType responseType,
                            ChecklistItemCategory category) {}

    private record TaskForm(String title, BatchAction action, List<ItemSpec> items, SopUpload sop) {}

    /** A builder input the developer needs to fix; the message is shown on the form. */
    private static class InvalidTaskForm extends Exception {
        InvalidTaskForm(String message) {
            super(message);
        }
    }

    private TaskForm parseForm(String title, String batchAction, List<String> itemType, List<String> itemText,
                               List<String> responseType, List<String> itemCategory,
                               MultipartFile sopPdfFile) throws InvalidTaskForm {
        if (title == null || title.trim().isEmpty()) {
            throw new InvalidTaskForm("Task name is required");
        }

        BatchAction action;
        try {
            action = (batchAction == null || batchAction.isBlank())
                    ? BatchAction.NONE : BatchAction.valueOf(batchAction);
        } catch (IllegalArgumentException e) {
            throw new InvalidTaskForm("Unknown batch record action: " + batchAction);
        }

        List<ItemSpec> items = new java.util.ArrayList<>();
        if (itemType != null) {
            for (int i = 0; i < itemType.size(); i++) {
                String text = (itemText != null && i < itemText.size()) ? itemText.get(i) : "";
                if (text == null || text.trim().isEmpty()) continue;
                ChecklistItemCategory category = parseCategory(
                        (itemCategory != null && i < itemCategory.size()) ? itemCategory.get(i) : null);
                if ("HEADER".equals(itemType.get(i))) {
                    items.add(new ItemSpec(ChecklistItemType.HEADER, text.trim(), ChecklistResponseType.NONE, category));
                } else {
                    String rt = (responseType != null && i < responseType.size()) ? responseType.get(i) : "TEXT";
                    try {
                        items.add(new ItemSpec(ChecklistItemType.QUESTION, text.trim(),
                                ChecklistResponseType.valueOf((rt == null || rt.isEmpty()) ? "TEXT" : rt), category));
                    } catch (IllegalArgumentException e) {
                        throw new InvalidTaskForm("Invalid response type: " + rt);
                    }
                }
            }
        }
        // A batch task already has its inputs (room, batch, action fields), so checklist items are optional.
        if (action == BatchAction.NONE && items.isEmpty()) {
            throw new InvalidTaskForm("At least one checklist item is required");
        }

        return new TaskForm(title.trim(), action, items, readSop(sopPdfFile));
    }

    private static ChecklistItemCategory parseCategory(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return ChecklistItemCategory.valueOf(value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * Validates an uploaded SOP (optional). Rejects non-PDFs by extension,
     * content-type, and magic bytes to prevent disguised uploads.
     */
    private SopUpload readSop(MultipartFile sopPdfFile) throws InvalidTaskForm {
        if (sopPdfFile == null || sopPdfFile.isEmpty()) return null;
        String originalName = sopPdfFile.getOriginalFilename();
        String contentType = sopPdfFile.getContentType();
        if (originalName == null || !originalName.toLowerCase().endsWith(".pdf")
                || contentType == null || !contentType.equalsIgnoreCase("application/pdf")) {
            throw new InvalidTaskForm("SOP file must be a PDF.");
        }
        byte[] bytes;
        try {
            bytes = sopPdfFile.getBytes();
        } catch (IOException e) {
            throw new InvalidTaskForm("Failed to read uploaded SOP: " + e.getMessage());
        }
        // PDF magic-byte check: every valid PDF starts with "%PDF".
        if (bytes.length < 4 || bytes[0] != '%' || bytes[1] != 'P' || bytes[2] != 'D' || bytes[3] != 'F') {
            throw new InvalidTaskForm("Uploaded SOP is not a valid PDF document.");
        }
        return new SopUpload(originalName, bytes);
    }

    private static Checklist newChecklist(String name, int version, List<ItemSpec> specs) {
        Checklist checklist = new Checklist();
        checklist.setName(name);
        checklist.setVersion(version);
        int displayOrder = 1;
        int questionOrder = 1;
        for (ItemSpec spec : specs) {
            ChecklistItem item = new ChecklistItem();
            item.setText(spec.text());
            item.setItemType(spec.type());
            item.setResponseType(spec.responseType());
            item.setCategory(spec.category());
            item.setDisplayOrder(displayOrder++);
            if (spec.type() == ChecklistItemType.QUESTION) item.setQuestionOrder(questionOrder++);
            item.setChecklist(checklist);
            checklist.addItem(item);
        }
        return checklist;
    }

    private static boolean sameItems(List<ChecklistItem> existing, List<ItemSpec> specs) {
        if (existing.size() != specs.size()) return false;
        for (int i = 0; i < specs.size(); i++) {
            ChecklistItem item = existing.get(i);
            ItemSpec spec = specs.get(i);
            if (item.getItemType() != spec.type() || item.getResponseType() != spec.responseType()
                    || item.getCategory() != spec.category() || !spec.text().equals(item.getText())) {
                return false;
            }
        }
        return true;
    }

    /** Friendly redirect when a developer uploads an SOP larger than the multipart limit. */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public String handleMaxUploadSize(RedirectAttributes redirectAttributes) {
        redirectAttributes.addAttribute("error", "SOP file is too large. Maximum size is 10 MB.");
        return "redirect:/developer/standard-tasks/new";
    }
}
