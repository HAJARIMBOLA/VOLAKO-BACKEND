package com.volako.backend.controller;

import com.volako.backend.domain.enums.DebtDirection;
import com.volako.backend.dto.debt.DebtPaymentRequest;
import com.volako.backend.dto.debt.DebtPaymentResponse;
import com.volako.backend.dto.debt.DebtRequest;
import com.volako.backend.dto.debt.DebtResponse;
import com.volako.backend.security.CurrentUserProvider;
import com.volako.backend.service.DebtService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/debts")
@RequiredArgsConstructor
public class DebtController {

    private final DebtService debtService;
    private final CurrentUserProvider currentUserProvider;

    @GetMapping
    public ResponseEntity<List<DebtResponse>> list(@RequestParam(required = false) DebtDirection direction) {
        return ResponseEntity.ok(debtService.listDebts(currentUserProvider.getCurrentUserId(), direction));
    }

    @GetMapping("/overdue")
    public ResponseEntity<List<DebtResponse>> overdue() {
        return ResponseEntity.ok(debtService.listOverdueDebts(currentUserProvider.getCurrentUserId()));
    }

    @PostMapping
    public ResponseEntity<DebtResponse> create(@Valid @RequestBody DebtRequest request) {
        DebtResponse response = debtService.createDebt(currentUserProvider.getCurrentUserId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<DebtResponse> update(@PathVariable Long id, @Valid @RequestBody DebtRequest request) {
        return ResponseEntity.ok(debtService.updateDebt(currentUserProvider.getCurrentUserId(), id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        debtService.deleteDebt(currentUserProvider.getCurrentUserId(), id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/payments")
    public ResponseEntity<List<DebtPaymentResponse>> payments(@PathVariable Long id) {
        return ResponseEntity.ok(debtService.listPayments(currentUserProvider.getCurrentUserId(), id));
    }

    @PostMapping("/{id}/payments")
    public ResponseEntity<DebtPaymentResponse> addPayment(@PathVariable Long id, @Valid @RequestBody DebtPaymentRequest request) {
        DebtPaymentResponse response = debtService.addPayment(currentUserProvider.getCurrentUserId(), id, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
