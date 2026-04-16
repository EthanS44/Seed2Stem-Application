package com.example.seed2stem;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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

    public DeveloperController(TaskRepository taskRepo,
                               ChecklistRunRepository runRepo,
                               RegistrationRequestRepository registrationRequestRepo,
                               PasswordResetRequestRepository passwordResetRequestRepo,
                               AuthService authService) {
        this.taskRepo = taskRepo;
        this.runRepo = runRepo;
        this.registrationRequestRepo = registrationRequestRepo;
        this.passwordResetRequestRepo = passwordResetRequestRepo;
        this.authService = authService;
    }

    @GetMapping("/user-created-tasks")
    public String userCreatedTasks(HttpSession session, Model model) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null) return "redirect:/auth/login";
        if (user.getAccountType() != AccountType.DEVELOPER) {
            return "redirect:/dashboard/home-dashboard";
        }

        List<Task> tasks = taskRepo.findByUserCreatedTrue();
        Map<Long, Long> taskRunMap = new HashMap<>();
        for (Task task : tasks) {
            runRepo.findFirstByTask(task).ifPresent(run ->
                    taskRunMap.put(task.getId(), run.getId()));
        }

        model.addAttribute("userCreatedTasks", tasks);
        model.addAttribute("taskRunMap", taskRunMap);
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
    public String approveRegistration(@PathVariable Long id, HttpSession session) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null) return "redirect:/auth/login";
        if (user.getAccountType() != AccountType.DEVELOPER) {
            return "redirect:/dashboard/home-dashboard";
        }

        authService.approveRegistration(id);
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
}
