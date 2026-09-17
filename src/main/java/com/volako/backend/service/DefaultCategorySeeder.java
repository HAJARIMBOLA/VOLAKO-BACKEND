package com.volako.backend.service;

import com.volako.backend.domain.Category;
import com.volako.backend.domain.User;
import com.volako.backend.domain.enums.TransactionType;
import com.volako.backend.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/** Seeds the default system categories for a newly registered user. */
@Component
@RequiredArgsConstructor
public class DefaultCategorySeeder {

    public static final String DEBT_REPAYMENT_MADE_CATEGORY = "Remboursement effectué";
    public static final String DEBT_REPAYMENT_RECEIVED_CATEGORY = "Remboursement reçu";

    private static final List<String> DEFAULT_EXPENSE_CATEGORIES =
            List.of("Nourriture", "Transport", "Logement", "Loisirs", DEBT_REPAYMENT_MADE_CATEGORY);
    private static final List<String> DEFAULT_INCOME_CATEGORIES =
            List.of("Salaire", "Freelance", DEBT_REPAYMENT_RECEIVED_CATEGORY);

    private final CategoryRepository categoryRepository;

    public void seedFor(User user) {
        DEFAULT_EXPENSE_CATEGORIES.forEach(name -> categoryRepository.save(systemCategory(user, name, TransactionType.EXPENSE)));
        DEFAULT_INCOME_CATEGORIES.forEach(name -> categoryRepository.save(systemCategory(user, name, TransactionType.INCOME)));
    }

    private Category systemCategory(User user, String name, TransactionType type) {
        return Category.builder()
                .user(user)
                .name(name)
                .type(type)
                .system(true)
                .active(true)
                .build();
    }
}
