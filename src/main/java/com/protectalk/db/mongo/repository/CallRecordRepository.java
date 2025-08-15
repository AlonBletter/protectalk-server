package com.protectalk.db.mongo.repository;

import com.protectalk.db.model.CallRecordEntity;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface CallRecordRepository extends MongoRepository<CallRecordEntity, String> {
    void saveCallRecord(CallRecordEntity record);
    List<CallRecordEntity> getCallRecords(String userId);
    List<CallRecordEntity> getAllCallRecords();
    void deleteCallRecord(String userId, String script);
    long getRecordCount();
}