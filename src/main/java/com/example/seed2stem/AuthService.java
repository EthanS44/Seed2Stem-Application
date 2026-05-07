package com.example.seed2stem;

import org.mindrot.jbcrypt.BCrypt;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;


@Service
public class AuthService {

    private final UserRepository userRepository;
    private final RegistrationRequestRepository registrationRequestRepo;
    private final PasswordResetRequestRepository passwordResetRequestRepo;

    public AuthService(UserRepository userRepository,
                       RegistrationRequestRepository registrationRequestRepo,
                       PasswordResetRequestRepository passwordResetRequestRepo) {
        this.userRepository = userRepository;
        this.registrationRequestRepo = registrationRequestRepo;
        this.passwordResetRequestRepo = passwordResetRequestRepo;
    }

    public User login(String username, String password) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Invalid username or password"));

        if (!BCrypt.checkpw(password, user.getPassword())) {
            throw new RuntimeException("Invalid username or password");
        }

        return user;
    }

    public void register(String username, String password, String firstName, String lastName) {
        if (userRepository.existsByUsername(username)) {
            throw new RuntimeException("Username already exists");
        }
        if (registrationRequestRepo.existsByUsername(username)) {
            throw new RuntimeException("A registration request for this username is already pending");
        }

        RegistrationRequest request = new RegistrationRequest();
        request.setUsername(username);
        request.setPassword(BCrypt.hashpw(password, BCrypt.gensalt()));
        request.setFirstName(firstName);
        request.setLastName(lastName);
        request.setStatus(RegistrationStatus.PENDING);
        request.setCreatedAt(LocalDateTime.now());

        registrationRequestRepo.save(request);
    }

    public User approveRegistration(Long requestId, AccountType accountType) {
        RegistrationRequest request = registrationRequestRepo.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Registration request not found"));

        if (request.getStatus() != RegistrationStatus.PENDING) {
            throw new RuntimeException("This request has already been processed");
        }

        if (accountType == null) {
            throw new RuntimeException("Account type must be selected");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(request.getPassword());
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setAccountType(accountType);
        userRepository.save(user);

        request.setAccountType(accountType);
        request.setStatus(RegistrationStatus.APPROVED);
        registrationRequestRepo.save(request);

        return user;
    }

    public void denyRegistration(Long requestId) {
        RegistrationRequest request = registrationRequestRepo.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Registration request not found"));

        if (request.getStatus() != RegistrationStatus.PENDING) {
            throw new RuntimeException("This request has already been processed");
        }

        request.setStatus(RegistrationStatus.DENIED);
        registrationRequestRepo.save(request);
    }

    /**
     * Create a password reset request. Verifies a user exists with the given
     * username AND matching first and last name (identity check) before creating
     * the request. Silently creates the request even if no match, so we don't
     * leak which usernames exist — but actually we DO leak by username unique
     * constraint checks elsewhere. For this internal app, we raise a clear
     * error if the user can't be found so users know the request didn't go
     * through.
     */
    public void createPasswordResetRequest(String username, String firstName, String lastName) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException(
                        "We couldn't find an account matching that information"));

        if (!user.getFirstName().equalsIgnoreCase(firstName)
                || !user.getLastName().equalsIgnoreCase(lastName)) {
            throw new RuntimeException("We couldn't find an account matching that information");
        }

        if (passwordResetRequestRepo.existsByUsernameAndStatus(username, PasswordResetStatus.PENDING)) {
            throw new RuntimeException("A password reset request for this account is already pending");
        }

        PasswordResetRequest request = new PasswordResetRequest();
        request.setUsername(username);
        request.setFirstName(user.getFirstName());
        request.setLastName(user.getLastName());
        request.setStatus(PasswordResetStatus.PENDING);
        request.setCreatedAt(LocalDateTime.now());

        passwordResetRequestRepo.save(request);
    }

    public void approvePasswordReset(Long requestId, String newPassword, User developer) {
        PasswordResetRequest request = passwordResetRequestRepo.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Password reset request not found"));

        if (request.getStatus() != PasswordResetStatus.PENDING) {
            throw new RuntimeException("This request has already been processed");
        }

        if (newPassword == null || newPassword.isBlank()) {
            throw new RuntimeException("New password cannot be blank");
        }

        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException("User no longer exists"));

        user.setPassword(BCrypt.hashpw(newPassword, BCrypt.gensalt()));
        userRepository.save(user);

        request.setStatus(PasswordResetStatus.APPROVED);
        request.setApprovedBy(developer);
        request.setApprovedAt(LocalDateTime.now());
        passwordResetRequestRepo.save(request);
    }

    public void denyPasswordReset(Long requestId, User developer) {
        PasswordResetRequest request = passwordResetRequestRepo.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Password reset request not found"));

        if (request.getStatus() != PasswordResetStatus.PENDING) {
            throw new RuntimeException("This request has already been processed");
        }

        request.setStatus(PasswordResetStatus.DENIED);
        request.setApprovedBy(developer);
        request.setApprovedAt(LocalDateTime.now());
        passwordResetRequestRepo.save(request);
    }

    public void changeOwnPassword(Long userId, String currentPassword, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!BCrypt.checkpw(currentPassword, user.getPassword())) {
            throw new RuntimeException("Current password is incorrect");
        }

        if (newPassword == null || newPassword.isBlank()) {
            throw new RuntimeException("New password cannot be blank");
        }

        user.setPassword(BCrypt.hashpw(newPassword, BCrypt.gensalt()));
        userRepository.save(user);
    }
}
