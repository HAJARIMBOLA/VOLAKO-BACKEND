package com.volako.backend.controller;

import com.volako.backend.dto.goal.GoalContributionRequest;
import com.volako.backend.dto.goal.GoalRequest;
import com.volako.backend.dto.goal.GoalResponse;
import com.volako.backend.security.CurrentUserProvider;
import com.volako.backend.service.GoalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/goals")
@RequiredArgsConstructor
public class GoalController {

    private final GoalService goalService;
    private final CurrentUserProvider currentUserProvider;

    @GetMapping
    public ResponseEntity<List<GoalResponse>> list() {
        return ResponseEntity.ok(goalService.listGoals(currentUserProvider.getCurrentUserId()));
    }

    @PostMapping
    public ResponseEntity<GoalResponse> create(@Valid @RequestBody GoalRequest request) {
        GoalResponse response = goalService.createGoal(currentUserProvider.getCurrentUserId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<GoalResponse> update(@PathVariable Long id, @Valid @RequestBody GoalRequest request) {
        return ResponseEntity.ok(goalService.updateGoal(currentUserProvider.getCurrentUserId(), id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        goalService.deleteGoal(currentUserProvider.getCurrentUserId(), id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/contributions")
    public ResponseEntity<GoalResponse> addContribution(@PathVariable Long id, @Valid @RequestBody GoalContributionRequest request) {
        GoalResponse response = goalService.addContribution(currentUserProvider.getCurrentUserId(), id, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
