package com.protectalk.db.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document("call_records")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CallRecordEntity {

    @Id
    private String id;

    private String userId;
    private String callerNumber;
    private String transcript;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;

    private Instant deletedAt; // You set this manually if you soft-delete
}
