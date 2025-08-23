package com.protectalk.usermanagment.service;

import com.protectalk.usermanagment.repo.UserRepository;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserQueryServiceTest {

    @Mock
    UserRepository repo;

    @InjectMocks
    UserQueryService service;

//    @Test
//    void findUsersLinkedTo_returnsTwoUsers() {
//        // given
//        String linkedPhone = "+15559876543";
//
//        var contact = new UserEntity.LinkedContact("+15559876543", "Jane Smith", "Daughter");
//        var u1 = new UserEntity("u1", "+15551230001", "John Doe", List.of(contact));
//        var u2 = new UserEntity("u2", "+15551230002", "Mary Roe", List.of(contact));
//
//        when(repo.findByLinkedContactsPhoneNumber(linkedPhone))
//                .thenReturn(List.of(u1, u2));
//
//        // when
//        List<UserEntity> result = service.findUsersLinkedTo(linkedPhone);
//
//        // then
//        assertEquals(2, result.size());
//        assertTrue(result.stream().anyMatch(u -> u.name.equals("John Doe")));
//        assertTrue(result.stream().anyMatch(u -> u.name.equals("Mary Roe")));
//
//        verify(repo, times(1)).findByLinkedContactsPhoneNumber(linkedPhone);
//        verifyNoMoreInteractions(repo);
//    }
}
