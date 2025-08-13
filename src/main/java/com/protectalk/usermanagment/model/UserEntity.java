package com.protectalk.usermanagment.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.List;

@Document(collection = "users")
@Data                         // getters, setters, toString, equals, hashCode
@Builder
public class UserEntity {

    @Id
    private String id;  // MongoDB internal _id

    private String phoneNumber;
    private String name;
    private String dateOfBirth;
    private String userType;  // Could be enum
    private List<LinkedContact> linkedContacts;
    private List<LinkedContact> oldLinkedContacts;

    public record LinkedContact(
            String phoneNumber,
            String name,
            String relationship
    ) {}
}