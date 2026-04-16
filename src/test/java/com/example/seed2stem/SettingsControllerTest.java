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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SettingsControllerTest {

    @Mock
    private AuthService authService;

    @InjectMocks
    private SettingsController controller;

    private MockHttpSession session;
    private User testUser;

    @BeforeEach
    void setUp() {
        session = new MockHttpSession();
        testUser = new User("john", "hashed", "John", "Doe", AccountType.TECHNICIAN);
        testUser.setId(1L);
    }

    // --- settings GET ---

    @Test
    void settings_noUser_redirectsToLogin() {
        Model model = new ConcurrentModel();
        String result = controller.settings(session, model, null, null);
        assertEquals("redirect:/auth/login", result);
    }

    @Test
    void settings_loggedIn_returnsSettingsViewWithUser() {
        session.setAttribute("loggedInUser", testUser);
        Model model = new ConcurrentModel();

        String result = controller.settings(session, model, null, null);

        assertEquals("settings", result);
        assertEquals(testUser, model.getAttribute("user"));
    }

    @Test
    void settings_withError_addsErrorToModel() {
        session.setAttribute("loggedInUser", testUser);
        Model model = new ConcurrentModel();

        controller.settings(session, model, "Something went wrong", null);

        assertEquals("Something went wrong", model.getAttribute("error"));
    }

    @Test
    void settings_withSuccess_addsSuccessToModel() {
        session.setAttribute("loggedInUser", testUser);
        Model model = new ConcurrentModel();

        controller.settings(session, model, null, "Password changed successfully");

        assertEquals("Password changed successfully", model.getAttribute("success"));
    }

    // --- changePassword POST ---

    @Test
    void changePassword_noUser_redirectsToLogin() {
        RedirectAttributes redirect = new RedirectAttributesModelMap();

        String result = controller.changePassword("current", "new", "new", session, redirect);

        assertEquals("redirect:/auth/login", result);
        verify(authService, never()).changeOwnPassword(anyLong(), anyString(), anyString());
    }

    @Test
    void changePassword_mismatchedNewPasswords_redirectsToSettingsWithError() {
        session.setAttribute("loggedInUser", testUser);
        RedirectAttributes redirect = new RedirectAttributesModelMap();

        String result = controller.changePassword("current", "new1", "new2", session, redirect);

        assertEquals("redirect:/settings", result);
        verify(authService, never()).changeOwnPassword(anyLong(), anyString(), anyString());
    }

    @Test
    void changePassword_validInput_callsServiceAndRedirects() {
        session.setAttribute("loggedInUser", testUser);
        doNothing().when(authService).changeOwnPassword(1L, "current", "newpass");
        RedirectAttributes redirect = new RedirectAttributesModelMap();

        String result = controller.changePassword("current", "newpass", "newpass", session, redirect);

        assertEquals("redirect:/settings", result);
        verify(authService).changeOwnPassword(1L, "current", "newpass");
    }

    @Test
    void changePassword_wrongCurrent_redirectsWithError() {
        session.setAttribute("loggedInUser", testUser);
        doThrow(new RuntimeException("Current password is incorrect"))
                .when(authService).changeOwnPassword(1L, "wrong", "newpass");
        RedirectAttributes redirect = new RedirectAttributesModelMap();

        String result = controller.changePassword("wrong", "newpass", "newpass", session, redirect);

        assertEquals("redirect:/settings", result);
    }
}
