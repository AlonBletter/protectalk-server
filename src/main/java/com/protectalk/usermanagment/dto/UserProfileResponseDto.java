package com.protectalk.usermanagment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfileResponseDto {

    // Basic user information
    private String firebaseUid;
    private String name;
    private String phoneNumber;
    private String userType;
    private Instant createdAt;

    // Contact relationships
    private List<LinkedContactDto> linkedContacts;

    // Pending requests (both sent and received)
    private List<ContactRequestDto> pendingReceivedRequests;
    private List<ContactRequestDto> pendingSentRequests;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class LinkedContactDto {
        private String phoneNumber;
        private String name;
        private String relationship;
        private String contactType; // TRUSTED_CONTACT or PROTEGEE
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ContactRequestDto {
        private String id;
        private String requesterPhoneNumber; // Phone number of the person making the request
        private String requesterName;       // Name of the person making the request
        private String targetPhoneNumber;
        private String targetName;          // Name of the target person
        private String relationship;
        private String contactType;
        private String status;
        private Instant createdAt;
    }
}
