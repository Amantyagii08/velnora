package com.ruralmarketplace.controller;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;

@RestController 
public class HealthController {
    @GetMapping ("/api/health")
  public String healthCheck(){
    return "Rural Marketplace is working";
    }
}
