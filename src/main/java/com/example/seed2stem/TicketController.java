package com.example.seed2stem;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;

/**
 * Bug / issue ticketing. Any logged-in user can submit a ticket; only
 * developers see the queue. Submitters get a confirmation screen on
 * submission and never look back — there's intentionally no "my tickets"
 * page (per design decision, the developer is the only audience).
 */
@Controller
public class TicketController {

    private final TicketRepository ticketRepo;

    public TicketController(TicketRepository ticketRepo) {
        this.ticketRepo = ticketRepo;
    }

    /* ---------------- Submit (any logged-in user) ---------------- */

    @GetMapping("/tickets/new")
    public String newTicketForm(HttpSession session, Model model,
                                @RequestParam(value = "error", required = false) String error) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null) return "redirect:/auth/login";
        model.addAttribute("error", error);
        return "ticket-new";
    }

    @PostMapping("/tickets")
    public String submitTicket(@RequestParam String title,
                               @RequestParam String description,
                               HttpSession session,
                               RedirectAttributes redirectAttributes) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null) return "redirect:/auth/login";

        if (title == null || title.trim().isEmpty()) {
            redirectAttributes.addAttribute("error", "Title is required");
            return "redirect:/tickets/new";
        }
        if (description == null || description.trim().isEmpty()) {
            redirectAttributes.addAttribute("error", "Description is required");
            return "redirect:/tickets/new";
        }

        Ticket ticket = new Ticket();
        ticket.setTitle(title.trim());
        ticket.setDescription(description.trim());
        ticket.setSubmittedBy(user);
        ticket.setSubmittedAt(LocalDateTime.now());
        ticket.setStatus(TicketStatus.OPEN);
        ticketRepo.save(ticket);

        return "redirect:/tickets/submitted";
    }

    @GetMapping("/tickets/submitted")
    public String submitted(HttpSession session) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null) return "redirect:/auth/login";
        return "ticket-submitted";
    }

    /* ---------------- Developer queue ---------------- */

    @GetMapping("/developer/tickets")
    public String developerTickets(HttpSession session, Model model,
                                   @RequestParam(value = "showResolved", required = false,
                                                 defaultValue = "false") boolean showResolved,
                                   @RequestParam(value = "success", required = false) String success,
                                   @RequestParam(value = "error", required = false) String error) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null) return "redirect:/auth/login";
        if (user.getAccountType() != AccountType.DEVELOPER) {
            return "redirect:/dashboard/home-dashboard";
        }

        if (showResolved) {
            model.addAttribute("tickets",
                    ticketRepo.findByStatusOrderBySubmittedAtAsc(TicketStatus.RESOLVED));
        } else {
            model.addAttribute("tickets",
                    ticketRepo.findByStatusOrderBySubmittedAtAsc(TicketStatus.OPEN));
        }
        model.addAttribute("openCount", ticketRepo.countByStatus(TicketStatus.OPEN));
        model.addAttribute("resolvedCount", ticketRepo.countByStatus(TicketStatus.RESOLVED));
        model.addAttribute("showResolved", showResolved);
        model.addAttribute("success", success);
        model.addAttribute("error", error);
        return "developer-tickets";
    }

    @PostMapping("/developer/tickets/{id}/resolve")
    public String resolveTicket(@PathVariable Long id, HttpSession session,
                                RedirectAttributes redirectAttributes) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null) return "redirect:/auth/login";
        if (user.getAccountType() != AccountType.DEVELOPER) {
            return "redirect:/dashboard/home-dashboard";
        }

        Ticket ticket = ticketRepo.findById(id).orElse(null);
        if (ticket == null) {
            redirectAttributes.addAttribute("error", "Ticket not found");
        } else {
            ticket.setStatus(TicketStatus.RESOLVED);
            ticket.setResolvedAt(LocalDateTime.now());
            ticketRepo.save(ticket);
            redirectAttributes.addAttribute("success", "Ticket marked resolved");
        }
        return "redirect:/developer/tickets";
    }

    @PostMapping("/developer/tickets/{id}/reopen")
    public String reopenTicket(@PathVariable Long id, HttpSession session,
                               RedirectAttributes redirectAttributes) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null) return "redirect:/auth/login";
        if (user.getAccountType() != AccountType.DEVELOPER) {
            return "redirect:/dashboard/home-dashboard";
        }

        Ticket ticket = ticketRepo.findById(id).orElse(null);
        if (ticket == null) {
            redirectAttributes.addAttribute("error", "Ticket not found");
        } else {
            ticket.setStatus(TicketStatus.OPEN);
            ticket.setResolvedAt(null);
            ticketRepo.save(ticket);
            redirectAttributes.addAttribute("success", "Ticket reopened");
        }
        return "redirect:/developer/tickets?showResolved=true";
    }
}
