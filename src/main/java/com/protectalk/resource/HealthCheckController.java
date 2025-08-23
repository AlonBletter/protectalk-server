// 6. Enhanced Controller with Database Operations
package com.protectalk.resource;

import com.protectalk.usermanagment.model.UserEntity;
import com.protectalk.usermanagment.repo.UserRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class HealthCheckController {
    private final UserRepository userRepository;

    public HealthCheckController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/hello")
    public String sayHello() {
        return "Hello, world!";
    }

    // Test MongoDB connection
    @GetMapping("/db-test")
    public String testDatabase() {
        try {
            List<UserEntity> records = userRepository.findAll();
            return "✅ Database connected! Total records: " + records;
        } catch (Exception e) {
            return "❌ Database error: " + e.getMessage();
        }
    }
}