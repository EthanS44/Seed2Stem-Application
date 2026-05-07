package com.example.seed2stem;

import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Map;

@Controller
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /** Show login page */
    @GetMapping("/login")
    public String loginPage(@RequestParam(value = "error", required = false) String error,
                            @RequestParam(value = "success", required = false) String success,
                            Model model) {
        model.addAttribute("error", error);
        model.addAttribute("success", success);
        return "login"; // login.html in templates
    }

    /** Login POST */
    @PostMapping("/login")
    public String login(@RequestParam String username,
                        @RequestParam String password,
                        HttpSession session,
                        RedirectAttributes redirectAttributes) {
        try {
            User user = authService.login(username, password);
            session.setAttribute("loggedInUser", user);
            return "redirect:/dashboard/home-dashboard";
        } catch (RuntimeException e) {
            redirectAttributes.addAttribute("error", e.getMessage());
            return "redirect:/auth/login";
        }
    }

    /** Show registration page */
    @GetMapping("/register")
    public String registerPage(@RequestParam(value = "error", required = false) String error,
                               Model model) {
        model.addAttribute("error", error);
        return "register";
    }

    // HANDLE REGISTER
    @PostMapping("/register")
    public String register(@RequestParam String username,
                           @RequestParam String password,
                           @RequestParam String confirmPassword,
                           @RequestParam String firstName,
                           @RequestParam String lastName,
                           RedirectAttributes redirectAttributes) {

        try {
            if (!password.equals(confirmPassword)) {
                redirectAttributes.addAttribute("error", "Passwords do not match");
                return "redirect:/auth/register";
            }

            authService.register(username, password, firstName, lastName);
            return "redirect:/auth/registration-pending";

        } catch (RuntimeException e) {
            redirectAttributes.addAttribute("error", e.getMessage());
            return "redirect:/auth/register";
        }
    }

    /** Show forgot password page */
    @GetMapping("/forgot-password")
    public String forgotPasswordPage(@RequestParam(value = "error", required = false) String error,
                                     Model model) {
        model.addAttribute("error", error);
        return "forgot-password";
    }

    /** Handle forgot password request */
    @PostMapping("/forgot-password")
    public String forgotPassword(@RequestParam String username,
                                 @RequestParam String firstName,
                                 @RequestParam String lastName,
                                 RedirectAttributes redirectAttributes) {
        try {
            authService.createPasswordResetRequest(username, firstName, lastName);
            return "redirect:/auth/forgot-password-submitted";
        } catch (RuntimeException e) {
            redirectAttributes.addAttribute("error", e.getMessage());
            return "redirect:/auth/forgot-password";
        }
    }

    /** Forgot password confirmation page */
    @GetMapping("/forgot-password-submitted")
    public String forgotPasswordSubmitted() {
        return "forgot-password-submitted";
    }

    /** Registration pending confirmation */
    @GetMapping("/registration-pending")
    public String registrationPending() {
        return "registration-pending";
    }

    /** Logout */
    @GetMapping("/logout")
    public String logout(HttpSession session, RedirectAttributes redirectAttributes) {
        session.invalidate();
        redirectAttributes.addAttribute("success", "Logged out successfully.");
        return "redirect:/auth/login";
    }
}




