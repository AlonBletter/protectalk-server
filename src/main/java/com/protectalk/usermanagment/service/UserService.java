package com.protectalk.usermanagment.service;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.UserRecord;
import com.protectalk.device.service.DeviceTokenService;
import com.protectalk.usermanagment.dto.CompleteRegistrationRequestDto;
import com.protectalk.usermanagment.dto.UserRequestDto;
import com.protectalk.usermanagment.model.UserEntity;
import com.protectalk.usermanagment.repo.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import com.protectalk.usermanagment.dto.UserProfileResponseDto;
import com.protectalk.usermanagment.repo.ContactRequestRepository;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;        // Mongo repository for UserEntity
    private final FirebaseAuth firebaseAuth;  // Injected once (configure in a @Configuration bean)
    private final DeviceTokenService deviceTokenService;
    private final ContactRequestRepository contactRequestRepository;

    public String createUser(UserRequestDto req) throws Exception {
        log.info("Creating user with phone number: {}", req.phoneNumber());
        assertUniquePhone(req.phoneNumber());

        UserRecord fbUser = createFirebaseUser(req);
        try {
            UserEntity entity = toEntity(req, fbUser.getUid());
            userRepository.save(entity);
            log.info("Successfully created user with UID: {} and phone: {}", fbUser.getUid(), req.phoneNumber());
            return fbUser.getUid();
        } catch (RuntimeException ex) {
            log.error("Failed to save user entity for UID: {}, deleting Firebase user", fbUser.getUid(), ex);
            safeDeleteFirebaseUser(fbUser.getUid());
            throw ex;
        }
    }

    /**
     * Create or update user profile after client-side Firebase registration
     */
    public void createOrUpdateProfile(String firebaseUid, UserRequestDto req) {
        log.info("Creating or updating profile for UID: {} with phone: {}", firebaseUid, req.phoneNumber());
        UserEntity entity = userRepository.findByFirebaseUid(firebaseUid)
                .orElse(UserEntity.builder()
                        .firebaseUid(firebaseUid)
                        .build());

        entity.setPhoneNumber(req.phoneNumber());
        entity.setName(req.name());

        userRepository.save(entity);
        log.info("Successfully saved profile for UID: {}", firebaseUid);
    }

    /**
     * Complete registration after client-side Firebase registration
     * Handles both user profile creation and device token registration
     */
    public void completeRegistration(String firebaseUid, CompleteRegistrationRequestDto request) {
        log.info("Completing registration for UID: {} with phone: {}", firebaseUid, request.phoneNumber());

        // 1. Create or update user profile
        UserEntity entity = userRepository.findByFirebaseUid(firebaseUid)
                .orElse(UserEntity.builder()
                        .firebaseUid(firebaseUid)
                        .build());

        entity.setPhoneNumber(request.phoneNumber());
        entity.setName(request.name());
        userRepository.save(entity);
        log.debug("User profile saved for UID: {}", firebaseUid);

        // 2. Register device token using existing device service
        deviceTokenService.register(firebaseUid, request.registerTokenRequest());
        log.info("Registration completed successfully for UID: {}", firebaseUid);
    }

    /**
     * Get comprehensive user profile data including contacts and requests
     */
    public UserProfileResponseDto getUserProfile(String firebaseUid) {
        log.info("Fetching user profile for UID: {}", firebaseUid);

        // Get user entity
        UserEntity user = userRepository.findByFirebaseUid(firebaseUid)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // Get linked contacts
        List<UserProfileResponseDto.LinkedContactDto> linkedContacts =
                user.getLinkedContacts() != null ?
                user.getLinkedContacts().stream()
                        .map(contact -> UserProfileResponseDto.LinkedContactDto.builder()
                                .phoneNumber(contact.phoneNumber())
                                .name(contact.name())
                                .relationship(contact.relationship())
                                .contactType("LINKED") // You might want to determine actual type
                                .build())
                        .toList() : List.of();

        // Get pending received requests (where this user is the target)
        List<UserProfileResponseDto.ContactRequestDto> receivedRequests =
                contactRequestRepository.findByTargetUidAndStatus(firebaseUid,
                        com.protectalk.usermanagment.model.ContactRequestEntity.RequestStatus.PENDING)
                .stream()
                .map(req -> UserProfileResponseDto.ContactRequestDto.builder()
                        .id(req.getId())
                        .requesterName(req.getRequesterName())
                        .targetPhoneNumber(req.getTargetPhoneNumber())
                        .relationship(req.getRelationship())
                        .contactType(req.getContactType().toString())
                        .status(req.getStatus().toString())
                        .createdAt(req.getCreatedAt())
                        .isIncoming(true)
                        .build())
                .toList();

        // Get pending sent requests (where this user is the requester)
        List<UserProfileResponseDto.ContactRequestDto> sentRequests =
                contactRequestRepository.findByRequesterUidAndStatus(firebaseUid,
                        com.protectalk.usermanagment.model.ContactRequestEntity.RequestStatus.PENDING)
                .stream()
                .map(req -> UserProfileResponseDto.ContactRequestDto.builder()
                        .id(req.getId())
                        .requesterName(req.getRequesterName())
                        .targetPhoneNumber(req.getTargetPhoneNumber())
                        .relationship(req.getRelationship())
                        .contactType(req.getContactType().toString())
                        .status(req.getStatus().toString())
                        .createdAt(req.getCreatedAt())
                        .isIncoming(false)
                        .build())
                .toList();

        UserProfileResponseDto profile = UserProfileResponseDto.builder()
                .firebaseUid(user.getFirebaseUid())
                .name(user.getName())
                .phoneNumber(user.getPhoneNumber())
                .userType(user.getUserType())
                .createdAt(user.getCreatedAt())
                .linkedContacts(linkedContacts)
                .pendingReceivedRequests(receivedRequests)
                .pendingSentRequests(sentRequests)
                .build();

        log.info("Successfully fetched profile for UID: {} with {} linked contacts, {} received requests, {} sent requests",
                firebaseUid, linkedContacts.size(), receivedRequests.size(), sentRequests.size());

        return profile;
    }

    // ---- helpers ----

    private void assertUniquePhone(String phone) {
        userRepository.findByPhoneNumber(phone).ifPresent(u -> {
            log.warn("Attempt to create user with existing phone number: {}", phone);
            throw new IllegalStateException("Phone number already exists");
        });
    }

    private UserRecord createFirebaseUser(UserRequestDto req) throws Exception {
        var fbReq = new UserRecord.CreateRequest()
                .setPhoneNumber(req.phoneNumber())
                .setDisplayName(req.name());
        return firebaseAuth.createUser(fbReq);
    }

    private UserEntity toEntity(UserRequestDto req, String firebaseUid) {
        var entity = new UserEntity();
        entity.setFirebaseUid(firebaseUid);
        entity.setPhoneNumber(req.phoneNumber());
        entity.setName(req.name());
        return entity;
    }

    private void safeDeleteFirebaseUser(String uid) {
        try {
            firebaseAuth.deleteUser(uid);
            log.info("Successfully deleted Firebase user: {}", uid);
        } catch (Exception e) {
            log.error("Failed to delete Firebase user: {}, user may remain orphaned", uid, e);
        }
    }
}
