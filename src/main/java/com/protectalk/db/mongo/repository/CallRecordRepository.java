package com.protectalk.db.mongo.repository;

import com.protectalk.db.model.CallRecordEntity;
import java.util.List;

public interface CallRecordRepository {
    void saveCallRecord(CallRecordEntity record);
    List<CallRecordEntity> getCallRecords(String userId);
    List<CallRecordEntity> getAllCallRecords();
    void deleteCallRecord(String userId, String script);
    long getRecordCount();
}