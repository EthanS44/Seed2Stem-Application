package com.example.seed2stem;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/dashboard")
public class DashboardController {

    @GetMapping("/home-dashboard")
    public String homeDashboard(HttpSession session) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user.getAccountType() == AccountType.MANAGER) {
            return "redirect:/dashboard/manager-dashboard";
        } else {
            return "redirect:/dashboard/technician-dashboard";
        }
    }

    @GetMapping("/technician-dashboard")
    public String technicianDashboard(){
        return  "technician-dashboard";
    }

    @GetMapping("/manager-dashboard")
    public String managerDashboard(){
        return  "manager-dashboard";
    }

    @GetMapping("/task-dashboard")
    public String taskDashboard(HttpSession session) {
        User user = (User) session.getAttribute("loggedInUser");

        if (user == null) {
            return "redirect:/auth/login";
        }
        if (user.getAccountType() == AccountType.MANAGER) {
            return "redirect:/dashboard/manager-task-dashboard";
        } else {
            return "redirect:/dashboard/technician-task-dashboard";
        }
    }

    @GetMapping("/technician-task-dashboard")
    public String technicianTaskDashboard(){
        return  "technician-task-dashboard";
    }

    @GetMapping("manager-task-dashboard")
    public String managerTaskDashboard(){
        return  "manager-task-dashboard";
    }

}
