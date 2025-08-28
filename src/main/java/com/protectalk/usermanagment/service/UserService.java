package com.protectalk.usermanagment.service;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.UserRecord;
import com.protectalk.device.service.DeviceTokenService;
import com.protectalk.usermanagment.dto.CompleteRegistrationRequestDto;
import com.protectalk.usermanagment.dto.UserRequestDto;
import com.protectalk.usermanagment.model.UserEntity;
import com.protectalk.usermanagment.repo.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;        // Mongo repository for UserEntity
    private final FirebaseAuth firebaseAuth;  // Injected once (configure in a @Configuration bean)
    private final DeviceTokenService deviceTokenService;

    public String createUser(UserRequestDto req) throws Exception {
        assertUniquePhone(req.phoneNumber());

        UserRecord fbUser = createFirebaseUser(req);
        try {
            UserEntity entity = toEntity(req, fbUser.getUid());
            userRepository.save(entity);
            return fbUser.getUid();
        } catch (RuntimeException ex) {
            safeDeleteFirebaseUser(fbUser.getUid());
            throw ex;
        }
    }

    /**
     * Create or update user profile after client-side Firebase registration
     */
    public void createOrUpdateProfile(String firebaseUid, UserRequestDto req) {
        UserEntity entity = userRepository.findByFirebaseUid(firebaseUid)
                .orElse(UserEntity.builder()
                        .firebaseUid(firebaseUid)
                        .build());

        entity.setPhoneNumber(req.phoneNumber());
        entity.setName(req.name());

        userRepository.save(entity);
    }

    /**
     * Complete registration after client-side Firebase registration
     * Handles both user profile creation and device token registration
     */
    public void completeRegistration(String firebaseUid, CompleteRegistrationRequestDto request) {
        // 1. Create or update user profile
        UserEntity entity = userRepository.findByFirebaseUid(firebaseUid)
                .orElse(UserEntity.builder()
                        .firebaseUid(firebaseUid)
                        .build());

        entity.setPhoneNumber(request.phoneNumber());
        entity.setName(request.name());
        userRepository.save(entity);

        // 2. Register device token using existing device service
        deviceTokenService.register(firebaseUid, request.registerTokenRequest());
    }

    // ---- helpers ----

    private void assertUniquePhone(String phone) {
        userRepository.findByPhoneNumber(phone).ifPresent(u -> {
            throw new IllegalStateException("Phone number already exists");
        });
    }

    private UserRecord createFirebaseUser(UserRequestDto req) throws Exception {
        var fbReq = new UserRecord.CreateRequest()
                .setPhoneNumber(req.phoneNumber())
                .setDisplayName(req.name());
        return firebaseAuth.createUser(fbReq);
    }

    private UserEntity toEntity(UserRequestDto req, String firebaseUid) {
        var entity = new UserEntity();
        entity.setFirebaseUid(firebaseUid);
        entity.setPhoneNumber(req.phoneNumber());
        entity.setName(req.name());
        return entity;
    }

    private void safeDeleteFirebaseUser(String uid) {
        try {
            firebaseAuth.deleteUser(uid);
        } catch (Exception ignored) {
            // optional: log and alert—Firebase user may remain orphaned if Mongo save failed
        }
    }
}
