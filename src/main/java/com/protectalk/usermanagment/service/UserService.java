package com.protectalk.usermanagment.service;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.UserRecord;
import com.protectalk.usermanagment.dto.UserRequestDto;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    public String createUser(UserRequestDto request) throws Exception {
        UserRecord.CreateRequest createRequest = new UserRecord.CreateRequest()
                .setPhoneNumber(request.phoneNumber())
                .setDisplayName(request.name());

        UserRecord userRecord = FirebaseAuth.getInstance().createUser(createRequest);
        return userRecord.getUid();
    }
}

//@Service
//public class UserService {
//
//    private final UserRepository userRepository;
//
//    public UserService(UserRepository userRepository) {
//        this.userRepository = userRepository;
//    }
//
//    public String createUser(UserRequest request) throws Exception {
//        // 1️⃣ Create Firebase Auth user
//        UserRecord.CreateRequest firebaseRequest = new UserRecord.CreateRequest()
//                .setPhoneNumber(request.phoneNumber())
//                .setDisplayName(request.name());
//
//        UserRecord firebaseUser = FirebaseAuth.getInstance().createUser(firebaseRequest);
//
//        // 2️⃣ Save extra data in MongoDB - extract to converter
//        User user = new User();
//        user.setPhoneNumber(request.phoneNumber());
//        user.setName(request.name());
//        user.setDateOfBirth(request.dateOfBirth());
//        user.setUserType(request.userType());
//        user.setLinkedContacts(List.of(
//                new User.LinkedContact(
//                        request.linkedContactPhone(),
//                        request.linkedContactName(),
//                        request.relationship()
//                )
//        ));
//        userRepository.save(user);
//
//        // 3️⃣ Return Firebase UID
//        return firebaseUser.getUid();
//    }
//}
