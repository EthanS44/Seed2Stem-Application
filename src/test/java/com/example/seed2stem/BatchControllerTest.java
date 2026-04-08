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

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BatchControllerTest {

    @Mock
    private BatchService batchService;

    @InjectMocks
    private BatchController controller;

    private MockHttpSession session;
    private User managerUser;
    private User techUser;
    private Batch testBatch;

    @BeforeEach
    void setUp() {
        session = new MockHttpSession();
        managerUser = new User("mgr", "hashed", "Manager", "User", AccountType.MANAGER);
        managerUser.setId(1L);
        techUser = new User("tech", "hashed", "Tech", "User", AccountType.TECHNICIAN);
        techUser.setId(2L);

        testBatch = new Batch();
        testBatch.setId(1L);
        testBatch.setBatchCode("26-074-BLD-B1-316-01");
        testBatch.setStatus(BatchStatus.CLONING);
    }

    // --- listBatches ---

    @Test
    void listBatches_noUser_redirectsToLogin() {
        Model model = new ConcurrentModel();
        assertEquals("redirect:/auth/login", controller.listBatches(session, model));
    }

    @Test
    void listBatches_loggedIn_returnsBatchList() {
        session.setAttribute("loggedInUser", techUser);
        Model model = new ConcurrentModel();
        when(batchService.getAllBatches()).thenReturn(List.of(testBatch));

        String result = controller.listBatches(session, model);

        assertEquals("batch-list", result);
        assertNotNull(model.getAttribute("batches"));
    }

    // --- createForm ---

    @Test
    void createForm_noUser_redirectsToLogin() {
        assertEquals("redirect:/auth/login", controller.createForm(session));
    }

    @Test
    void createForm_technician_redirectsToBatches() {
        session.setAttribute("loggedInUser", techUser);
        assertEquals("redirect:/batches", controller.createForm(session));
    }

    @Test
    void createForm_manager_returnsBatchCreateView() {
        session.setAttribute("loggedInUser", managerUser);
        assertEquals("batch-create", controller.createForm(session));
    }

    // --- createBatch POST ---

    @Test
    void createBatch_noUser_redirectsToLogin() {
        var redirect = new RedirectAttributesModelMap();
        String result = controller.createBatch("strain", "STR", "B1", 50, "2026-03-15", null, session, redirect);
        assertEquals("redirect:/auth/login", result);
    }

    @Test
    void createBatch_technician_redirectsToBatches() {
        session.setAttribute("loggedInUser", techUser);
        var redirect = new RedirectAttributesModelMap();
        String result = controller.createBatch("strain", "STR", "B1", 50, "2026-03-15", null, session, redirect);
        assertEquals("redirect:/batches", result);
    }

    @Test
    void createBatch_manager_createsBatchAndRedirects() {
        session.setAttribute("loggedInUser", managerUser);
        testBatch.setId(5L);
        when(batchService.createBatch(eq("Blue Dream"), eq("BLD"), eq("B1"),
                eq(50), eq(LocalDate.of(2026, 3, 15)), isNull(), eq(managerUser)))
                .thenReturn(testBatch);
        var redirect = new RedirectAttributesModelMap();

        String result = controller.createBatch("Blue Dream", "BLD", "B1", 50, "2026-03-15", null, session, redirect);

        assertEquals("redirect:/batches/5", result);
    }

    // --- viewBatch ---

    @Test
    void viewBatch_noUser_redirectsToLogin() {
        Model model = new ConcurrentModel();
        assertEquals("redirect:/auth/login", controller.viewBatch(1L, session, model));
    }

    @Test
    void viewBatch_loggedIn_populatesModel() {
        session.setAttribute("loggedInUser", managerUser);
        Model model = new ConcurrentModel();
        when(batchService.getBatchById(1L)).thenReturn(testBatch);
        when(batchService.getActivityLog(1L)).thenReturn(List.of());
        when(batchService.getWorkerSessions(1L)).thenReturn(List.of());

        String result = controller.viewBatch(1L, session, model);

        assertEquals("batch-detail", result);
        assertEquals(testBatch, model.getAttribute("batch"));
        assertEquals(true, model.getAttribute("isManager"));
    }

    @Test
    void viewBatch_technician_isManagerFalse() {
        session.setAttribute("loggedInUser", techUser);
        Model model = new ConcurrentModel();
        when(batchService.getBatchById(1L)).thenReturn(testBatch);
        when(batchService.getActivityLog(1L)).thenReturn(List.of());
        when(batchService.getWorkerSessions(1L)).thenReturn(List.of());

        controller.viewBatch(1L, session, model);

        assertEquals(false, model.getAttribute("isManager"));
    }

    // --- advanceStatus ---

    @Test
    void advanceStatus_noUser_redirectsToLogin() {
        var redirect = new RedirectAttributesModelMap();
        assertEquals("redirect:/auth/login", controller.advanceStatus(1L, session, redirect));
    }

    @Test
    void advanceStatus_technician_redirectsToBatchDetail() {
        session.setAttribute("loggedInUser", techUser);
        var redirect = new RedirectAttributesModelMap();
        assertEquals("redirect:/batches/1", controller.advanceStatus(1L, session, redirect));
    }

    @Test
    void advanceStatus_manager_advancesAndRedirects() {
        session.setAttribute("loggedInUser", managerUser);
        testBatch.setStatus(BatchStatus.VEGETATIVE);
        when(batchService.advanceStatus(1L, managerUser)).thenReturn(testBatch);
        var redirect = new RedirectAttributesModelMap();

        String result = controller.advanceStatus(1L, session, redirect);

        assertEquals("redirect:/batches/1", result);
        verify(batchService).advanceStatus(1L, managerUser);
    }

    // --- destroyBatch ---

    @Test
    void destroyBatch_noUser_redirectsToLogin() {
        var redirect = new RedirectAttributesModelMap();
        assertEquals("redirect:/auth/login", controller.destroyBatch(1L, session, redirect));
    }

    @Test
    void destroyBatch_technician_redirectsToBatchDetail() {
        session.setAttribute("loggedInUser", techUser);
        var redirect = new RedirectAttributesModelMap();
        assertEquals("redirect:/batches/1", controller.destroyBatch(1L, session, redirect));
    }

    @Test
    void destroyBatch_manager_destroysAndRedirects() {
        session.setAttribute("loggedInUser", managerUser);
        var redirect = new RedirectAttributesModelMap();

        String result = controller.destroyBatch(1L, session, redirect);

        assertEquals("redirect:/batches/1", result);
        verify(batchService).markDestroyed(1L, managerUser);
    }

    // --- recordHarvest ---

    @Test
    void recordHarvest_noUser_redirectsToLogin() {
        var redirect = new RedirectAttributesModelMap();
        assertEquals("redirect:/auth/login",
                controller.recordHarvest(1L, "2026-06-01", 500.0, null, session, redirect));
    }

    @Test
    void recordHarvest_validInput_recordsAndRedirects() {
        session.setAttribute("loggedInUser", managerUser);
        var redirect = new RedirectAttributesModelMap();

        String result = controller.recordHarvest(1L, "2026-06-01", 500.0, 10.0, session, redirect);

        assertEquals("redirect:/batches/1", result);
        verify(batchService).recordHarvest(1L, LocalDate.of(2026, 6, 1), 500.0, 10.0, managerUser);
    }
}
