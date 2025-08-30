package com.protectalk.usermanagment.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document("contact_requests")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@CompoundIndex(name = "requester_target_type_unique", def = "{'requesterUid': 1, 'targetPhoneNumber': 1, 'contactType': 1}", unique = true)
public class ContactRequestEntity {

    @Id
    private String id;

    @Indexed
    private String requesterUid;        // Firebase UID of the person making the request

    private String requesterName;       // Name provided by requester

    @Indexed
    private String targetPhoneNumber;   // Phone number of the person being requested

    private String targetUid;           // Firebase UID of target (if they're registered), null if not found

    private String relationship;        // "family", "friend", "other"

    @Indexed
    private ContactType contactType;    // TRUSTED_CONTACT or PROTEGEE

    private RequestStatus status;       // PENDING, APPROVED, DENIED, EXPIRED

    @CreatedDate
    private Instant createdAt;

    private Instant respondedAt;        // When the request was approved/denied

    private Instant deletedAt;

    public enum RequestStatus {
        PENDING,
        APPROVED,
        DENIED,
        EXPIRED
    }
}
