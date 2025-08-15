// 6. Enhanced Controller with Database Operations
package com.protectalk.resource;

import com.protectalk.db.model.CallRecordEntity;
import com.protectalk.db.mongo.repository.CallRecordRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class HealthCheckController {

    private final CallRecordRepository callRecordRepository;

    public HealthCheckController(CallRecordRepository callRecordRepository) {
        this.callRecordRepository = callRecordRepository;
    }

    @GetMapping("/hello")
    public String sayHello() {
        return "Hello, world!";
    }

    // Test MongoDB connection
    @GetMapping("/db-test")
    public String testDatabase() {
        try {
            long count = callRecordRepository.getRecordCount();
            return "✅ Database connected! Total records: " + count;
        } catch (Exception e) {
            return "❌ Database error: " + e.getMessage();
        }
    }

    // Save a call record
    @PostMapping("/calls")
    public String saveCall(@RequestParam String userId, @RequestParam String script) {
        try {
            CallRecordEntity record = new CallRecordEntity(userId, script);
            callRecordRepository.saveCallRecord(record);
            return "✅ Call record saved for user: " + userId;
        } catch (Exception e) {
            return "❌ Error saving record: " + e.getMessage();
        }
    }

    // Get calls for a specific user
    @GetMapping("/calls/{userId}")
    public List<CallRecordEntity> getUserCalls(@PathVariable String userId) {
        return callRecordRepository.getCallRecords(userId);
    }

    // Get all call records
    @GetMapping("/calls")
    public List<CallRecordEntity> getAllCalls() {
        return callRecordRepository.getAllCallRecords();
    }

    // Add sample data for testing
    @PostMapping("/sample-data")
    public String addSampleData() {
        try {
            callRecordRepository.saveCallRecord(new CallRecordEntity("user1", "Hello, this is my first call"));
            callRecordRepository.saveCallRecord(new CallRecordEntity("user1", "Second call from user1"));
            callRecordRepository.saveCallRecord(new CallRecordEntity("user2", "Call from user2"));
            callRecordRepository.saveCallRecord(new CallRecordEntity("user3", "Emergency call script"));

            return "✅ Sample data added! 4 records created.";
        } catch (Exception e) {
            return "❌ Error adding sample data: " + e.getMessage();
        }
    }
}