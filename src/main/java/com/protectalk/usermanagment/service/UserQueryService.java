package com.protectalk.usermanagment.service;

import com.google.firebase.remoteconfig.User;
import com.protectalk.usermanagment.model.UserEntity;
import com.protectalk.usermanagment.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserQueryService {
    private final UserRepository repo;
    public UserQueryService(UserRepository repo) { this.repo = repo; }

    public List<UserEntity> findUsersLinkedTo(String linkedPhone) {
        return repo.findByLinkedContactsPhoneNumber(linkedPhone);
    }
}
