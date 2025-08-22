package com.protectalk.db.mongo.repository;

import com.protectalk.db.model.CallRecordEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CallRecordRepository extends MongoRepository<CallRecordEntity, String> {


    // fetch by owner
    List<CallRecordEntity> findByUserId(String userId);

    // optional: sorted/paged variants if you have createdAt
    List<CallRecordEntity> findByUserIdOrderByCreatedAtDesc(String userId);
    Page<CallRecordEntity> findByUserId(String userId, Pageable pageable);

    // counts
    long countByUserId(String userId);
}
