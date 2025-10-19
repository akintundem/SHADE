package ai.eventplanner.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
public class TestController {

    @GetMapping("/")
    public Map<String, String> home() {
        Map<String, String> response = new HashMap<>();
        response.put("message", "Event Planner Monolith is running!");
        response.put("status", "UP");
        response.put("port", "8080");
        return response;
    }

    // Health endpoint moved to AuthController at /api/v1/auth/health
}
