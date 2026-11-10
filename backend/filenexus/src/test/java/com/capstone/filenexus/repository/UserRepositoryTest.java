package com.capstone.filenexus.repository;



import com.capstone.filenexus.entity.User;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@AutoConfigureTestDatabase(
        replace = AutoConfigureTestDatabase.Replace.NONE
)
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    void shouldSaveAndRetrieveUser() {

        User user = new User();

        user.setUsername("admin");
        user.setEmail("admin@filenexus.com");
        user.setPasswordHash("test-password-hash");

        User savedUser = userRepository.saveAndFlush(user);

        assertNotNull(savedUser.getUserId());

        var retrievedUser =
                userRepository.findByEmail("admin@filenexus.com");

        assertTrue(retrievedUser.isPresent());

        assertEquals(
                "admin",
                retrievedUser.get().getUsername()
        );
    }
}
