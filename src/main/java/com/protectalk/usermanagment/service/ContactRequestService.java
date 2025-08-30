package com.protectalk.usermanagment.service;

import com.protectalk.usermanagment.dto.ContactRequestDto;
import com.protectalk.usermanagment.model.ContactType;
import com.protectalk.usermanagment.model.ContactRequestEntity;
import com.protectalk.usermanagment.model.UserEntity;
import com.protectalk.usermanagment.repo.ContactRequestRepository;
import com.protectalk.usermanagment.repo.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContactRequestService {

    private final ContactRequestRepository requestRepository;
    private final UserRepository           userRepository;

    /**
     * Create a new contact request (trusted contact or protegee)
     */
    public void createRequest(String requesterUid, ContactRequestDto requestDto) {
        log.info("Creating {} request from UID: {} to phone: {}",
                requestDto.contactType(), requesterUid, requestDto.phoneNumber());

        // Check if request already exists for this contact type
        var existingRequest = requestRepository.findByRequesterUidAndTargetPhoneNumberAndContactType(
                requesterUid, requestDto.phoneNumber(), requestDto.contactType());

        if (existingRequest.isPresent()) {
            log.warn("Request already exists for UID: {} to phone: {} with type: {}",
                    requesterUid, requestDto.phoneNumber(), requestDto.contactType());
            throw new IllegalStateException("Request already exists for this contact with type: " + requestDto.contactType());
        }

        // Get requester's name
        String requesterName = userRepository.findByFirebaseUid(requesterUid)
                .map(UserEntity::getName)
                .orElse("Unknown User");

        // Check if target is already a registered user
        String targetUid = userRepository.findByPhoneNumber(requestDto.phoneNumber())
                .map(UserEntity::getFirebaseUid)
                .orElse(null);

        log.debug("Target UID for phone {}: {}", requestDto.phoneNumber(), targetUid != null ? targetUid : "not registered");

        // Create the request
        ContactRequestEntity request = ContactRequestEntity.builder()
                                                           .requesterUid(requesterUid)
                                                           .requesterName(requesterName)
                                                           .targetPhoneNumber(requestDto.phoneNumber())
                                                           .targetUid(targetUid)
                                                           .relationship(requestDto.relationship())
                                                           .contactType(requestDto.contactType())
                                                           .status(ContactRequestEntity.RequestStatus.PENDING)
                                                           .build();

        requestRepository.save(request);
        log.info("Successfully created {} request from {} to {}",
                requestDto.contactType(), requesterName, requestDto.phoneNumber());
    }

    /**
     * Get all pending requests for a user (both by phone and UID)
     */
    public List<ContactRequestEntity> getPendingRequestsForUser(String userUid) {
        log.debug("Getting pending requests for UID: {}", userUid);

        // Get user's phone number
        String phoneNumber = userRepository.findByFirebaseUid(userUid)
                .map(UserEntity::getPhoneNumber)
                .orElse(null);

        List<ContactRequestEntity> requests = requestRepository.findByTargetUidAndStatus(
            userUid, ContactRequestEntity.RequestStatus.PENDING);

        // Also check by phone number in case the request was made before user registered
        if (phoneNumber != null) {
            List<ContactRequestEntity> phoneRequests = requestRepository.findByTargetPhoneNumberAndStatus(
                phoneNumber, ContactRequestEntity.RequestStatus.PENDING);

            // Update these requests with the target UID now that we know it
            phoneRequests.forEach(req -> {
                if (req.getTargetUid() == null) {
                    log.debug("Updating request {} with target UID: {}", req.getId(), userUid);
                    req.setTargetUid(userUid);
                    requestRepository.save(req);
                }
            });

            requests.addAll(phoneRequests);
        }

        log.info("Found {} pending requests for UID: {}", requests.size(), userUid);
        return requests;
    }

    /**
     * Approve a contact request and handle the relationship based on contact type
     */
    public void approveRequest(String requestId, String approvingUserUid) {
        log.info("Approving request {} by UID: {}", requestId, approvingUserUid);

        ContactRequestEntity request = requestRepository.findById(requestId)
                                                        .orElseThrow(() -> new IllegalArgumentException("Request not found"));

        // Get the approving user's phone number to verify they are the target
        String approvingUserPhone = userRepository.findByFirebaseUid(approvingUserUid)
                .map(UserEntity::getPhoneNumber)
                .orElseThrow(() -> new IllegalArgumentException("Approving user not found"));

        // Verify the approving user is the target by comparing phone numbers
        if (!approvingUserPhone.equals(request.getTargetPhoneNumber())) {
            log.warn("Unauthorized approval attempt - UID: {} (phone: {}) tried to approve request for target phone: {}",
                    approvingUserUid, approvingUserPhone, request.getTargetPhoneNumber());
            throw new IllegalArgumentException("User not authorized to approve this request");
        }

        if (request.getStatus() != ContactRequestEntity.RequestStatus.PENDING) {
            log.warn("Attempt to approve non-pending request: {} with status: {}", requestId, request.getStatus());
            throw new IllegalStateException("Request is not pending");
        }

        // Update request status
        request.setStatus(ContactRequestEntity.RequestStatus.APPROVED);
        requestRepository.save(request);

        log.info("Successfully approved {} request from {} to phone: {}",
                request.getContactType(), request.getRequesterName(), request.getTargetPhoneNumber());
    }

    /**
     * Deny a contact request
     */
    public void denyRequest(String requestId, String denyingUserUid) {
        log.info("Denying request {} by UID: {}", requestId, denyingUserUid);

        ContactRequestEntity request = requestRepository.findById(requestId)
                                                        .orElseThrow(() -> new IllegalArgumentException("Request not found"));

        // Get the denying user's phone number to verify they are the target
        String denyingUserPhone = userRepository.findByFirebaseUid(denyingUserUid)
                .map(UserEntity::getPhoneNumber)
                .orElseThrow(() -> new IllegalArgumentException("Denying user not found"));

        // Verify the denying user is the target by comparing phone numbers
        if (!denyingUserPhone.equals(request.getTargetPhoneNumber())) {
            log.warn("Unauthorized denial attempt - UID: {} (phone: {}) tried to deny request for target phone: {}",
                    denyingUserUid, denyingUserPhone, request.getTargetPhoneNumber());
            throw new IllegalArgumentException("User not authorized to deny this request");
        }

        if (request.getStatus() != ContactRequestEntity.RequestStatus.PENDING) {
            log.warn("Attempt to deny non-pending request: {} with status: {}", requestId, request.getStatus());
            throw new IllegalStateException("Request is not pending");
        }

        request.setStatus(ContactRequestEntity.RequestStatus.DENIED);
        request.setRespondedAt(Instant.now());
        requestRepository.save(request);

        log.info("Successfully denied {} request from {} to phone: {}",
                request.getContactType(), request.getRequesterName(), request.getTargetPhoneNumber());
    }

    // Helper methods
    private void addLinkedContact(String userUid, String phoneNumber, String name, String relationship, ContactType contactType) {
        userRepository.findByFirebaseUid(userUid).ifPresent(user -> {
            UserEntity.LinkedContact newContact = new UserEntity.LinkedContact(phoneNumber, name, relationship);
            if (user.getLinkedContacts() == null) {
                user.setLinkedContacts(List.of(newContact));
            } else {
                user.getLinkedContacts().add(newContact);
            }
            userRepository.save(user);
        });
    }

    private String getTargetName(String targetUid) {
        return userRepository.findByFirebaseUid(targetUid)
                .map(UserEntity::getName)
                .orElse("Unknown");
    }

    private String getRequesterPhoneNumber(String requesterUid) {
        return userRepository.findByFirebaseUid(requesterUid)
                .map(UserEntity::getPhoneNumber)
                .orElse("Unknown");
    }

    private String getInverseRelationship(String relationship) {
        // Simple inverse mapping - you can expand this
        return switch (relationship.toLowerCase()) {
            case "parent" -> "child";
            case "child" -> "parent";
            case "spouse" -> "spouse";
            default -> "contact";
        };
    }
}
