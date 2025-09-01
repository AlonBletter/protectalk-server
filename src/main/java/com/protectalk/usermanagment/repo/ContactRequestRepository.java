package com.protectalk.usermanagment.repo;

import com.protectalk.usermanagment.model.ContactType;
import com.protectalk.usermanagment.model.ContactRequestEntity;
import com.protectalk.usermanagment.model.ContactRequestEntity.RequestStatus;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ContactRequestRepository extends MongoRepository<ContactRequestEntity, String> {

    // Find all pending requests for a specific target phone number and contact type
    List<ContactRequestEntity> findByTargetPhoneNumberAndStatusAndContactType(
        String targetPhoneNumber, ContactRequestEntity.RequestStatus status, ContactType contactType);

    // Find all pending requests for a specific target UID and contact type
    List<ContactRequestEntity> findByTargetUidAndStatusAndContactType(
        String targetUid, ContactRequestEntity.RequestStatus status, ContactType contactType);

    // Find all pending requests for a specific target (any contact type)
    List<ContactRequestEntity> findByTargetPhoneNumberAndStatus(String targetPhoneNumber, ContactRequestEntity.RequestStatus status);
    List<ContactRequestEntity> findByTargetUidAndStatus(String targetUid, ContactRequestEntity.RequestStatus status);

    // Find all requests made by a specific user
    List<ContactRequestEntity> findByRequesterUid(String requesterUid);
    List<ContactRequestEntity> findByRequesterUidAndStatus(String requesterUid, ContactRequestEntity.RequestStatus status);

    // Check if a request already exists between two users for a specific contact type
    Optional<ContactRequestEntity> findByRequesterUidAndTargetPhoneNumberAndContactType(
            String requesterUid, String targetPhoneNumber, ContactType contactType);

    // Check if a pending request already exists between two users for a specific contact type
    Optional<ContactRequestEntity> findByRequesterUidAndTargetPhoneNumberAndContactTypeAndStatus(
            String requesterUid, String targetPhoneNumber, ContactType contactType, RequestStatus status);

    // Delete old completed requests to avoid unique constraint issues
    void deleteAllByRequesterUidAndTargetPhoneNumberAndContactTypeAndStatusNotIn(
            String requesterUid, String targetPhoneNumber, ContactType contactType, List<RequestStatus> statuses);

    // Find all pending requests (for cleanup/expiration jobs)
    List<ContactRequestEntity> findByStatus(ContactRequestEntity.RequestStatus status);
}
