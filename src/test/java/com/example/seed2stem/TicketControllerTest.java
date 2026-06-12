package com.example.seed2stem;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TicketControllerTest {

    @Mock private TicketRepository ticketRepo;

    @InjectMocks
    private TicketController controller;

    private MockHttpSession session;
    private User techUser;
    private User developerUser;

    @BeforeEach
    void setUp() {
        session = new MockHttpSession();
        techUser = new User("t@s2s.com", "h", "Tech", "User", AccountType.TECHNICIAN);
        techUser.setId(1L);
        developerUser = new User("d@s2s.com", "h", "Dev", "User", AccountType.DEVELOPER);
        developerUser.setId(2L);
    }

    // --- new ticket form ---

    @Test
    void newTicketForm_noUser_redirectsToLogin() {
        Model model = new ConcurrentModel();
        assertEquals("redirect:/auth/login", controller.newTicketForm(session, model, null));
    }

    @Test
    void newTicketForm_loggedIn_returnsForm() {
        session.setAttribute("loggedInUser", techUser);
        Model model = new ConcurrentModel();
        assertEquals("ticket-new", controller.newTicketForm(session, model, null));
    }

    // --- submit ---

    @Test
    void submitTicket_noUser_redirectsToLogin() {
        var ra = new RedirectAttributesModelMap();
        assertEquals("redirect:/auth/login",
                controller.submitTicket("title", "desc", session, ra));
        verify(ticketRepo, never()).save(any());
    }

    @Test
    void submitTicket_blankTitle_redirectsBackWithError() {
        session.setAttribute("loggedInUser", techUser);
        var ra = new RedirectAttributesModelMap();
        assertEquals("redirect:/tickets/new",
                controller.submitTicket("  ", "desc", session, ra));
        verify(ticketRepo, never()).save(any());
    }

    @Test
    void submitTicket_blankDescription_redirectsBackWithError() {
        session.setAttribute("loggedInUser", techUser);
        var ra = new RedirectAttributesModelMap();
        assertEquals("redirect:/tickets/new",
                controller.submitTicket("title", "", session, ra));
        verify(ticketRepo, never()).save(any());
    }

    @Test
    void submitTicket_valid_savesAndRedirectsToSubmitted() {
        session.setAttribute("loggedInUser", techUser);
        var ra = new RedirectAttributesModelMap();

        String result = controller.submitTicket("Login is broken", "Clicking sign in does nothing", session, ra);

        assertEquals("redirect:/tickets/submitted", result);
        verify(ticketRepo).save(argThat(t ->
                "Login is broken".equals(t.getTitle())
                        && "Clicking sign in does nothing".equals(t.getDescription())
                        && t.getSubmittedBy() == techUser
                        && t.getStatus() == TicketStatus.OPEN
                        && t.getSubmittedAt() != null));
    }

    @Test
    void submitTicket_trimsTitleAndDescription() {
        session.setAttribute("loggedInUser", techUser);
        var ra = new RedirectAttributesModelMap();

        controller.submitTicket("  hello  ", "  world\n  ", session, ra);

        verify(ticketRepo).save(argThat(t ->
                "hello".equals(t.getTitle()) && "world".equals(t.getDescription())));
    }

    // --- developer queue ---

    @Test
    void developerTickets_noUser_redirectsToLogin() {
        Model model = new ConcurrentModel();
        assertEquals("redirect:/auth/login",
                controller.developerTickets(session, model, false, null, null));
    }

    @Test
    void developerTickets_technician_redirectsToHome() {
        session.setAttribute("loggedInUser", techUser);
        Model model = new ConcurrentModel();
        assertEquals("redirect:/dashboard/home-dashboard",
                controller.developerTickets(session, model, false, null, null));
    }

    @Test
    void developerTickets_developer_openTickets_populatesModel() {
        session.setAttribute("loggedInUser", developerUser);
        Ticket ticket = new Ticket();
        ticket.setId(7L);
        ticket.setTitle("Crash");
        when(ticketRepo.findByStatusOrderBySubmittedAtAsc(TicketStatus.OPEN))
                .thenReturn(List.of(ticket));
        when(ticketRepo.countByStatus(TicketStatus.OPEN)).thenReturn(1L);
        when(ticketRepo.countByStatus(TicketStatus.RESOLVED)).thenReturn(0L);

        Model model = new ConcurrentModel();
        String result = controller.developerTickets(session, model, false, null, null);

        assertEquals("developer-tickets", result);
        assertNotNull(model.getAttribute("tickets"));
        assertEquals(1L, model.getAttribute("openCount"));
        assertEquals(false, model.getAttribute("showResolved"));
    }

    @Test
    void developerTickets_developer_resolvedFilter_usesResolvedQuery() {
        session.setAttribute("loggedInUser", developerUser);
        when(ticketRepo.findByStatusOrderBySubmittedAtAsc(TicketStatus.RESOLVED))
                .thenReturn(List.of());
        when(ticketRepo.countByStatus(TicketStatus.OPEN)).thenReturn(0L);
        when(ticketRepo.countByStatus(TicketStatus.RESOLVED)).thenReturn(3L);

        Model model = new ConcurrentModel();
        String result = controller.developerTickets(session, model, true, null, null);

        assertEquals("developer-tickets", result);
        assertEquals(true, model.getAttribute("showResolved"));
        verify(ticketRepo).findByStatusOrderBySubmittedAtAsc(TicketStatus.RESOLVED);
        verify(ticketRepo, never()).findByStatusOrderBySubmittedAtAsc(TicketStatus.OPEN);
    }

    // --- resolve ---

    @Test
    void resolveTicket_noUser_redirectsToLogin() {
        var ra = new RedirectAttributesModelMap();
        assertEquals("redirect:/auth/login", controller.resolveTicket(1L, session, ra));
        verify(ticketRepo, never()).save(any());
    }

    @Test
    void resolveTicket_technician_redirectsToHome() {
        session.setAttribute("loggedInUser", techUser);
        var ra = new RedirectAttributesModelMap();
        assertEquals("redirect:/dashboard/home-dashboard",
                controller.resolveTicket(1L, session, ra));
        verify(ticketRepo, never()).save(any());
    }

    @Test
    void resolveTicket_developer_flipsStatusAndTimestamps() {
        session.setAttribute("loggedInUser", developerUser);
        Ticket ticket = new Ticket();
        ticket.setId(5L);
        ticket.setStatus(TicketStatus.OPEN);
        when(ticketRepo.findById(5L)).thenReturn(Optional.of(ticket));
        var ra = new RedirectAttributesModelMap();

        String result = controller.resolveTicket(5L, session, ra);

        assertEquals("redirect:/developer/tickets", result);
        verify(ticketRepo).save(argThat(t ->
                t.getStatus() == TicketStatus.RESOLVED && t.getResolvedAt() != null));
    }

    @Test
    void resolveTicket_missing_redirectsWithError() {
        session.setAttribute("loggedInUser", developerUser);
        when(ticketRepo.findById(999L)).thenReturn(Optional.empty());
        var ra = new RedirectAttributesModelMap();

        String result = controller.resolveTicket(999L, session, ra);

        assertEquals("redirect:/developer/tickets", result);
        verify(ticketRepo, never()).save(any());
    }

    // --- reopen ---

    @Test
    void reopenTicket_developer_flipsBackToOpenAndClearsResolvedAt() {
        session.setAttribute("loggedInUser", developerUser);
        Ticket ticket = new Ticket();
        ticket.setId(5L);
        ticket.setStatus(TicketStatus.RESOLVED);
        ticket.setResolvedAt(java.time.LocalDateTime.now());
        when(ticketRepo.findById(5L)).thenReturn(Optional.of(ticket));
        var ra = new RedirectAttributesModelMap();

        String result = controller.reopenTicket(5L, session, ra);

        assertEquals("redirect:/developer/tickets?showResolved=true", result);
        verify(ticketRepo).save(argThat(t ->
                t.getStatus() == TicketStatus.OPEN && t.getResolvedAt() == null));
    }
}
