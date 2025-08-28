package com.protectalk.usermanagment.controller;

import com.google.firebase.auth.*;
import com.protectalk.security.model.FirebasePrincipal;
import com.protectalk.usermanagment.dto.ContactRequestDto;
import com.protectalk.usermanagment.dto.CompleteRegistrationRequestDto;
import com.protectalk.usermanagment.dto.UserRequestDto;
import com.protectalk.usermanagment.model.ContactType;
import com.protectalk.usermanagment.model.ContactRequestEntity;
import com.protectalk.usermanagment.service.ContactRequestService;
import com.protectalk.usermanagment.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RequestMapping("/api/users")
@RestController
public class UserController {
    private final UserService           userService;
    private final ContactRequestService contactRequestService;

    public UserController(UserService userService, ContactRequestService contactRequestService) {
        this.userService = userService;
        this.contactRequestService = contactRequestService;
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
    public ResponseEntity<String> registerUser(@RequestBody UserRequestDto request) {
        try {
            String uid = userService.createUser(request);
            return ResponseEntity.ok("User created with UID: " + uid);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error: " + e.getMessage());
        }
    }

    @PostMapping("/contact-request")
    public ResponseEntity<String> sendContactRequest(
            @AuthenticationPrincipal FirebasePrincipal me,
            @Valid @RequestBody ContactRequestDto request) {
        try {
            contactRequestService.createRequest(me.uid(), request);
            String requestType = request.contactType() == ContactType.TRUSTED_CONTACT ?
                    "trusted contact" : "protegee";
            return ResponseEntity.ok(requestType + " request sent successfully");
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("Error: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error: " + e.getMessage());
        }
    }

    @GetMapping("/pending-requests")
    public ResponseEntity<List<ContactRequestEntity>> getPendingRequests(
            @AuthenticationPrincipal FirebasePrincipal me) {
        try {
            List<ContactRequestEntity> pendingRequests =
                    contactRequestService.getPendingRequestsForUser(me.uid());
            return ResponseEntity.ok(pendingRequests);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(null);
        }
    }

    @PostMapping("/requests/{requestId}/approve")
    public ResponseEntity<String> approveRequest(
            @AuthenticationPrincipal FirebasePrincipal me,
            @PathVariable String requestId) {
        try {
            contactRequestService.approveRequest(requestId, me.uid());
            return ResponseEntity.ok("Request approved successfully");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Error: " + e.getMessage());
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("Error: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error: " + e.getMessage());
        }
    }

    @PostMapping("/requests/{requestId}/deny")
    public ResponseEntity<String> denyRequest(
            @AuthenticationPrincipal FirebasePrincipal me,
            @PathVariable String requestId) {
        try {
            contactRequestService.denyRequest(requestId, me.uid());
            return ResponseEntity.ok("Request denied successfully");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Error: " + e.getMessage());
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("Error: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error: " + e.getMessage());
        }
    }

    @PostMapping("/profile/setup")
    public ResponseEntity<String> setupUserProfile(
            @AuthenticationPrincipal FirebasePrincipal me,
            @Valid @RequestBody UserRequestDto request) {
        try {
            userService.createOrUpdateProfile(me.uid(), request);
            return ResponseEntity.ok("Profile created successfully");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error: " + e.getMessage());
        }
    }

    @PostMapping("/complete-registration")
    public ResponseEntity<String> completeRegistration(
            @AuthenticationPrincipal FirebasePrincipal me,
            @Valid @RequestBody CompleteRegistrationRequestDto request) {
        try {
            userService.completeRegistration(me.uid(), request);
            return ResponseEntity.ok("Registration completed successfully");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error: " + e.getMessage());
        }
    }
}