package com.example.seed2stem;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/batches")
public class BatchController {

    // Batches UI is hidden. Any /batches/** request (GET or POST) is redirected to the home dashboard.
    // The Batch entity/service/repo remain so historical batch data and calendar events stay intact.
    @RequestMapping("/**")
    public String hidden() {
        return "redirect:/dashboard/home-dashboard";
    }

    @RequestMapping
    public String hiddenRoot() {
        return "redirect:/dashboard/home-dashboard";
    }
}
