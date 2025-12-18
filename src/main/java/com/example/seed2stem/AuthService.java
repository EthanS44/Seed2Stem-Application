package com.example.seed2stem;

import org.mindrot.jbcrypt.BCrypt;
import org.springframework.stereotype.Service;


@Service
public class AuthService {

    private final UserRepository userRepository;

    public AuthService(UserRepository userRepository) {
        this.userRepository = userRepository;
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

        User user = new User();
        user.setUsername(username);
        user.setPassword(BCrypt.hashpw(password, BCrypt.gensalt())); // hash password with jBCrypt
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setAccountType(accountType);

        userRepository.save(user);
    }
}



