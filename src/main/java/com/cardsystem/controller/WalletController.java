package com.cardsystem.controller;

import com.cardsystem.dto.WalletAdjustRequest;
import com.cardsystem.dto.WalletFreezeRequest;
import com.cardsystem.dto.WalletResponse;
import com.cardsystem.models.Student;
import com.cardsystem.models.Wallet;
import com.cardsystem.services.WalletService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/wallets")
@RequiredArgsConstructor
public class WalletController {

    private final WalletService walletService;

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','FINANCE_ADMIN','SCHOOL_ADMIN','READ_ONLY')")
    @Transactional(readOnly = true)
    public ResponseEntity<List<WalletResponse>> listWallets(
            @RequestParam(value = "schoolId", required = false) String schoolId) {

        List<WalletResponse> body = walletService.listWallets(schoolId).stream()
                .map(this::toDto)
                .collect(Collectors.toList());

        return ResponseEntity.ok(body);
    }

    @PostMapping("/adjust")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','FINANCE_ADMIN')")
    @Transactional
    public ResponseEntity<WalletResponse> adjust(@Valid @RequestBody WalletAdjustRequest request) {
        Wallet wallet = walletService.manualAdjustAndReturn(request.getWalletId(), request.getAmount(), request.getReason());
        return ResponseEntity.ok(toDto(wallet));
    }

    @PostMapping("/freeze")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','FINANCE_ADMIN')")
    @Transactional
    public ResponseEntity<WalletResponse> freeze(@Valid @RequestBody WalletFreezeRequest request) {
        Wallet wallet = walletService.setFreeze(request.getWalletId(), request.isFreeze());
        return ResponseEntity.ok(toDto(wallet));
    }

    private WalletResponse toDto(Wallet wallet) {
        Student student = wallet.getStudent();
        return WalletResponse.builder()
                .walletId(wallet.getId())
                .studentId(student.getId())
                .studentNumber(student.getStudentNumber())
                .studentName(student.getName())
                .schoolCode(student.getSchool().getCode())
                .balance(wallet.getCachedBalance())
                .status(wallet.getStatus())
                .build();
    }
}
