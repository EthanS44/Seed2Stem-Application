package com.example.seed2stem;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class HomeControllerTest {

    @Test
    void home_redirectsToLogin() {
        HomeController controller = new HomeController();
        String result = controller.home();
        assertEquals("redirect:/auth/login", result);
    }
}
