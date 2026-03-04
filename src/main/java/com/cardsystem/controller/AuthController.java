package com.cardsystem.controller;

import com.cardsystem.dto.*;
import com.cardsystem.models.User;
import com.cardsystem.security.JwtUtil;
import com.cardsystem.services.RefreshTokenService;
import com.cardsystem.services.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final UserService userService;
    private final UserDetailsService userDetailsService;
    private final RefreshTokenService refreshTokenService;

    // ────────────────────────────────────────────────
    // 1. User Info
    // ────────────────────────────────────────────────

    @GetMapping("/me")
    public ResponseEntity<com.cardsystem.dto.UserResponse> me() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        String username = auth.getName();
        return userService.findByUsername(username)
                .map(u -> com.cardsystem.dto.UserResponse.builder()
                        .id(u.getId())
                        .username(u.getUsername())
                        .fullName(u.getFullName())
                        .role(u.getRole())
                        .active(u.isActive())
                        .createdAt(u.getCreatedAt())
                        .updatedAt(u.getUpdatedAt())
                        .build())
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }


    // ────────────────────────────────────────────────
    // 2. Login User
    // ────────────────────────────────────────────────

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);
        String token = jwtUtil.generateToken(authentication);
        String role = authentication.getAuthorities().stream()
                .map(Object::toString)
                .findFirst()
                .orElse("");

        // create refresh token
        User user = userService.findByUsername(request.getUsername()).orElseThrow();
        String refresh = refreshTokenService.createRefreshToken(user).getToken();

        return ResponseEntity.ok(new LoginResponse(token, role, refresh));
    }

    // ────────────────────────────────────────────────
    // 3. Register / create user (super-admin)
    // ────────────────────────────────────────────────

    @PostMapping("/users")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<User> register(@Valid @RequestBody RegisterRequest request) {
        User created = userService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // ────────────────────────────────────────────────
    // 4. Refresh token (not much needed)
    // ────────────────────────────────────────────────

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@Valid @RequestBody RefreshRequest req) {
        return refreshTokenService.findByToken(req.getRefreshToken())
                .map(rt -> {
                    if (rt.isRevoked() || rt.getExpiryDate().isBefore(java.time.LocalDateTime.now())) {
                        return ResponseEntity.<RefreshResponse>status(HttpStatus.UNAUTHORIZED).build();
                    }
                    UserDetails ud = userDetailsService.loadUserByUsername(rt.getUser().getUsername());
                    UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(ud, null, ud.getAuthorities());
                    String access = jwtUtil.generateToken(auth);
                    // optional: rotate refresh token
                    refreshTokenService.revoke(rt);
                    String newRefresh = refreshTokenService.createRefreshToken(rt.getUser()).getToken();
                    return ResponseEntity.ok(new RefreshResponse(access, newRefresh));
                })
                .orElseGet(() -> ResponseEntity.<RefreshResponse>status(HttpStatus.UNAUTHORIZED).build());
    }

    // ────────────────────────────────────────────────
    // 5. Logout User
    // ────────────────────────────────────────────────

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshRequest req) {
        refreshTokenService.findByToken(req.getRefreshToken()).ifPresent(refreshTokenService::revoke);
        return ResponseEntity.noContent().build();
    }

    // ────────────────────────────────────────────────
    // 6. Change password
    // ────────────────────────────────────────────────

    @PostMapping("/change-password")
    public ResponseEntity<Void> changePassword(@Valid @RequestBody ChangePasswordRequest req) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        String username = auth.getName();
        userService.changePassword(username, req.getOldPassword(), req.getNewPassword());
        return ResponseEntity.noContent().build();
    }

    // ────────────────────────────────────────────────
    // 7. Change user role
    // ────────────────────────────────────────────────

    @PutMapping("/users/{id}/role")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Void> changeRole(@PathVariable Long id, @Valid @RequestBody ChangeRoleRequest req) {
        userService.changeRole(req.getUsername(), req.getRole());
        return ResponseEntity.noContent().build();
    }

    // ────────────────────────────────────────────────
    // 8. Get all users
    // ────────────────────────────────────────────────

    @GetMapping("/users")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<java.util.List<com.cardsystem.dto.UserResponse>> getAllUsers() {
        return ResponseEntity.ok(userService.listAllUsers());
    }


}
