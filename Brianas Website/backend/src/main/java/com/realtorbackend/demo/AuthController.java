package com.realtorbackend.demo;

import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final UserService userService;
    private final JwtService jwtService;

    public AuthController(UserService userService, JwtService jwtService) {
        this.userService = userService;
        this.jwtService = jwtService;
    }

    @PostMapping("/register")
    public ResponseEntity<User> register(@RequestBody User user) {
        User created = userService.register(user);
        created.setPassword(null);
        created.setPasswordHash(null);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody Map<String, String> payload) {
        String email = payload.get("email");
        String password = payload.get("password");
        User loggedIn = userService.login(email, password);
        loggedIn.setPassword(null);
        loggedIn.setPasswordHash(null);
        return ResponseEntity.ok(Map.of("token", jwtService.generateToken(loggedIn), "user", loggedIn));
    }

    @GetMapping(value = "/verify-email", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> verifyEmail(@org.springframework.web.bind.annotation.RequestParam String token) {
        return userService.verifyEmail(token)
                ? ResponseEntity.ok("Email verified. You can now return to the site and log in.")
                : ResponseEntity.badRequest().body("This verification link is invalid or has expired.");
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<Void> resendVerification(@RequestBody Map<String, String> payload) {
        userService.resendVerification(payload.get("email"));
        return ResponseEntity.accepted().build();
    }

    @ExceptionHandler({ IllegalArgumentException.class, IllegalStateException.class })
    public ResponseEntity<Map<String, String>> handleAuthError(RuntimeException ex) {
        return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
    }
}
