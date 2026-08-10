package com.safeher.backend.service;

import com.safeher.backend.dto.AuthResponse;
import com.safeher.backend.dto.LoginRequest;
import com.safeher.backend.dto.RegisterRequest;
import com.safeher.backend.entity.Role;
import com.safeher.backend.entity.User;
import com.safeher.backend.exception.ApiException;
import com.safeher.backend.repository.UserRepository;
import com.safeher.backend.security.JwtService;
import com.safeher.backend.util.HandleGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final HandleGenerator handleGenerator;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = request.email().trim().toLowerCase();

        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "That email is already registered. Try signing in instead.");
        }

        User user = User.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(request.password()))
                .anonHandle(handleGenerator.generateUnique())
                .displayCity(blankToNull(request.displayCity()))
                .role(Role.USER)
                .build();

        User saved = userRepository.save(user);
        return toResponse(saved);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        String email = request.email().trim().toLowerCase();

        // One message for both "no such account" and "wrong password", so this
        // endpoint cannot be used to discover which emails are registered.
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED,
                        "That email and password do not match."));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED,
                    "That email and password do not match.");
        }

        if (user.isBanned()) {
            throw new ApiException(HttpStatus.FORBIDDEN,
                    "This account has been suspended. Contact support if you think "
                    + "this is a mistake.");
        }

        user.setLastSeenAt(Instant.now());
        userRepository.save(user);

        return toResponse(user);
    }

    private AuthResponse toResponse(User user) {
        return new AuthResponse(
                jwtService.issue(user),
                user.getId(),
                user.getAnonHandle(),
                user.getRole().name(),
                jwtService.getTtlSeconds());
    }

    private String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }
}
