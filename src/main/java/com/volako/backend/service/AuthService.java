package com.volako.backend.service;

import com.volako.backend.domain.RefreshToken;
import com.volako.backend.domain.User;
import com.volako.backend.dto.auth.AuthResponse;
import com.volako.backend.dto.auth.LoginRequest;
import com.volako.backend.dto.auth.RegisterRequest;
import com.volako.backend.dto.auth.UserResponse;
import com.volako.backend.exception.BadRequestException;
import com.volako.backend.exception.UnauthorizedException;
import com.volako.backend.repository.RefreshTokenRepository;
import com.volako.backend.repository.UserRepository;
import com.volako.backend.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final DefaultCategorySeeder defaultCategorySeeder;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @org.springframework.beans.factory.annotation.Value("${app.jwt.refresh-token-expiration-ms}")
    private long refreshTokenExpirationMs;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByPhoneNumber(request.phoneNumber())) {
            throw new BadRequestException("PHONE_NUMBER_TAKEN", "Un compte existe déjà avec ce numéro de téléphone");
        }
        if (userRepository.existsByEmail(request.email())) {
            throw new BadRequestException("EMAIL_TAKEN", "Un compte existe déjà avec cet email");
        }

        User user = User.builder()
                .firstName(request.firstName())
                .lastName(request.lastName())
                .phoneNumber(request.phoneNumber())
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .build();
        user = userRepository.save(user);

        defaultCategorySeeder.seedFor(user);

        return buildAuthResponse(user);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByPhoneNumber(request.phoneNumber())
                .orElseThrow(() -> new UnauthorizedException("Numéro de téléphone ou mot de passe incorrect"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new UnauthorizedException("Numéro de téléphone ou mot de passe incorrect");
        }

        return buildAuthResponse(user);
    }

    @Transactional
    public AuthResponse refresh(String rawRefreshToken) {
        RefreshToken storedToken = refreshTokenRepository.findByToken(rawRefreshToken)
                .orElseThrow(() -> new UnauthorizedException("Refresh token invalide"));

        if (!storedToken.isValid()) {
            throw new UnauthorizedException("Refresh token expiré ou révoqué");
        }

        storedToken.setRevoked(true);
        refreshTokenRepository.save(storedToken);

        return buildAuthResponse(storedToken.getUser());
    }

    @Transactional
    public void logout(String rawRefreshToken) {
        refreshTokenRepository.findByToken(rawRefreshToken)
                .ifPresent(token -> {
                    token.setRevoked(true);
                    refreshTokenRepository.save(token);
                });
    }

    public UserResponse me(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UnauthorizedException("Utilisateur introuvable"));
        return UserResponse.from(user);
    }

    private AuthResponse buildAuthResponse(User user) {
        String accessToken = jwtService.generateAccessToken(user.getId(), user.getPhoneNumber());
        String rawRefreshToken = UUID.randomUUID().toString() + UUID.randomUUID();

        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .token(rawRefreshToken)
                .expiresAt(OffsetDateTime.now().plus(Duration.ofMillis(refreshTokenExpirationMs)))
                .revoked(false)
                .build();
        refreshTokenRepository.save(refreshToken);

        return new AuthResponse(accessToken, rawRefreshToken, UserResponse.from(user));
    }
}
