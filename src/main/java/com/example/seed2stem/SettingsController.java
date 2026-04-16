package com.example.seed2stem;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/settings")
public class SettingsController {

    private final AuthService authService;

    public SettingsController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping
    public String settings(HttpSession session, Model model,
                           @RequestParam(value = "error", required = false) String error,
                           @RequestParam(value = "success", required = false) String success) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null) return "redirect:/auth/login";

        model.addAttribute("user", user);
        model.addAttribute("error", error);
        model.addAttribute("success", success);
        return "settings";
    }

    @PostMapping("/change-password")
    public String changePassword(@RequestParam String currentPassword,
                                 @RequestParam String newPassword,
                                 @RequestParam String confirmPassword,
                                 HttpSession session,
                                 RedirectAttributes redirectAttributes) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null) return "redirect:/auth/login";

        if (!newPassword.equals(confirmPassword)) {
            redirectAttributes.addAttribute("error", "New passwords do not match");
            return "redirect:/settings";
        }

        try {
            authService.changeOwnPassword(user.getId(), currentPassword, newPassword);
            redirectAttributes.addAttribute("success", "Password changed successfully");
        } catch (RuntimeException e) {
            redirectAttributes.addAttribute("error", e.getMessage());
        }
        return "redirect:/settings";
    }
}
