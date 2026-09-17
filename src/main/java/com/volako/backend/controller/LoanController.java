package com.volako.backend.controller;

import com.volako.backend.dto.loan.LoanPaymentRequest;
import com.volako.backend.dto.loan.LoanPaymentResponse;
import com.volako.backend.dto.loan.LoanRequest;
import com.volako.backend.dto.loan.LoanResponse;
import com.volako.backend.security.CurrentUserProvider;
import com.volako.backend.service.LoanService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/loans")
@RequiredArgsConstructor
public class LoanController {

    private final LoanService loanService;
    private final CurrentUserProvider currentUserProvider;

    @GetMapping
    public ResponseEntity<List<LoanResponse>> list() {
        return ResponseEntity.ok(loanService.listLoans(currentUserProvider.getCurrentUserId()));
    }

    @PostMapping
    public ResponseEntity<LoanResponse> create(@Valid @RequestBody LoanRequest request) {
        LoanResponse response = loanService.createLoan(currentUserProvider.getCurrentUserId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<LoanResponse> update(@PathVariable Long id, @Valid @RequestBody LoanRequest request) {
        return ResponseEntity.ok(loanService.updateLoan(currentUserProvider.getCurrentUserId(), id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        loanService.deleteLoan(currentUserProvider.getCurrentUserId(), id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/payments")
    public ResponseEntity<List<LoanPaymentResponse>> payments(@PathVariable Long id) {
        return ResponseEntity.ok(loanService.listPayments(currentUserProvider.getCurrentUserId(), id));
    }

    @PostMapping("/{id}/payments")
    public ResponseEntity<LoanPaymentResponse> addPayment(@PathVariable Long id, @Valid @RequestBody LoanPaymentRequest request) {
        LoanPaymentResponse response = loanService.addPayment(currentUserProvider.getCurrentUserId(), id, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
