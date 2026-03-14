package com.cardsystem.controller;

import com.cardsystem.dto.PosAuthorizationRequest;
import com.cardsystem.dto.PosAuthorizationResponse;
import com.cardsystem.services.PosAuthorizationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/pos")
@RequiredArgsConstructor
public class PosController {

    private final PosAuthorizationService authorizationService;

    /**
     * PDQ / POS authorization: validates card status, wallet status, and PIN.
     */
    @PostMapping("/authorize")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'SUPER_ADMIN', 'FINANCE_ADMIN')")
    public ResponseEntity<PosAuthorizationResponse> authorize(@Valid @RequestBody PosAuthorizationRequest request) {
        PosAuthorizationResponse resp = authorizationService.authorize(request);
        return ResponseEntity.ok(resp);
    }
}
