package com.protectalk.usermanagment.controller;

import com.protectalk.security.model.FirebasePrincipal;
import com.protectalk.usermanagment.dto.AddContactRequestDto;
import com.protectalk.usermanagment.dto.CompleteRegistrationRequestDto;
import com.protectalk.usermanagment.dto.UserProfileResponseDto;
import com.protectalk.usermanagment.dto.UserRequestDto;
import com.protectalk.usermanagment.model.ContactType;
import com.protectalk.usermanagment.model.ContactRequestEntity;
import com.protectalk.usermanagment.service.ContactRequestService;
import com.protectalk.usermanagment.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RequestMapping("/api/users")
@RestController
@RequiredArgsConstructor
public class UserController {
    private final UserService           userService;
    private final ContactRequestService contactRequestService;

    @PostMapping("/contact-request")
    public ResponseEntity<String> sendContactRequest(
            @AuthenticationPrincipal FirebasePrincipal me,
            @Valid @RequestBody AddContactRequestDto request) {
        log.info("Contact request received from UID: {} for phone: {} as {}",
                me.uid(), request.phoneNumber(), request.contactType());
        try {
            contactRequestService.createRequest(me.uid(), request);
            String requestType = request.contactType() == ContactType.TRUSTED_CONTACT ?
                    "trusted contact" : "protegee";
            log.info("Successfully sent {} request from UID: {} to phone: {}",
                    requestType, me.uid(), request.phoneNumber());
            return ResponseEntity.ok(requestType + " request sent successfully");
        } catch (IllegalStateException e) {
            log.warn("Contact request conflict for UID: {} to phone: {} - {}",
                    me.uid(), request.phoneNumber(), e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("Error: " + e.getMessage());
        } catch (Exception e) {
            log.error("Contact request failed for UID: {} to phone: {}",
                     me.uid(), request.phoneNumber(), e);
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
        log.info("Request approval attempt for request ID: {} by UID: {}", requestId, me.uid());
        try {
            contactRequestService.approveRequest(requestId, me.uid());
            log.info("Request {} approved successfully by UID: {}", requestId, me.uid());
            return ResponseEntity.ok("Request approved successfully");
        } catch (IllegalArgumentException e) {
            log.warn("Bad request for approval - request ID: {}, UID: {} - {}", requestId, me.uid(), e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Error: " + e.getMessage());
        } catch (IllegalStateException e) {
            log.warn("Conflict during approval - request ID: {}, UID: {} - {}", requestId, me.uid(), e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("Error: " + e.getMessage());
        } catch (Exception e) {
            log.error("Failed to approve request ID: {} by UID: {}", requestId, me.uid(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error: " + e.getMessage());
        }
    }

    @PostMapping("/requests/{requestId}/deny")
    public ResponseEntity<String> denyRequest(
            @AuthenticationPrincipal FirebasePrincipal me,
            @PathVariable String requestId) {
        log.info("Request denial attempt for request ID: {} by UID: {}", requestId, me.uid());
        try {
            contactRequestService.denyRequest(requestId, me.uid());
            log.info("Request {} denied successfully by UID: {}", requestId, me.uid());
            return ResponseEntity.ok("Request denied successfully");
        } catch (IllegalArgumentException e) {
            log.warn("Bad request for denial - request ID: {}, UID: {} - {}", requestId, me.uid(), e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Error: " + e.getMessage());
        } catch (IllegalStateException e) {
            log.warn("Conflict during denial - request ID: {}, UID: {} - {}", requestId, me.uid(), e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("Error: " + e.getMessage());
        } catch (Exception e) {
            log.error("Failed to deny request ID: {} by UID: {}", requestId, me.uid(), e);
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
            @AuthenticationPrincipal FirebasePrincipal principal,
            @Valid @RequestBody CompleteRegistrationRequestDto request) {
        log.info("Complete registration request received for UID: {}", principal.uid());
        try {
            userService.completeRegistration(principal.uid(), request);
            log.info("Registration completed successfully for UID: {}", principal.uid());
            return ResponseEntity.ok("User registration completed successfully");
        } catch (Exception e) {
            log.error("Registration completion failed for UID: {}", principal.uid(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error: " + e.getMessage());
        }
    }

    @GetMapping("/profile")
    public ResponseEntity<UserProfileResponseDto> getUserProfile(
            @AuthenticationPrincipal FirebasePrincipal me) {
        log.info("User profile request from UID: {}", me.uid());
        try {
            UserProfileResponseDto profile = userService.getUserProfile(me.uid());
            log.info("Successfully retrieved profile for UID: {}", me.uid());
            return ResponseEntity.ok(profile);
        } catch (IllegalArgumentException e) {
            log.warn("User not found for UID: {}", me.uid());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(null);
        } catch (Exception e) {
            log.error("Failed to retrieve profile for UID: {}", me.uid(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(null);
        }
    }

    @DeleteMapping("/contacts")
    public ResponseEntity<String> deleteLinkedContact(
            @AuthenticationPrincipal FirebasePrincipal me,
            @RequestParam String phoneNumber,
            @RequestParam ContactType contactType) {
        log.info("Delete contact request from UID: {} for phone: {} with type: {}",
                me.uid(), phoneNumber, contactType);
        try {
            userService.deleteLinkedContact(me.uid(), phoneNumber, contactType);
            log.info("Successfully deleted contact {} for UID: {}", phoneNumber, me.uid());
            return ResponseEntity.ok("Contact deleted successfully");
        } catch (IllegalArgumentException e) {
            log.warn("Invalid delete contact request from UID: {} - {}", me.uid(), e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Error: " + e.getMessage());
        } catch (Exception e) {
            log.error("Failed to delete contact for UID: {}", me.uid(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error: " + e.getMessage());
        }
    }

    @PostMapping("/requests/{requestId}/cancel")
    public ResponseEntity<String> cancelRequest(
            @AuthenticationPrincipal FirebasePrincipal me,
            @PathVariable String requestId) {
        log.info("Cancel request attempt for request ID: {} by UID: {}", requestId, me.uid());
        try {
            contactRequestService.cancelRequest(requestId, me.uid());
            log.info("Request {} canceled successfully by UID: {}", requestId, me.uid());
            return ResponseEntity.ok("Request canceled successfully");
        } catch (IllegalArgumentException e) {
            log.warn("Bad request for cancellation - request ID: {}, UID: {} - {}", requestId, me.uid(), e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Error: " + e.getMessage());
        } catch (IllegalStateException e) {
            log.warn("Conflict during cancellation - request ID: {}, UID: {} - {}", requestId, me.uid(), e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("Error: " + e.getMessage());
        } catch (Exception e) {
            log.error("Failed to cancel request ID: {} by UID: {}", requestId, me.uid(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error: " + e.getMessage());
        }
    }
}