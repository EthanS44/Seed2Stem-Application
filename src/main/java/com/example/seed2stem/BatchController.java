package com.example.seed2stem;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;

@Controller
@RequestMapping("/batches")
public class BatchController {

    private final BatchService batchService;

    public BatchController(BatchService batchService) {
        this.batchService = batchService;
    }

    @GetMapping
    public String listBatches(HttpSession session, Model model) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null) return "redirect:/auth/login";

        model.addAttribute("batches", batchService.getAllBatches());
        return "batch-list";
    }

    @GetMapping("/create")
    public String createForm(HttpSession session) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null) return "redirect:/auth/login";
        if (user.getAccountType() != AccountType.MANAGER) return "redirect:/batches";
        return "batch-create";
    }

    @PostMapping("/create")
    public String createBatch(@RequestParam String strain,
                              @RequestParam String strainAcronym,
                              @RequestParam String room,
                              @RequestParam Integer plantCount,
                              @RequestParam String startDate,
                              @RequestParam(required = false) String notes,
                              HttpSession session,
                              RedirectAttributes redirectAttributes) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null) return "redirect:/auth/login";
        if (user.getAccountType() != AccountType.MANAGER) return "redirect:/batches";

        try {
            Batch batch = batchService.createBatch(strain, strainAcronym, room,
                    plantCount, LocalDate.parse(startDate), notes, user);
            redirectAttributes.addFlashAttribute("success",
                    "Batch " + batch.getBatchCode() + " created successfully.");
            return "redirect:/batches/" + batch.getId();
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/batches/create";
        }
    }

    @GetMapping("/{id}")
    public String viewBatch(@PathVariable Long id, HttpSession session, Model model) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null) return "redirect:/auth/login";

        Batch batch = batchService.getBatchById(id);
        model.addAttribute("batch", batch);
        model.addAttribute("isManager", user.getAccountType() == AccountType.MANAGER);
        model.addAttribute("activityLog", batchService.getActivityLog(id));
        model.addAttribute("workerSessions", batchService.getWorkerSessions(id));
        return "batch-detail";
    }

    @PostMapping("/{id}/advance")
    public String advanceStatus(@PathVariable Long id, HttpSession session,
                                RedirectAttributes redirectAttributes) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null) return "redirect:/auth/login";
        if (user.getAccountType() != AccountType.MANAGER) return "redirect:/batches/" + id;

        try {
            Batch batch = batchService.advanceStatus(id, user);
            redirectAttributes.addFlashAttribute("success",
                    "Batch advanced to " + batch.getStatus().getDisplayName());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/batches/" + id;
    }

    @PostMapping("/{id}/destroy")
    public String destroyBatch(@PathVariable Long id, HttpSession session,
                               RedirectAttributes redirectAttributes) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null) return "redirect:/auth/login";
        if (user.getAccountType() != AccountType.MANAGER) return "redirect:/batches/" + id;

        batchService.markDestroyed(id, user);
        redirectAttributes.addFlashAttribute("success", "Batch marked as destroyed.");
        return "redirect:/batches/" + id;
    }

    @PostMapping("/{id}/harvest")
    public String recordHarvest(@PathVariable Long id,
                                @RequestParam String harvestDate,
                                @RequestParam Double harvestWeight,
                                @RequestParam(required = false) Double coaWeight,
                                HttpSession session,
                                RedirectAttributes redirectAttributes) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null) return "redirect:/auth/login";

        try {
            batchService.recordHarvest(id, LocalDate.parse(harvestDate), harvestWeight, coaWeight, user);
            redirectAttributes.addFlashAttribute("success", "Harvest recorded successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/batches/" + id;
    }

    @PostMapping("/{id}/worker-session")
    public String addWorkerSession(@PathVariable Long id,
                                   @RequestParam String sessionType,
                                   @RequestParam String workerName,
                                   @RequestParam String sessionDate,
                                   @RequestParam(required = false) Double weightGrams,
                                   @RequestParam(required = false) Integer bagCount,
                                   @RequestParam(required = false) String bagSize,
                                   @RequestParam(required = false) Double hoursWorked,
                                   @RequestParam(required = false) String notes,
                                   HttpSession session,
                                   RedirectAttributes redirectAttributes) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null) return "redirect:/auth/login";

        try {
            batchService.addWorkerSession(id, sessionType, workerName,
                    LocalDate.parse(sessionDate), weightGrams, bagCount, bagSize,
                    hoursWorked, notes, user);
            redirectAttributes.addFlashAttribute("success", "Worker session recorded.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/batches/" + id;
    }
}
