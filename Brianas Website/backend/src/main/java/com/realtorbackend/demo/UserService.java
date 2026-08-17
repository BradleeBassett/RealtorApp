package com.realtorbackend.demo;

import java.util.List;
import java.util.Optional;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final EmailVerificationService emailVerificationService;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public UserService(UserRepository userRepository, EmailVerificationService emailVerificationService) {
        this.userRepository = userRepository;
        this.emailVerificationService = emailVerificationService;
    }

    public List<User> findAll() {
        return userRepository.findAll();
    }

    public Optional<User> findByEmail(String email) {
        if (email == null || email.isBlank()) {
            return Optional.empty();
        }
        return userRepository.findByEmail(email.trim().toLowerCase());
    }

    public User create(User user) {
        if (user == null) {
            throw new IllegalArgumentException("User cannot be null");
        }
        if (user.getEmail() == null || user.getEmail().isBlank()) {
            throw new IllegalArgumentException("User email is required");
        }
        validateEmailAddress(user);
        if (user.getFirstName() == null || user.getFirstName().isBlank()) {
            throw new IllegalArgumentException("First name is required");
        }
        if (user.getLastName() == null || user.getLastName().isBlank()) {
            throw new IllegalArgumentException("Last name is required");
        }
        validatePhoneNumber(user);
        if (user.getRole() == null) {
            user.setRole(User.Role.USER);
        }

        boolean validRole = user.getRole() == User.Role.ADMIN
                || user.getRole() == User.Role.MANAGER
                || user.getRole() == User.Role.AGENT
                || user.getRole() == User.Role.USER;

        if (!validRole) {
            throw new IllegalArgumentException("User role must be one of: ADMIN, MANAGER, AGENT, USER");
        }

        if (user.getPassword() != null && !user.getPassword().isBlank()) {
            user.setPasswordHash(passwordEncoder.encode(user.getPassword()));
            user.setPassword(null);
        }

        if (user.getPasswordHash() == null || user.getPasswordHash().isBlank()) {
            user.setPasswordHash(passwordEncoder.encode("changeme"));
        }

        return userRepository.save(user);
    }

    public User register(User user) {
        if (user == null) {
            throw new IllegalArgumentException("User cannot be null");
        }
        if (user.getPassword() == null || user.getPassword().isBlank()) {
            throw new IllegalArgumentException("Password is required");
        }
        if (user.getEmail() == null || user.getEmail().isBlank()) {
            throw new IllegalArgumentException("Email is required");
        }
        if (findByEmail(user.getEmail()).isPresent()) {
            throw new IllegalArgumentException("A user with that email already exists");
        }

        validateEmailAddress(user);
        user.setRole(User.Role.USER);
        user.setEmailVerified(!emailVerificationService.isEnabled());
        emailVerificationService.ensureConfigured();

        User created = create(user);
        emailVerificationService.sendVerificationEmail(created);
        created.setPassword(null);
        return created;
    }

    public User login(String email, String password) {
        if (email == null || email.isBlank() || password == null || password.isBlank()) {
            throw new IllegalArgumentException("Email and password are required");
        }

        User user = findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Invalid email or password"));

        if (user.getPasswordHash() == null || !passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid email or password");
        }
        if (!user.isEmailVerified()) {
            throw new IllegalArgumentException("Please verify your email before logging in");
        }

        user.setPassword(null);
        return user;
    }

    public boolean verifyEmail(String token) {
        return emailVerificationService.verify(token);
    }

    public void resendVerification(String email) {
        findByEmail(email).ifPresent(emailVerificationService::resendVerificationEmail);
    }

    public boolean deleteById(Long id) {
        return userRepository.deleteById(id);
    }

    public Optional<User> update(Long id, User changes) {
        User existing = userRepository.findById(id).orElse(null);
        if (existing == null) {
            return Optional.empty();
        }

        existing.setFirstName(changes.getFirstName());
        existing.setLastName(changes.getLastName());
        existing.setPhoneNumber(changes.getPhoneNumber());
        existing.setEmail(changes.getEmail());
        existing.setDescription(changes.getDescription());
        existing.setRole(changes.getRole());

        validateProfile(existing);
        return Optional.of(userRepository.save(existing));
    }

    private void validateProfile(User user) {
        if (user.getEmail() == null || user.getEmail().isBlank()) {
            throw new IllegalArgumentException("User email is required");
        }
        if (user.getFirstName() == null || user.getFirstName().isBlank()) {
            throw new IllegalArgumentException("First name is required");
        }
        if (user.getLastName() == null || user.getLastName().isBlank()) {
            throw new IllegalArgumentException("Last name is required");
        }
        validatePhoneNumber(user);
        validateEmailAddress(user);
    }

    private void validatePhoneNumber(User user) {
        String phoneNumber = user.getPhoneNumber();
        if (phoneNumber == null || phoneNumber.isBlank()) {
            throw new IllegalArgumentException("Phone number is required");
        }
        String normalizedPhoneNumber = phoneNumber.trim();
        if (!normalizedPhoneNumber.matches("[0-9+() .-]{7,30}")) {
            throw new IllegalArgumentException("Phone number must contain 7 to 30 valid characters");
        }
        user.setPhoneNumber(normalizedPhoneNumber);
    }

    private void validateEmailAddress(User user) {
        String email = user.getEmail().trim().toLowerCase();
        if (!email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            throw new IllegalArgumentException("Enter a valid email address");
        }
        user.setEmail(email);
    }
}
