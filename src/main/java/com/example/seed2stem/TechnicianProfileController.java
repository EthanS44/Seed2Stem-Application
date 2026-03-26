package com.example.seed2stem;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/technicians")
public class TechnicianProfileController {

    private final UserRepository userRepository;
    private final ChecklistRunRepository checklistRunRepository;

    public TechnicianProfileController(UserRepository userRepository,
                                       ChecklistRunRepository checklistRunRepository) {
        this.userRepository = userRepository;
        this.checklistRunRepository = checklistRunRepository;
    }

    @GetMapping
    public String listTechnicians(HttpSession session, Model model) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null) return "redirect:/auth/login";
        if (user.getAccountType() != AccountType.MANAGER) return "redirect:/dashboard/home-dashboard";

        List<User> technicians = userRepository.findByAccountType(AccountType.TECHNICIAN);
        model.addAttribute("technicians", technicians);
        return "technician-list";
    }

    @GetMapping("/{id}")
    public String technicianProfile(@PathVariable Long id, HttpSession session, Model model) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null) return "redirect:/auth/login";
        if (user.getAccountType() != AccountType.MANAGER) return "redirect:/dashboard/home-dashboard";

        User technician = userRepository.findById(id).orElse(null);
        if (technician == null) return "redirect:/technicians";

        List<ChecklistRun> completedRuns = checklistRunRepository.findCompletedByUserOrderByStartTimeDesc(technician);

        model.addAttribute("technician", technician);
        model.addAttribute("completedRuns", completedRuns);
        return "technician-profile";
    }
}
