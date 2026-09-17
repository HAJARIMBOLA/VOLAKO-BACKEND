package com.volako.backend.service;

import com.volako.backend.domain.Account;
import com.volako.backend.domain.FinancialGoal;
import com.volako.backend.domain.GoalContribution;
import com.volako.backend.domain.User;
import com.volako.backend.dto.goal.GoalContributionRequest;
import com.volako.backend.dto.goal.GoalRequest;
import com.volako.backend.dto.goal.GoalResponse;
import com.volako.backend.exception.ResourceNotFoundException;
import com.volako.backend.repository.AccountRepository;
import com.volako.backend.repository.FinancialGoalRepository;
import com.volako.backend.repository.GoalContributionRepository;
import com.volako.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/** "Fonds" — a savings tracker. Contributions are a separate ledger from account
 *  transactions: they record progress toward a target without moving money between
 *  accounts, since the cash was typically already set aside informally. */
@Service
@RequiredArgsConstructor
public class GoalService {

    private final FinancialGoalRepository goalRepository;
    private final GoalContributionRepository contributionRepository;
    private final AccountRepository accountRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<GoalResponse> listGoals(Long userId) {
        return goalRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(goal -> GoalResponse.from(goal, contributionRepository.sumAmountByGoalId(goal.getId())))
                .toList();
    }

    @Transactional
    public GoalResponse createGoal(Long userId, GoalRequest request) {
        User user = userRepository.getReferenceById(userId);
        FinancialGoal goal = FinancialGoal.builder()
                .user(user)
                .name(request.name())
                .targetAmount(request.targetAmount())
                .targetDate(request.targetDate())
                .build();
        goal = goalRepository.save(goal);

        return GoalResponse.from(goal, BigDecimal.ZERO);
    }

    @Transactional
    public GoalResponse updateGoal(Long userId, Long goalId, GoalRequest request) {
        FinancialGoal goal = getOwnedGoal(userId, goalId);
        goal.setName(request.name());
        goal.setTargetAmount(request.targetAmount());
        goal.setTargetDate(request.targetDate());
        goal = goalRepository.save(goal);

        return GoalResponse.from(goal, contributionRepository.sumAmountByGoalId(goal.getId()));
    }

    @Transactional
    public void deleteGoal(Long userId, Long goalId) {
        FinancialGoal goal = getOwnedGoal(userId, goalId);
        goalRepository.delete(goal);
    }

    @Transactional
    public GoalResponse addContribution(Long userId, Long goalId, GoalContributionRequest request) {
        FinancialGoal goal = getOwnedGoal(userId, goalId);
        Account account = accountRepository.findByIdAndUserId(request.accountId(), userId)
                .orElseThrow(() -> new ResourceNotFoundException("Compte introuvable"));

        GoalContribution contribution = GoalContribution.builder()
                .goal(goal)
                .account(account)
                .amount(request.amount())
                .contributionDate(request.contributionDate())
                .build();
        contributionRepository.save(contribution);

        return GoalResponse.from(goal, contributionRepository.sumAmountByGoalId(goal.getId()));
    }

    private FinancialGoal getOwnedGoal(Long userId, Long goalId) {
        return goalRepository.findByIdAndUserId(goalId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Fonds introuvable"));
    }
}
