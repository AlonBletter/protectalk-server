package com.protectalk.usermanagment.repo;

import com.protectalk.usermanagment.model.UserEntity;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends MongoRepository<UserEntity, String> {
    Optional<UserEntity> findByPhoneNumber(String phoneNumber);
    Optional<UserEntity> findByFirebaseUid(String firebaseUid);
    List<UserEntity> findByLinkedContactsPhoneNumber(String phoneNumber);

}