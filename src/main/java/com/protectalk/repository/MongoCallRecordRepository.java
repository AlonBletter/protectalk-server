package com.protectalk.repository;

import com.protectalk.model.CallRecord;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.ArrayList;

@Repository
public class MongoCallRecordRepository implements CallRecordRepository {

    private final MongoCollection<Document> collection;

    public MongoCallRecordRepository(MongoClient mongoClient,
                                     @Value("${spring.data.mongodb.database:protectalk}") String dbName) {
        MongoDatabase database = mongoClient.getDatabase(dbName);
        this.collection = database.getCollection("callrecords");
    }

    @Override
    public void saveCallRecord(CallRecord record) {
        try {
            Document doc = new Document("userId", record.getUserId())
                    .append("script", record.getScript())
                    .append("timestamp", System.currentTimeMillis());
            collection.insertOne(doc);
        } catch (Exception e) {
            throw new RuntimeException("Failed to save call record: " + e.getMessage(), e);
        }
    }

    @Override
    public List<CallRecord> getCallRecords(String userId) {
        List<CallRecord> results = new ArrayList<>();
        try {
            for (Document doc : collection.find(Filters.eq("userId", userId))) {
                String script = doc.getString("script");
                results.add(new CallRecord(userId, script));
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to retrieve call records: " + e.getMessage(), e);
        }
        return results;
    }

    @Override
    public List<CallRecord> getAllCallRecords() {
        List<CallRecord> results = new ArrayList<>();
        try {
            for (Document doc : collection.find()) {
                String userId = doc.getString("userId");
                String script = doc.getString("script");
                results.add(new CallRecord(userId, script));
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to retrieve all call records: " + e.getMessage(), e);
        }
        return results;
    }

    @Override
    public void deleteCallRecord(String userId, String script) {
        try {
            collection.deleteOne(Filters.and(
                    Filters.eq("userId", userId),
                    Filters.eq("script", script)
            ));
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete call record: " + e.getMessage(), e);
        }
    }

    @Override
    public long getRecordCount() {
        try {
            return collection.countDocuments();
        } catch (Exception e) {
            throw new RuntimeException("Failed to count records: " + e.getMessage(), e);
        }
    }
}
