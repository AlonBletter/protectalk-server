package com.protectalk.usermanagment.controller;

import com.google.firebase.auth.*;
import com.protectalk.usermanagment.dto.UserRequest;
import com.protectalk.usermanagment.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RequestMapping("/users")
@RestController
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/auth/validate-token")
    public ResponseEntity<?> validateToken(@RequestBody Map<String, String> body) {
        String idToken = body.get("idToken");
        if (idToken == null || idToken.isEmpty()) {
            return ResponseEntity.badRequest().body("idToken must be provided");
        }
        try {
            FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(idToken);
            String uid = decodedToken.getUid();
            String phoneNumber = decodedToken.getClaims().get("phone_number") != null
                    ? decodedToken.getClaims().get("phone_number").toString()
                    : null;

            return ResponseEntity.ok(Map.of(
                    "uid", uid,
                    "phoneNumber", phoneNumber
            ));
        } catch (FirebaseAuthException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid ID token");
        }
    }

    @GetMapping("/list")
    public List<Map<String, String>> listUsers() throws Exception {
        List<Map<String, String>> result = new ArrayList<>();

        ListUsersPage page = FirebaseAuth.getInstance().listUsers(null);
        while (page != null) {
            for (ExportedUserRecord user : page.getValues()) {
                Map<String, String> userInfo = new HashMap<>();
                userInfo.put("uid", user.getUid());
                userInfo.put("phoneNumber", user.getPhoneNumber());
                result.add(userInfo);
            }
            page = page.getNextPage();
        }

        return result;
    }

    @PostMapping("/register")
    public ResponseEntity<String> registerUser(@RequestBody UserRequest request) {
        try {
            String uid = userService.createUser(request);
            return ResponseEntity.ok("User created with UID: " + uid);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error: " + e.getMessage());
        }
    }
}