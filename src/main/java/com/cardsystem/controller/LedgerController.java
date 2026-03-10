package com.cardsystem.controller;



import com.cardsystem.dto.PaymentNotificationDto;
import com.cardsystem.services.LedgerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/internal/ledger")
@RequiredArgsConstructor
public class LedgerController {

    private final LedgerService paymentNotificationService;

    @PostMapping("/credit")
    public ResponseEntity<Void> credit(@Valid @RequestBody PaymentNotificationDto dto) {
        paymentNotificationService.handlePayment(dto);
        return ResponseEntity.ok().build();
    }
}