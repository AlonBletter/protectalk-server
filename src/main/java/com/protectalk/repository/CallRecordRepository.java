package com.protectalk.repository;

import com.protectalk.model.CallRecord;
import java.util.List;

public interface CallRecordRepository {
    void saveCallRecord(CallRecord record);
    List<CallRecord> getCallRecords(String userId);
    List<CallRecord> getAllCallRecords();
    void deleteCallRecord(String userId, String script);
    long getRecordCount();
}