package com.ruralmarketplace.controller;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;

@RestController 
public class HealthController {
    @GetMapping ("/api/health")
  public java.util.Map<String, String> healthCheck(){
    return java.util.Map.of("status", "UP", "service", "Velnora");
    }
}
