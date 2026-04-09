package com.example.seed2stem;

import org.mindrot.jbcrypt.BCrypt;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;


@Service
public class AuthService {

    private final UserRepository userRepository;
    private final RegistrationRequestRepository registrationRequestRepo;

    public AuthService(UserRepository userRepository,
                       RegistrationRequestRepository registrationRequestRepo) {
        this.userRepository = userRepository;
        this.registrationRequestRepo = registrationRequestRepo;
    }

    public User login(String username, String password) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Invalid username or password"));

        if (!BCrypt.checkpw(password, user.getPassword())) {
            throw new RuntimeException("Invalid username or password");
        }

        return user;
    }

    public void register(String username, String password, String firstName, String lastName, AccountType accountType) {
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
        request.setAccountType(accountType);
        request.setStatus(RegistrationStatus.PENDING);
        request.setCreatedAt(LocalDateTime.now());

        registrationRequestRepo.save(request);
    }

    public User approveRegistration(Long requestId) {
        RegistrationRequest request = registrationRequestRepo.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Registration request not found"));

        if (request.getStatus() != RegistrationStatus.PENDING) {
            throw new RuntimeException("This request has already been processed");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(request.getPassword());
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setAccountType(request.getAccountType());
        userRepository.save(user);

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
}
