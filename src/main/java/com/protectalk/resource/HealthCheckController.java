package com.protectalk.resource;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthCheckController {
    @GetMapping("/hello")
    public String sayHello() {
        return "Hello, world!";
    }

    @GetMapping("/test")
    public String test() {
        return "test!";
    }}


