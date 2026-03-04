package com.cardsystem.services;

import com.cardsystem.models.RefreshToken;
import com.cardsystem.models.User;
import com.cardsystem.repository.RefreshTokenRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${jwt.refreshExpirationMs}")
    private long refreshExpirationMs;

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
    }

    public RefreshToken createRefreshToken(User user) {
        RefreshToken t = new RefreshToken();
        t.setToken(UUID.randomUUID().toString());
        t.setUser(user);
        t.setExpiryDate(LocalDateTime.now().plusSeconds(refreshExpirationMs / 1000));
        t.setRevoked(false);
        return refreshTokenRepository.save(t);
    }

    public Optional<RefreshToken> findByToken(String token) {
        return refreshTokenRepository.findByToken(token);
    }

    public void revoke(RefreshToken token) {
        token.setRevoked(true);
        refreshTokenRepository.save(token);
    }

    public void revokeAllForUser(User user) {
        refreshTokenRepository.findAllByUser(user).forEach(t -> {
            t.setRevoked(true);
        });
        refreshTokenRepository.deleteAllByUser(user);
    }
}
