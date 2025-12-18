package com.example.seed2stem;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/")
    public String home() {
        // Redirect root URL to login page
        return "redirect:/auth/login";
    }
}
