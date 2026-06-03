package com.example.seed2stem;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpSession;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExportControllerTest {

    @Mock
    private ExcelExportService exportService;

    @InjectMocks
    private ExportController controller;

    private MockHttpSession session;
    private User managerUser;
    private User developerUser;
    private User techUser;

    @BeforeEach
    void setUp() {
        session = new MockHttpSession();
        managerUser = new User("m@s2s.com", "h", "Manager", "User", AccountType.MANAGER);
        developerUser = new User("d@s2s.com", "h", "Dev", "User", AccountType.DEVELOPER);
        techUser = new User("t@s2s.com", "h", "Tech", "User", AccountType.TECHNICIAN);
    }

    @Test
    void exportAllData_noUser_returns401() {
        ResponseEntity<byte[]> resp = controller.exportAllData(session);
        assertEquals(401, resp.getStatusCode().value());
        verify(exportService, never()).buildWorkbook();
    }

    @Test
    void exportAllData_technician_returns403() {
        session.setAttribute("loggedInUser", techUser);
        ResponseEntity<byte[]> resp = controller.exportAllData(session);
        assertEquals(403, resp.getStatusCode().value());
        verify(exportService, never()).buildWorkbook();
    }

    @Test
    void exportAllData_manager_returns403() {
        session.setAttribute("loggedInUser", managerUser);
        ResponseEntity<byte[]> resp = controller.exportAllData(session);
        assertEquals(403, resp.getStatusCode().value());
        verify(exportService, never()).buildWorkbook();
    }

    @Test
    void exportAllData_developer_returnsXlsxBytesAsAttachment() {
        session.setAttribute("loggedInUser", developerUser);
        byte[] dummy = {1, 2, 3, 4};
        when(exportService.buildWorkbook()).thenReturn(dummy);

        ResponseEntity<byte[]> resp = controller.exportAllData(session);

        assertEquals(200, resp.getStatusCode().value());
        assertArrayEquals(dummy, resp.getBody());
        assertEquals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                resp.getHeaders().getContentType().toString());
        String disposition = resp.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION);
        assertNotNull(disposition);
        assertTrue(disposition.startsWith("attachment;"));
        assertTrue(disposition.contains("seed2stem-export-"));
        assertTrue(disposition.endsWith(".xlsx\""));
    }
}
