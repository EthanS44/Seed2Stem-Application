package com.example.seed2stem;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mindrot.jbcrypt.BCrypt;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RegistrationRequestRepository registrationRequestRepo;

    @Mock
    private PasswordResetRequestRepository passwordResetRequestRepo;

    @InjectMocks
    private AuthService authService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User("john", BCrypt.hashpw("password123", BCrypt.gensalt()),
                "John", "Doe", AccountType.TECHNICIAN);
        testUser.setId(1L);
    }

    // --- login ---

    @Test
    void login_withValidCredentials_returnsUser() {
        when(userRepository.findByEmail("john")).thenReturn(Optional.of(testUser));

        User result = authService.login("john", "password123");

        assertNotNull(result);
        assertEquals("john", result.getEmail());
        assertEquals("John", result.getFirstName());
    }

    @Test
    void login_withInvalidUsername_throwsException() {
        when(userRepository.findByEmail("unknown")).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> authService.login("unknown", "password123"));
        assertEquals("Invalid email or password", ex.getMessage());
    }

    @Test
    void login_withWrongPassword_throwsException() {
        when(userRepository.findByEmail("john")).thenReturn(Optional.of(testUser));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> authService.login("john", "wrongpassword"));
        assertEquals("Invalid email or password", ex.getMessage());
    }

    // --- register (now creates RegistrationRequest) ---

    @Test
    void register_withNewEmail_createsRegistrationRequest() {
        when(userRepository.existsByEmail("newuser@s2s.com")).thenReturn(false);
        when(registrationRequestRepo.existsByEmail("newuser@s2s.com")).thenReturn(false);
        when(registrationRequestRepo.save(any(RegistrationRequest.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        authService.register("newuser@s2s.com", "pass123", "Jane", "Smith");

        verify(registrationRequestRepo).save(argThat(req -> {
            assertEquals("newuser@s2s.com", req.getEmail());
            assertEquals("Jane", req.getFirstName());
            assertEquals("Smith", req.getLastName());
            assertNull(req.getAccountType());
            assertEquals(RegistrationStatus.PENDING, req.getStatus());
            assertNotNull(req.getCreatedAt());
            assertTrue(BCrypt.checkpw("pass123", req.getPassword()));
            return true;
        }));
        verify(userRepository, never()).save(any());
    }

    @Test
    void register_normalizesEmail_lowercaseAndTrim() {
        when(userRepository.existsByEmail("jane@s2s.com")).thenReturn(false);
        when(registrationRequestRepo.existsByEmail("jane@s2s.com")).thenReturn(false);
        when(registrationRequestRepo.save(any(RegistrationRequest.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        authService.register("  Jane@S2S.com  ", "pass", "Jane", "Smith");

        verify(registrationRequestRepo).save(argThat(req ->
                "jane@s2s.com".equals(req.getEmail())));
    }

    @Test
    void register_withInvalidEmail_throwsException() {
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> authService.register("not-an-email", "pass", "Jane", "Smith"));
        assertEquals("Please enter a valid email address", ex.getMessage());
        verify(registrationRequestRepo, never()).save(any());
    }

    @Test
    void register_withExistingEmail_throwsException() {
        when(userRepository.existsByEmail("john@s2s.com")).thenReturn(true);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> authService.register("john@s2s.com", "pass", "John", "Doe"));
        assertEquals("An account with this email already exists", ex.getMessage());
        verify(registrationRequestRepo, never()).save(any());
    }

    @Test
    void register_withPendingRequest_throwsException() {
        when(userRepository.existsByEmail("pending@s2s.com")).thenReturn(false);
        when(registrationRequestRepo.existsByEmail("pending@s2s.com")).thenReturn(true);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> authService.register("pending@s2s.com", "pass", "P", "User"));
        assertEquals("A registration request for this email is already pending", ex.getMessage());
        verify(registrationRequestRepo, never()).save(any());
    }

    // --- approveRegistration ---

    @Test
    void approveRegistration_pendingRequest_createsUserWithChosenAccountType() {
        RegistrationRequest request = new RegistrationRequest();
        request.setId(1L);
        request.setEmail("newuser");
        request.setPassword("hashedpass");
        request.setFirstName("Jane");
        request.setLastName("Smith");
        request.setStatus(RegistrationStatus.PENDING);
        request.setCreatedAt(LocalDateTime.now());

        when(registrationRequestRepo.findById(1L)).thenReturn(Optional.of(request));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(registrationRequestRepo.save(any(RegistrationRequest.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        User result = authService.approveRegistration(1L, AccountType.MANAGER);

        assertEquals("newuser", result.getEmail());
        assertEquals("Jane", result.getFirstName());
        assertEquals("Smith", result.getLastName());
        assertEquals(AccountType.MANAGER, result.getAccountType());
        assertEquals("hashedpass", result.getPassword());
        assertEquals(RegistrationStatus.APPROVED, request.getStatus());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void approveRegistration_nullAccountType_throwsException() {
        RegistrationRequest request = new RegistrationRequest();
        request.setId(1L);
        request.setStatus(RegistrationStatus.PENDING);

        when(registrationRequestRepo.findById(1L)).thenReturn(Optional.of(request));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> authService.approveRegistration(1L, null));
        assertEquals("Account type must be selected", ex.getMessage());
        verify(userRepository, never()).save(any());
    }

    @Test
    void approveRegistration_notFound_throwsException() {
        when(registrationRequestRepo.findById(999L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> authService.approveRegistration(999L, AccountType.TECHNICIAN));
        verify(userRepository, never()).save(any());
    }

    @Test
    void approveRegistration_alreadyProcessed_throwsException() {
        RegistrationRequest request = new RegistrationRequest();
        request.setId(1L);
        request.setStatus(RegistrationStatus.APPROVED);

        when(registrationRequestRepo.findById(1L)).thenReturn(Optional.of(request));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> authService.approveRegistration(1L, AccountType.TECHNICIAN));
        assertEquals("This request has already been processed", ex.getMessage());
        verify(userRepository, never()).save(any());
    }

    // --- denyRegistration ---

    @Test
    void denyRegistration_pendingRequest_setsStatusToDenied() {
        RegistrationRequest request = new RegistrationRequest();
        request.setId(1L);
        request.setStatus(RegistrationStatus.PENDING);

        when(registrationRequestRepo.findById(1L)).thenReturn(Optional.of(request));
        when(registrationRequestRepo.save(any(RegistrationRequest.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        authService.denyRegistration(1L);

        assertEquals(RegistrationStatus.DENIED, request.getStatus());
        verify(userRepository, never()).save(any());
    }

    @Test
    void denyRegistration_notFound_throwsException() {
        when(registrationRequestRepo.findById(999L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> authService.denyRegistration(999L));
    }

    @Test
    void denyRegistration_alreadyProcessed_throwsException() {
        RegistrationRequest request = new RegistrationRequest();
        request.setId(1L);
        request.setStatus(RegistrationStatus.DENIED);

        when(registrationRequestRepo.findById(1L)).thenReturn(Optional.of(request));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> authService.denyRegistration(1L));
        assertEquals("This request has already been processed", ex.getMessage());
    }

    // --- createPasswordResetRequest ---

    @Test
    void createPasswordResetRequest_matchingUser_createsRequest() {
        when(userRepository.findByEmail("john")).thenReturn(Optional.of(testUser));
        when(passwordResetRequestRepo.existsByEmailAndStatus("john", PasswordResetStatus.PENDING))
                .thenReturn(false);
        when(passwordResetRequestRepo.save(any(PasswordResetRequest.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        authService.createPasswordResetRequest("john", "John", "Doe");

        verify(passwordResetRequestRepo).save(argThat(req -> {
            assertEquals("john", req.getEmail());
            assertEquals("John", req.getFirstName());
            assertEquals("Doe", req.getLastName());
            assertEquals(PasswordResetStatus.PENDING, req.getStatus());
            assertNotNull(req.getCreatedAt());
            return true;
        }));
    }

    @Test
    void createPasswordResetRequest_caseInsensitiveName_createsRequest() {
        when(userRepository.findByEmail("john")).thenReturn(Optional.of(testUser));
        when(passwordResetRequestRepo.existsByEmailAndStatus("john", PasswordResetStatus.PENDING))
                .thenReturn(false);
        when(passwordResetRequestRepo.save(any(PasswordResetRequest.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        authService.createPasswordResetRequest("john", "JOHN", "doe");

        verify(passwordResetRequestRepo).save(any(PasswordResetRequest.class));
    }

    @Test
    void createPasswordResetRequest_unknownUsername_throwsException() {
        when(userRepository.findByEmail("unknown")).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> authService.createPasswordResetRequest("unknown", "John", "Doe"));
        assertEquals("We couldn't find an account matching that information", ex.getMessage());
        verify(passwordResetRequestRepo, never()).save(any());
    }

    @Test
    void createPasswordResetRequest_nameMismatch_throwsException() {
        when(userRepository.findByEmail("john")).thenReturn(Optional.of(testUser));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> authService.createPasswordResetRequest("john", "Jane", "Smith"));
        assertEquals("We couldn't find an account matching that information", ex.getMessage());
        verify(passwordResetRequestRepo, never()).save(any());
    }

    @Test
    void createPasswordResetRequest_existingPending_throwsException() {
        when(userRepository.findByEmail("john")).thenReturn(Optional.of(testUser));
        when(passwordResetRequestRepo.existsByEmailAndStatus("john", PasswordResetStatus.PENDING))
                .thenReturn(true);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> authService.createPasswordResetRequest("john", "John", "Doe"));
        assertEquals("A password reset request for this account is already pending", ex.getMessage());
        verify(passwordResetRequestRepo, never()).save(any());
    }

    // --- approvePasswordReset ---

    @Test
    void approvePasswordReset_pendingRequest_updatesPasswordAndMarksApproved() {
        User developer = new User("dev", "hash", "Dev", "Eloper", AccountType.DEVELOPER);
        developer.setId(2L);

        PasswordResetRequest request = new PasswordResetRequest();
        request.setId(1L);
        request.setEmail("john");
        request.setStatus(PasswordResetStatus.PENDING);

        when(passwordResetRequestRepo.findById(1L)).thenReturn(Optional.of(request));
        when(userRepository.findByEmail("john")).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(passwordResetRequestRepo.save(any(PasswordResetRequest.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        authService.approvePasswordReset(1L, "newpassword", developer);

        assertEquals(PasswordResetStatus.APPROVED, request.getStatus());
        assertEquals(developer, request.getApprovedBy());
        assertNotNull(request.getApprovedAt());
        assertTrue(BCrypt.checkpw("newpassword", testUser.getPassword()));
        verify(userRepository).save(testUser);
    }

    @Test
    void approvePasswordReset_notFound_throwsException() {
        when(passwordResetRequestRepo.findById(999L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> authService.approvePasswordReset(999L, "newpass", testUser));
        verify(userRepository, never()).save(any());
    }

    @Test
    void approvePasswordReset_alreadyProcessed_throwsException() {
        PasswordResetRequest request = new PasswordResetRequest();
        request.setId(1L);
        request.setStatus(PasswordResetStatus.APPROVED);

        when(passwordResetRequestRepo.findById(1L)).thenReturn(Optional.of(request));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> authService.approvePasswordReset(1L, "newpass", testUser));
        assertEquals("This request has already been processed", ex.getMessage());
        verify(userRepository, never()).save(any());
    }

    @Test
    void approvePasswordReset_blankPassword_throwsException() {
        PasswordResetRequest request = new PasswordResetRequest();
        request.setId(1L);
        request.setStatus(PasswordResetStatus.PENDING);

        when(passwordResetRequestRepo.findById(1L)).thenReturn(Optional.of(request));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> authService.approvePasswordReset(1L, "  ", testUser));
        assertEquals("New password cannot be blank", ex.getMessage());
        verify(userRepository, never()).save(any());
    }

    // --- denyPasswordReset ---

    @Test
    void denyPasswordReset_pendingRequest_marksDenied() {
        User developer = new User("dev", "hash", "Dev", "Eloper", AccountType.DEVELOPER);
        developer.setId(2L);

        PasswordResetRequest request = new PasswordResetRequest();
        request.setId(1L);
        request.setStatus(PasswordResetStatus.PENDING);

        when(passwordResetRequestRepo.findById(1L)).thenReturn(Optional.of(request));
        when(passwordResetRequestRepo.save(any(PasswordResetRequest.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        authService.denyPasswordReset(1L, developer);

        assertEquals(PasswordResetStatus.DENIED, request.getStatus());
        assertEquals(developer, request.getApprovedBy());
        assertNotNull(request.getApprovedAt());
        verify(userRepository, never()).save(any());
    }

    @Test
    void denyPasswordReset_alreadyProcessed_throwsException() {
        PasswordResetRequest request = new PasswordResetRequest();
        request.setId(1L);
        request.setStatus(PasswordResetStatus.DENIED);

        when(passwordResetRequestRepo.findById(1L)).thenReturn(Optional.of(request));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> authService.denyPasswordReset(1L, testUser));
        assertEquals("This request has already been processed", ex.getMessage());
    }

    // --- changeOwnPassword ---

    @Test
    void changeOwnPassword_correctCurrent_updatesPassword() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        authService.changeOwnPassword(1L, "password123", "newpassword");

        assertTrue(BCrypt.checkpw("newpassword", testUser.getPassword()));
        verify(userRepository).save(testUser);
    }

    @Test
    void changeOwnPassword_wrongCurrent_throwsException() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> authService.changeOwnPassword(1L, "wrongpass", "newpassword"));
        assertEquals("Current password is incorrect", ex.getMessage());
        verify(userRepository, never()).save(any());
    }

    @Test
    void changeOwnPassword_blankNew_throwsException() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> authService.changeOwnPassword(1L, "password123", "  "));
        assertEquals("New password cannot be blank", ex.getMessage());
        verify(userRepository, never()).save(any());
    }

    @Test
    void changeOwnPassword_userNotFound_throwsException() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> authService.changeOwnPassword(999L, "anything", "newpassword"));
        verify(userRepository, never()).save(any());
    }
}
