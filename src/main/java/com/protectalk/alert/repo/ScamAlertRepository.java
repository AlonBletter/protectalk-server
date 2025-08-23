// src/main/java/com/protectalk/alerts/repo/CallRecordRepository.java
package com.protectalk.alert.repo;

import com.protectalk.alert.model.AlertRecordEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface ScamAlertRepository extends MongoRepository<AlertRecordEntity, String> {

    List<AlertRecordEntity> findByUserId(String userId);

    List<AlertRecordEntity> findByUserIdOrderByCreatedAtDesc(String userId);
    Page<AlertRecordEntity> findByUserId(String userId, Pageable pageable);

    Optional<AlertRecordEntity> findByEventId(String eventId);
    Optional<AlertRecordEntity> findByUserIdAndEventId(String userId, String eventId);

    long countByUserId(String userId);
}
