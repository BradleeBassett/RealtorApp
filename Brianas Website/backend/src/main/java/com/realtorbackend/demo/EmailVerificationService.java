package com.realtorbackend.demo;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailVerificationService {
    private static final SecureRandom RANDOM = new SecureRandom();
    private final JdbcTemplate jdbcTemplate;
    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final boolean enabled;
    private final String mailHost;
    private final String fromAddress;
    private final String publicUrl;

    public EmailVerificationService(
            JdbcTemplate jdbcTemplate,
            ObjectProvider<JavaMailSender> mailSenderProvider,
            @Value("${app.email-verification.enabled}") boolean enabled,
            @Value("${spring.mail.host:}") String mailHost,
            @Value("${app.email-verification.from:}") String fromAddress,
            @Value("${app.public-url}") String publicUrl) {
        this.jdbcTemplate = jdbcTemplate;
        this.mailSenderProvider = mailSenderProvider;
        this.enabled = enabled;
        this.mailHost = mailHost;
        this.fromAddress = fromAddress;
        this.publicUrl = publicUrl;
    }

    public void ensureConfigured() {
        if (!enabled) {
            return;
        }
        if (mailHost.isBlank() || fromAddress.isBlank() || mailSenderProvider.getIfAvailable() == null) {
            throw new IllegalStateException("Email verification is not configured. Contact the site administrator.");
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void sendVerificationEmail(User user) {
        if (!enabled) {
            return;
        }
        ensureConfigured();
        String token = newToken();
        jdbcTemplate.update(
                "UPDATE users SET verification_token_hash = ?, verification_token_expires_at = CURRENT_TIMESTAMP + INTERVAL '24 hours' WHERE id = ?",
                hash(token), user.getId());

        String link = publicUrl.replaceAll("/+$", "") + "/api/auth/verify-email?token=" + token;
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(user.getEmail());
        message.setSubject("Verify your Sweetwater Land Company account");
        message.setText("Verify your email address by opening this link within 24 hours:\n\n" + link);
        mailSenderProvider.getObject().send(message);
    }

    public boolean verify(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }
        return jdbcTemplate.update(
                "UPDATE users SET email_verified = TRUE, verification_token_hash = NULL, verification_token_expires_at = NULL "
                        + "WHERE verification_token_hash = ? AND verification_token_expires_at > CURRENT_TIMESTAMP",
                hash(token)) == 1;
    }

    public void resendVerificationEmail(User user) {
        if (!user.isEmailVerified()) {
            sendVerificationEmail(user);
        }
    }

    private String newToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String token) {
        try {
            return Base64.getEncoder().encodeToString(
                    MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to secure verification token", exception);
        }
    }
}