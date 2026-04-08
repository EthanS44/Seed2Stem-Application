package com.example.seed2stem;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mindrot.jbcrypt.BCrypt;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AuthService authService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User("john", BCrypt.hashpw("password123", BCrypt.gensalt()),
                "John", "Doe", AccountType.TECHNICIAN);
        testUser.setId(1L);
    }

    @Test
    void login_withValidCredentials_returnsUser() {
        when(userRepository.findByUsername("john")).thenReturn(Optional.of(testUser));

        User result = authService.login("john", "password123");

        assertNotNull(result);
        assertEquals("john", result.getUsername());
        assertEquals("John", result.getFirstName());
    }

    @Test
    void login_withInvalidUsername_throwsException() {
        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> authService.login("unknown", "password123"));
        assertEquals("Invalid username or password", ex.getMessage());
    }

    @Test
    void login_withWrongPassword_throwsException() {
        when(userRepository.findByUsername("john")).thenReturn(Optional.of(testUser));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> authService.login("john", "wrongpassword"));
        assertEquals("Invalid username or password", ex.getMessage());
    }

    @Test
    void register_withNewUsername_savesUser() {
        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        authService.register("newuser", "pass123", "Jane", "Smith", AccountType.MANAGER);

        verify(userRepository).save(argThat(user -> {
            assertEquals("newuser", user.getUsername());
            assertEquals("Jane", user.getFirstName());
            assertEquals("Smith", user.getLastName());
            assertEquals(AccountType.MANAGER, user.getAccountType());
            assertTrue(BCrypt.checkpw("pass123", user.getPassword()));
            return true;
        }));
    }

    @Test
    void register_withExistingUsername_throwsException() {
        when(userRepository.existsByUsername("john")).thenReturn(true);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> authService.register("john", "pass", "John", "Doe", AccountType.TECHNICIAN));
        assertEquals("Username already exists", ex.getMessage());
        verify(userRepository, never()).save(any());
    }
}
