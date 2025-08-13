//package com.protectalk.usermanagment.repository;
//
//import com.protectalk.usermanagment.model.User;
//import org.springframework.data.mongodb.repository.MongoRepository;
//import java.util.Optional;
//
//public interface UserRepository extends MongoRepository<User, String> {
//    Optional<User> findByPhoneNumber(String phoneNumber);
//}