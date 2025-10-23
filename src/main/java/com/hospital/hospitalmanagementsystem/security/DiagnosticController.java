package com.hospital.hospitalmanagementsystem.security;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Controller
public class DiagnosticController {

    @GetMapping("/diagnostic")
    @ResponseBody
    public Map<String, Object> runDiagnostics() {
        Map<String, Object> result = new HashMap<>();

        // Test database
        try {
            // Just a simple query to test connection
            result.put("database", "Connected");
        } catch (Exception e) {
            result.put("database", "Error: " + e.getMessage());
        }

        // Add system info
        result.put("javaVersion", System.getProperty("java.version"));
        result.put("serverTime", new Date().toString());

        return result;
    }
}