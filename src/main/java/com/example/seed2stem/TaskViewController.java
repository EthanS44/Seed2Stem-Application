package com.example.seed2stem;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/tasks")
public class TaskViewController {

    private final TaskRepository taskRepo;
    private final ChecklistItemRepository itemRepo;

    public TaskViewController(TaskRepository taskRepo,
                          ChecklistItemRepository itemRepo) {
        this.taskRepo = taskRepo;
        this.itemRepo = itemRepo;
    }

    /* ---------------- Task View ---------------- */

    @GetMapping("/{taskId}")
    public String viewTask(@PathVariable Long taskId, Model model) {

        Task task = taskRepo.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found"));

        model.addAttribute("task", task);
        return "task-view";
    }

    /* -------------- Checklist View -------------- */

    @GetMapping("/{taskId}/start")
    public String startTask(@PathVariable Long taskId, Model model) {

        Task task = taskRepo.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found"));

        Checklist checklist = task.getChecklist();

        model.addAttribute("task", task);
        model.addAttribute("checklist", checklist);
        model.addAttribute(
                "items",
                itemRepo.findByChecklistIdOrderByItemOrder(checklist.getId())
        );

        return "checklist-view";
    }
}
