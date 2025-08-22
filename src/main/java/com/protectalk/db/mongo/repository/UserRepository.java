package com.protectalk.db.mongo.repository;

import com.protectalk.usermanagment.model.UserEntity;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends MongoRepository<UserEntity, String> {
    Optional<UserEntity> findByPhoneNumber(String phoneNumber);
    List<UserEntity> findByLinkedContactsPhoneNumber(String phoneNumber);

}