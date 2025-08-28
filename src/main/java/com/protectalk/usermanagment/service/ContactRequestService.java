package com.protectalk.usermanagment.service;

import com.protectalk.usermanagment.dto.ContactRequestDto;
import com.protectalk.usermanagment.model.ContactType;
import com.protectalk.usermanagment.model.ContactRequestEntity;
import com.protectalk.usermanagment.model.UserEntity;
import com.protectalk.usermanagment.repo.ContactRequestRepository;
import com.protectalk.usermanagment.repo.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ContactRequestService {

    private final ContactRequestRepository requestRepository;
    private final UserRepository           userRepository;

    /**
     * Create a new contact request (trusted contact or protegee)
     */
    public void createRequest(String requesterUid, ContactRequestDto requestDto) {
        // Check if request already exists for this contact type
        var existingRequest = requestRepository.findByRequesterUidAndTargetPhoneNumberAndContactType(
                requesterUid, requestDto.phoneNumber(), requestDto.contactType());

        if (existingRequest.isPresent()) {
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
    }

    /**
     * Get all pending requests for a user (both by phone and UID)
     */
    public List<ContactRequestEntity> getPendingRequestsForUser(String userUid) {
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
                    req.setTargetUid(userUid);
                    requestRepository.save(req);
                }
            });

            requests.addAll(phoneRequests);
        }

        return requests;
    }

    /**
     * Approve a contact request and handle the relationship based on contact type
     */
    public void approveRequest(String requestId, String approvingUserUid) {
        ContactRequestEntity request = requestRepository.findById(requestId)
                                                        .orElseThrow(() -> new IllegalArgumentException("Request not found"));

        // Verify the approving user is the target
        if (!approvingUserUid.equals(request.getTargetUid())) {
            throw new IllegalArgumentException("User not authorized to approve this request");
        }

        if (request.getStatus() != ContactRequestEntity.RequestStatus.PENDING) {
            throw new IllegalStateException("Request is not pending");
        }

        // Update request status
        request.setStatus(ContactRequestEntity.RequestStatus.APPROVED);
        request.setRespondedAt(Instant.now());
        requestRepository.save(request);

        // Handle the relationship based on contact type
        if (request.getContactType() == ContactType.TRUSTED_CONTACT) {
            // Requester wants target as trusted contact (target will receive alerts from requester)
            addLinkedContact(request.getRequesterUid(), request.getTargetPhoneNumber(),
                    getTargetName(request.getTargetUid()), request.getRelationship(), ContactType.TRUSTED_CONTACT);
            addLinkedContact(request.getTargetUid(), getRequesterPhoneNumber(request.getRequesterUid()),
                    request.getRequesterName(), getInverseRelationship(request.getRelationship()), ContactType.PROTEGEE);
        } else { // PROTEGEE
            // Requester wants to be target's protegee (requester will receive alerts from target)
            addLinkedContact(request.getRequesterUid(), request.getTargetPhoneNumber(),
                    getTargetName(request.getTargetUid()), request.getRelationship(), ContactType.PROTEGEE);
            addLinkedContact(request.getTargetUid(), getRequesterPhoneNumber(request.getRequesterUid()),
                    request.getRequesterName(), getInverseRelationship(request.getRelationship()), ContactType.TRUSTED_CONTACT);
        }
    }

    /**
     * Deny a trusted contact request
     */
    public void denyRequest(String requestId, String denyingUserUid) {
        ContactRequestEntity request = requestRepository.findById(requestId)
                                                        .orElseThrow(() -> new IllegalArgumentException("Request not found"));

        // Verify the denying user is the target
        if (!denyingUserUid.equals(request.getTargetUid())) {
            throw new IllegalArgumentException("User not authorized to deny this request");
        }

        if (request.getStatus() != ContactRequestEntity.RequestStatus.PENDING) {
            throw new IllegalStateException("Request is not pending");
        }

        request.setStatus(ContactRequestEntity.RequestStatus.DENIED);
        request.setRespondedAt(Instant.now());
        requestRepository.save(request);
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
