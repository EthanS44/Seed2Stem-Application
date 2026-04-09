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
class AuthControllerTest {

    @Mock
    private AuthService authService;

    @InjectMocks
    private AuthController controller;

    private MockHttpSession session;

    @BeforeEach
    void setUp() {
        session = new MockHttpSession();
    }

    // --- loginPage ---

    @Test
    void loginPage_returnsLoginView() {
        Model model = new ConcurrentModel();
        String result = controller.loginPage(null, null, model);
        assertEquals("login", result);
    }

    @Test
    void loginPage_withError_addsErrorToModel() {
        Model model = new ConcurrentModel();
        controller.loginPage("Invalid credentials", null, model);
        assertEquals("Invalid credentials", model.getAttribute("error"));
    }

    @Test
    void loginPage_withSuccess_addsSuccessToModel() {
        Model model = new ConcurrentModel();
        controller.loginPage(null, "Account created", model);
        assertEquals("Account created", model.getAttribute("success"));
    }

    // --- login POST ---

    @Test
    void login_validCredentials_redirectsToDashboard() {
        User user = new User("john", "hashed", "John", "Doe", AccountType.TECHNICIAN);
        when(authService.login("john", "pass")).thenReturn(user);
        RedirectAttributes redirect = new RedirectAttributesModelMap();

        String result = controller.login("john", "pass", session, redirect);

        assertEquals("redirect:/dashboard/home-dashboard", result);
        assertEquals(user, session.getAttribute("loggedInUser"));
    }

    @Test
    void login_invalidCredentials_redirectsToLoginWithError() {
        when(authService.login("john", "wrong")).thenThrow(new RuntimeException("Invalid username or password"));
        RedirectAttributes redirect = new RedirectAttributesModelMap();

        String result = controller.login("john", "wrong", session, redirect);

        assertEquals("redirect:/auth/login", result);
        assertNull(session.getAttribute("loggedInUser"));
    }

    // --- registerPage ---

    @Test
    void registerPage_returnsRegisterView() {
        Model model = new ConcurrentModel();
        String result = controller.registerPage(null, model);
        assertEquals("register", result);
    }

    // --- register POST ---

    @Test
    void register_validInput_redirectsToRegistrationPending() {
        doNothing().when(authService).register("newuser", "pass", "Jane", "Smith", AccountType.MANAGER);
        RedirectAttributes redirect = new RedirectAttributesModelMap();

        String result = controller.register("newuser", "pass", "Jane", "Smith", "MANAGER", redirect);

        assertEquals("redirect:/auth/registration-pending", result);
    }

    @Test
    void registrationPending_returnsCorrectView() {
        String result = controller.registrationPending();
        assertEquals("registration-pending", result);
    }

    @Test
    void register_invalidAccountType_redirectsToRegisterWithError() {
        RedirectAttributes redirect = new RedirectAttributesModelMap();

        String result = controller.register("newuser", "pass", "Jane", "Smith", "INVALID_TYPE", redirect);

        assertEquals("redirect:/auth/register", result);
    }

    @Test
    void register_duplicateUsername_redirectsToRegisterWithError() {
        doThrow(new RuntimeException("Username already exists"))
                .when(authService).register("john", "pass", "John", "Doe", AccountType.TECHNICIAN);
        RedirectAttributes redirect = new RedirectAttributesModelMap();

        String result = controller.register("john", "pass", "John", "Doe", "TECHNICIAN", redirect);

        assertEquals("redirect:/auth/register", result);
    }

    // --- logout ---

    @Test
    void logout_invalidatesSessionAndRedirects() {
        session.setAttribute("loggedInUser", new User());
        RedirectAttributes redirect = new RedirectAttributesModelMap();

        String result = controller.logout(session, redirect);

        assertEquals("redirect:/auth/login", result);
        assertTrue(session.isInvalid());
    }
}
