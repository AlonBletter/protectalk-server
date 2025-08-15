package com.protectalk.usermanagment.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

@Document("users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserEntity {

    @Id
    private String id;

    @Indexed(unique = true)
    private String phoneNumber;          // E.164

    @Indexed(unique = true, sparse = true)
    private String firebaseUid;          // Link to Firebase user

    private String name;
    private String dateOfBirth;          // keep as String to match current service; can switch to LocalDate later
    private String userType;             // Protegee / TrustedContact / Both

    private List<LinkedContact> linkedContacts;
    private List<LinkedContact> oldLinkedContacts;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;

    public record LinkedContact(String phoneNumber, String name, String relationship) {}
}