package com.protectalk.usermanagment.service;

import com.protectalk.device.service.DeviceTokenService;
import com.protectalk.messaging.NotificationGateway;
import com.protectalk.messaging.OutboundMessage;
import com.protectalk.usermanagment.dto.AddContactRequestDto;
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
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContactRequestService {

    // Notification constants
    private static final String NOTIFICATION_TYPE_APPROVED    = "contact_request_approved";
    private static final String NOTIFICATION_TYPE_DENIED      = "contact_request_denied";
    private static final String NOTIFICATION_TYPE_RECEIVED    = "contact_request_received";
    private static final String NOTIFICATION_TITLE_APPROVED   = "Request Approved";
    private static final String NOTIFICATION_TITLE_DENIED     = "Request Denied";
    private static final String NOTIFICATION_TITLE_RECEIVED   = "New Contact Request";
    private static final String CONTACT_TYPE_DISPLAY_TRUSTED  = "trusted contact";
    private static final String CONTACT_TYPE_DISPLAY_PROTEGEE = "protegee";
    private static final String DEFAULT_USER_NAME             = "Someone";
    private static final String DEFAULT_UNKNOWN_USER          = "Unknown User";
    private static final String DEFAULT_UNKNOWN               = "Unknown";

    // Notification data keys
    private static final String DATA_KEY_TYPE                = "type";
    private static final String DATA_KEY_REQUEST_ID          = "requestId";
    private static final String DATA_KEY_CONTACT_TYPE        = "contactType";
    private static final String DATA_KEY_APPROVING_USER_NAME = "approvingUserName";
    private static final String DATA_KEY_DENYING_USER_NAME   = "denyingUserName";
    private static final String DATA_KEY_REQUESTER_NAME      = "requesterName";
    private static final String DATA_KEY_TARGET_PHONE_NUMBER = "targetPhoneNumber";

    // Relationship mappings
    private static final String RELATIONSHIP_PARENT  = "parent";
    private static final String RELATIONSHIP_CHILD   = "child";
    private static final String RELATIONSHIP_SPOUSE  = "spouse";
    private static final String RELATIONSHIP_CONTACT = "contact";

    // Log message types
    private static final String LOG_TYPE_APPROVAL = "approval";
    private static final String LOG_TYPE_DENIAL   = "denial";
    private static final String LOG_TYPE_RECEIVED = "received";
    private static final String LOG_TYPE_CANCEL   = "cancel";

    private final ContactRequestRepository requestRepository;
    private final UserRepository           userRepository;
    private final DeviceTokenService       deviceTokenService;
    private final NotificationGateway      notificationGateway;

    /**
     * Create a new contact request (trusted contact or protégée)
     */
    public void createRequest(String requesterUid, AddContactRequestDto requestDto) {
        log.info("Creating {} request from UID: {} to phone: {}", requestDto.contactType(), requesterUid,
                 requestDto.phoneNumber());

        // Check if there's already a PENDING request for this contact type
        var existingPendingRequest = requestRepository.findByRequesterUidAndTargetPhoneNumberAndContactTypeAndStatus(
                requesterUid, requestDto.phoneNumber(), requestDto.contactType(), ContactRequestEntity.RequestStatus.PENDING);

        if (existingPendingRequest.isPresent()) {
            log.warn("Pending request already exists for UID: {} to phone: {} with type: {}", requesterUid,
                     requestDto.phoneNumber(), requestDto.contactType());
            throw new IllegalStateException(
                "Pending request already exists for this contact with type: " + requestDto.contactType());
        }

        // Check if users currently have an active linked contact relationship
        if (hasActiveLinkedContact(requesterUid, requestDto.phoneNumber(), requestDto.contactType())) {
            log.warn("Active linked contact already exists for UID: {} to phone: {} with type: {}", requesterUid,
                     requestDto.phoneNumber(), requestDto.contactType());
            throw new IllegalStateException(
                "Active contact relationship already exists with type: " + requestDto.contactType());
        }

        // Get requester's name
        String requesterName =
            userRepository.findByFirebaseUid(requesterUid).map(UserEntity::getName).orElse(DEFAULT_UNKNOWN_USER);

        // Check if target is already a registered user
        String targetUid =
            userRepository.findByPhoneNumber(requestDto.phoneNumber()).map(UserEntity::getFirebaseUid).orElse(null);

        log.debug("Target UID for phone {}: {}", requestDto.phoneNumber(),
                  targetUid != null ? targetUid : "not registered");

        // Create the request
        ContactRequestEntity request =
            ContactRequestEntity.builder().requesterUid(requesterUid).requesterName(requesterName)
                                .targetPhoneNumber(requestDto.phoneNumber()).targetName(requestDto.name())
                                .targetUid(targetUid)
                                .relationship(requestDto.relationship()).contactType(requestDto.contactType())
                                .status(ContactRequestEntity.RequestStatus.PENDING).build();

        requestRepository.save(request);
        log.info("Successfully created {} request from {} to {}", requestDto.contactType(), requesterName,
                 requestDto.phoneNumber());

        // Send push notification to the target user if they are registered
        if (targetUid != null) {
            sendReceivedNotification(request);
        }
    }

    /**
     * Get all pending requests for a user (both by phone and UID)
     */
    public List<ContactRequestEntity> getPendingRequestsForUser(String userUid) {
        log.debug("Getting pending requests for UID: {}", userUid);

        // Get user's phone number
        String phoneNumber = userRepository.findByFirebaseUid(userUid).map(UserEntity::getPhoneNumber).orElse(null);

        List<ContactRequestEntity> requests =
            requestRepository.findByTargetUidAndStatus(userUid, ContactRequestEntity.RequestStatus.PENDING);

        // Also check by phone number in case the request was made before user registered
        if (phoneNumber != null) {
            List<ContactRequestEntity> phoneRequests = requestRepository.findByTargetPhoneNumberAndStatus(phoneNumber,
                                                                                                          ContactRequestEntity.RequestStatus.PENDING);

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

        ContactRequestEntity request =
            requestRepository.findById(requestId).orElseThrow(() -> new IllegalArgumentException("Request not found"));

        // Get the approving user's phone number to verify they are the target
        String approvingUserPhone = userRepository.findByFirebaseUid(approvingUserUid).map(UserEntity::getPhoneNumber)
                                                  .orElseThrow(
                                                      () -> new IllegalArgumentException("Approving user not found"));

        // Verify the approving user is the target by comparing phone numbers
        if (!approvingUserPhone.equals(request.getTargetPhoneNumber())) {
            log.warn(
                "Unauthorized approval attempt - UID: {} (phone: {}) tried to approve request for target phone: {}",
                approvingUserUid, approvingUserPhone, request.getTargetPhoneNumber());
            throw new IllegalArgumentException("User not authorized to approve this request");
        }

        if (request.getStatus() != ContactRequestEntity.RequestStatus.PENDING) {
            log.warn("Attempt to approve non-pending request: {} with status: {}", requestId, request.getStatus());
            throw new IllegalStateException("Request is not pending");
        }

        // Update request status
        request.setStatus(ContactRequestEntity.RequestStatus.APPROVED);
        request.setRespondedAt(Instant.now());
        requestRepository.save(request);

        log.info("Successfully approved {} request from {} to phone: {}", request.getContactType(),
                 request.getRequesterName(), request.getTargetPhoneNumber());

        // Add the relationship to both users' linked contacts
        createLinkedContactRelationship(request, approvingUserUid);

        // Send push notification to the requester
        sendApprovalNotification(request, approvingUserUid);
    }

    /**
     * Create the linked contact relationship between requester and approver
     */
    private void createLinkedContactRelationship(ContactRequestEntity request, String approvingUserUid) {
        try {
            // Get approving user's details
            UserEntity approvingUser = userRepository.findByFirebaseUid(approvingUserUid).orElseThrow(
                () -> new IllegalArgumentException("Approving user not found"));

            // Get requester's details
            UserEntity requesterUser = userRepository.findByFirebaseUid(request.getRequesterUid()).orElseThrow(
                () -> new IllegalArgumentException("Requester user not found"));

            if (request.getContactType() == ContactType.TRUSTED_CONTACT) {
                // Requester wants approver as their TRUSTED_CONTACT
                // Add approver to requester's trusted contacts
                addLinkedContact(requesterUser, approvingUser.getPhoneNumber(), approvingUser.getName(),
                                 request.getRelationship(), ContactType.TRUSTED_CONTACT);

                // Add requester to approver's protegees (inverse relationship)
                String inverseRelationship = getInverseRelationship(request.getRelationship());
                addLinkedContact(approvingUser, requesterUser.getPhoneNumber(), requesterUser.getName(),
                                 inverseRelationship, ContactType.PROTEGEE);

                log.info("Created TRUSTED_CONTACT relationship: {} -> {}", requesterUser.getName(),
                         approvingUser.getName());

            }
            else if (request.getContactType() == ContactType.PROTEGEE) {
                // Requester wants approver as their PROTÉGÉE
                // Add approver to requester's protegees
                addLinkedContact(requesterUser, approvingUser.getPhoneNumber(), approvingUser.getName(),
                                 request.getRelationship(), ContactType.PROTEGEE);

                // Add requester to approver's trusted contacts (inverse relationship)
                String inverseRelationship = getInverseRelationship(request.getRelationship());
                addLinkedContact(approvingUser, requesterUser.getPhoneNumber(), requesterUser.getName(),
                                 inverseRelationship, ContactType.TRUSTED_CONTACT);

                log.info("Created PROTEGEE relationship: {} -> {}", requesterUser.getName(), approvingUser.getName());
            }

        }
        catch (Exception e) {
            log.error("Failed to create linked contact relationship for request: {}", request.getId(), e);
            // Don't throw - approval should succeed even if relationship creation fails
        }
    }

    /**
     * Deny a contact request
     */
    public void denyRequest(String requestId, String denyingUserUid) {
        log.info("Denying request {} by UID: {}", requestId, denyingUserUid);

        ContactRequestEntity request =
            requestRepository.findById(requestId).orElseThrow(() -> new IllegalArgumentException("Request not found"));

        // Get the denying user's phone number to verify they are the target
        String denyingUserPhone = userRepository.findByFirebaseUid(denyingUserUid).map(UserEntity::getPhoneNumber)
                                                .orElseThrow(
                                                    () -> new IllegalArgumentException("Denying user not found"));

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

        // Update request status
        request.setStatus(ContactRequestEntity.RequestStatus.DENIED);
        request.setRespondedAt(Instant.now());
        requestRepository.save(request);

        log.info("Successfully denied {} request from {} to phone: {}", request.getContactType(),
                 request.getRequesterName(), request.getTargetPhoneNumber());

        // Send push notification to the requester about the denial
        sendDenialNotification(request, denyingUserUid);
    }

    /**
     * Cancel a pending contact request
     */
    public void cancelRequest(String requestId, String cancelingUserUid) {
        log.info("Canceling request {} by UID: {}", requestId, cancelingUserUid);

        ContactRequestEntity request = requestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Request not found"));

        // Verify the canceling user is the requester
        if (!cancelingUserUid.equals(request.getRequesterUid())) {
            log.warn("Unauthorized cancel attempt - UID: {} tried to cancel request made by: {}",
                    cancelingUserUid, request.getRequesterUid());
            throw new IllegalArgumentException("User not authorized to cancel this request");
        }

        if (request.getStatus() != ContactRequestEntity.RequestStatus.PENDING) {
            log.warn("Attempt to cancel non-pending request: {} with status: {}", requestId, request.getStatus());
            throw new IllegalStateException("Only pending requests can be canceled");
        }

        // Update request status to CANCELED instead of deleting
        request.setStatus(ContactRequestEntity.RequestStatus.CANCELED);
        request.setRespondedAt(Instant.now());
        requestRepository.save(request);

        log.info("Successfully canceled {} request from {} to phone: {}",
                request.getContactType(), request.getRequesterName(), request.getTargetPhoneNumber());
    }

    /**
     * Send push notification to requester when their contact request is approved
     */
    private void sendApprovalNotification(ContactRequestEntity request, String approvingUserUid) {
        String approvingUserName =
            userRepository.findByFirebaseUid(approvingUserUid).map(UserEntity::getName).orElse(DEFAULT_USER_NAME);

        String contactTypeDisplay =
            request.getContactType() == ContactType.TRUSTED_CONTACT ? CONTACT_TYPE_DISPLAY_TRUSTED :
            CONTACT_TYPE_DISPLAY_PROTEGEE;

        String body  = String.format("%s accepted your %s request", approvingUserName, contactTypeDisplay);

        Map<String, String> data =
            Map.of(DATA_KEY_TYPE, NOTIFICATION_TYPE_APPROVED,
                   DATA_KEY_REQUEST_ID, request.getId(),
                   DATA_KEY_CONTACT_TYPE, request.getContactType().toString(),
                   DATA_KEY_APPROVING_USER_NAME, approvingUserName,
                   DATA_KEY_TARGET_PHONE_NUMBER, request.getTargetPhoneNumber());

        sendNotificationToRequester(request, NOTIFICATION_TITLE_APPROVED, body, data, LOG_TYPE_APPROVAL);
    }

    /**
     * Send push notification to requester when their contact request is denied
     */
    private void sendDenialNotification(ContactRequestEntity request, String denyingUserUid) {
        String denyingUserName =
            userRepository.findByFirebaseUid(denyingUserUid).map(UserEntity::getName).orElse(DEFAULT_USER_NAME);

        String contactTypeDisplay =
            request.getContactType() == ContactType.TRUSTED_CONTACT ? CONTACT_TYPE_DISPLAY_TRUSTED :
            CONTACT_TYPE_DISPLAY_PROTEGEE;

        String body  = String.format("%s denied your %s request", denyingUserName, contactTypeDisplay);

        Map<String, String> data =
            Map.of(DATA_KEY_TYPE, NOTIFICATION_TYPE_DENIED,
                   DATA_KEY_REQUEST_ID, request.getId(),
                   DATA_KEY_CONTACT_TYPE, request.getContactType().toString(),
                   DATA_KEY_DENYING_USER_NAME, denyingUserName, DATA_KEY_TARGET_PHONE_NUMBER, request.getTargetPhoneNumber());

        sendNotificationToRequester(request, NOTIFICATION_TITLE_DENIED, body, data, LOG_TYPE_DENIAL);
    }

    /**
     * Send push notification to target user when they receive a new contact request
     */
    private void sendReceivedNotification(ContactRequestEntity request) {
        String contactTypeDisplay = request.getContactType() == ContactType.TRUSTED_CONTACT ?
                CONTACT_TYPE_DISPLAY_TRUSTED : CONTACT_TYPE_DISPLAY_PROTEGEE;

        String title = NOTIFICATION_TITLE_RECEIVED;
        String body = String.format("%s wants to add you as their %s",
                request.getRequesterName(), contactTypeDisplay);

        Map<String, String> data = Map.of(
                DATA_KEY_TYPE, NOTIFICATION_TYPE_RECEIVED,
                DATA_KEY_REQUEST_ID, request.getId(),
                DATA_KEY_CONTACT_TYPE, request.getContactType().toString(),
                DATA_KEY_REQUESTER_NAME, request.getRequesterName(),
                DATA_KEY_TARGET_PHONE_NUMBER, request.getTargetPhoneNumber()
        );

        sendNotificationToTarget(request, title, body, data, LOG_TYPE_RECEIVED);
    }

    /**
     * Generic method to send push notification to the target user
     */
    private void sendNotificationToTarget(ContactRequestEntity request, String title, String body,
                                        Map<String, String> data, String notificationType) {
        try {
            // Get target user's device tokens
            List<String> tokens = deviceTokenService.getDeviceTokensForUser(request.getTargetUid());

            if (tokens.isEmpty()) {
                log.warn("No device tokens found for target UID: {}", request.getTargetUid());
                return;
            }

            OutboundMessage message = new OutboundMessage(title, body, data, tokens);
            var result = notificationGateway.send(message);

            log.info("Sent {} notification to target UID: {} - success: {}, failure: {}",
                    notificationType, request.getTargetUid(), result.success(), result.failure());

            // Clean up invalid tokens if any
            if (!result.invalidTokens().isEmpty()) {
                log.info("Cleaning up {} invalid tokens for target UID: {}",
                        result.invalidTokens().size(), request.getTargetUid());
                result.invalidTokens().forEach(deviceTokenService::deleteToken);
            }

        } catch (Exception e) {
            log.error("Failed to send {} notification for request: {}", notificationType, request.getId(), e);
            // Don't throw - request processing should succeed even if notification fails
        }
    }

    /**
     * Generic method to send push notification to the requester
     */
    private void sendNotificationToRequester(ContactRequestEntity request, String title, String body,
                                             Map<String, String> data, String notificationType) {
        try {
            // Get requester's device tokens
            List<String> tokens = deviceTokenService.getDeviceTokensForUser(request.getRequesterUid());

            if (tokens.isEmpty()) {
                log.warn("No device tokens found for requester UID: {}", request.getRequesterUid());
                return;
            }

            OutboundMessage message = new OutboundMessage(title, body, data, tokens);
            var             result  = notificationGateway.send(message);

            log.info("Sent {} notification to requester UID: {} - success: {}, failure: {}", notificationType,
                     request.getRequesterUid(), result.success(), result.failure());

            // Clean up invalid tokens if any
            if (!result.invalidTokens().isEmpty()) {
                log.info("Cleaning up {} invalid tokens for UID: {}", result.invalidTokens().size(),
                         request.getRequesterUid());
                result.invalidTokens().forEach(deviceTokenService::deleteToken);
            }

        }
        catch (Exception e) {
            log.error("Failed to send {} notification for request: {}", notificationType, request.getId(), e);
            // Don't throw - request processing should succeed even if notification fails
        }
    }

    /**
     * Add a linked contact to a user's contact list
     */
    private void addLinkedContact(UserEntity user, String phoneNumber, String name, String relationship,
                                  ContactType contactType) {
        UserEntity.LinkedContact newContact =
            new UserEntity.LinkedContact(phoneNumber, name, relationship, contactType, Instant.now(), null);

        if (user.getLinkedContacts() == null) {
            user.setLinkedContacts(List.of(newContact));
        }
        else {
            // Check if contact already exists
            boolean exists = user.getLinkedContacts().stream().anyMatch(
                contact -> contact.phoneNumber().equals(phoneNumber) && contact.contactType() == contactType);

            if (!exists) {
                user.getLinkedContacts().add(newContact);
            }
            else {
                log.debug("Contact {} already exists as {} for user {}", phoneNumber, contactType, user.getName());
                return;
            }
        }

        userRepository.save(user);
        log.debug("Added {} contact {} to user {}", contactType, name, user.getName());
    }

    private String getInverseRelationship(String relationship) {
        // Simple inverse mapping - you can expand this
        return switch (relationship.toLowerCase()) {
            case RELATIONSHIP_PARENT -> RELATIONSHIP_CHILD;
            case RELATIONSHIP_CHILD -> RELATIONSHIP_PARENT;
            case RELATIONSHIP_SPOUSE -> RELATIONSHIP_SPOUSE;
            default -> RELATIONSHIP_CONTACT;
        };
    }

    /**
     * Check if there is an active linked contact relationship for the given user and contact
     */
    private boolean hasActiveLinkedContact(String userUid, String targetPhoneNumber, ContactType contactType) {
        // Check user's linked contacts
        UserEntity user = userRepository.findByFirebaseUid(userUid).orElse(null);
        if (user != null && user.getLinkedContacts() != null) {
            for (UserEntity.LinkedContact contact : user.getLinkedContacts()) {
                if (contact.phoneNumber().equals(targetPhoneNumber) && contact.contactType() == contactType) {
                    return true;
                }
            }
        }

        // Check if target is a user and if their linked contacts include the user
        UserEntity targetUser = userRepository.findByPhoneNumber(targetPhoneNumber).orElse(null);
        if (targetUser != null && targetUser.getLinkedContacts() != null) {
            for (UserEntity.LinkedContact contact : targetUser.getLinkedContacts()) {
                if (contact.phoneNumber().equals(userUid) && contact.contactType() == contactType) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Cleanup old requests for the same contact to avoid unique constraint violation
     */
    private void cleanupOldRequests(String requesterUid, String targetPhoneNumber, ContactType contactType) {
        // Delete old completed requests (approved or denied) for the same contact
        requestRepository.deleteAllByRequesterUidAndTargetPhoneNumberAndContactTypeAndStatusNotIn(
                requesterUid, targetPhoneNumber, contactType, List.of(ContactRequestEntity.RequestStatus.PENDING));
    }
}
