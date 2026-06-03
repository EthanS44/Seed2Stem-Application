package com.example.seed2stem;

import org.mindrot.jbcrypt.BCrypt;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.regex.Pattern;


@Service
public class AuthService {

    /**
     * Pragmatic email regex — rejects obvious garbage (no '@', no domain, no
     * TLD) without trying to fully implement RFC 5322. Good enough for form
     * input; the source of truth is whether you can actually deliver mail.
     */
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9._%+\\-]+@[A-Za-z0-9.\\-]+\\.[A-Za-z]{2,}$");

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

    public User login(String email, String password) {
        User user = userRepository.findByEmail(normalizeEmail(email))
                .orElseThrow(() -> new RuntimeException("Invalid email or password"));

        if (!BCrypt.checkpw(password, user.getPassword())) {
            throw new RuntimeException("Invalid email or password");
        }

        return user;
    }

    public void register(String email, String password, String firstName, String lastName) {
        String normalized = normalizeEmail(email);
        validateEmail(normalized);

        if (userRepository.existsByEmail(normalized)) {
            throw new RuntimeException("An account with this email already exists");
        }
        if (registrationRequestRepo.existsByEmail(normalized)) {
            throw new RuntimeException("A registration request for this email is already pending");
        }

        RegistrationRequest request = new RegistrationRequest();
        request.setEmail(normalized);
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
        user.setEmail(request.getEmail());
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
     * email AND matching first and last name (identity check) before creating
     * the request. Raises a clear error if the user can't be found so the
     * requester knows the request didn't go through.
     */
    public void createPasswordResetRequest(String email, String firstName, String lastName) {
        String normalized = normalizeEmail(email);

        User user = userRepository.findByEmail(normalized)
                .orElseThrow(() -> new RuntimeException(
                        "We couldn't find an account matching that information"));

        if (!user.getFirstName().equalsIgnoreCase(firstName)
                || !user.getLastName().equalsIgnoreCase(lastName)) {
            throw new RuntimeException("We couldn't find an account matching that information");
        }

        if (passwordResetRequestRepo.existsByEmailAndStatus(normalized, PasswordResetStatus.PENDING)) {
            throw new RuntimeException("A password reset request for this account is already pending");
        }

        PasswordResetRequest request = new PasswordResetRequest();
        request.setEmail(normalized);
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

        User user = userRepository.findByEmail(request.getEmail())
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

    /** Lowercase + trim so 'Foo@bar.com  ' and 'foo@bar.com' compare equal. */
    private static String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }

    /** Rejects obviously-malformed emails before we hit the DB unique check. */
    private static void validateEmail(String email) {
        if (email == null || email.isBlank() || !EMAIL_PATTERN.matcher(email).matches()) {
            throw new RuntimeException("Please enter a valid email address");
        }
    }
}
