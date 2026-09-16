package com.volako.backend.service;

import com.volako.backend.domain.Category;
import com.volako.backend.domain.User;
import com.volako.backend.domain.enums.TransactionType;
import com.volako.backend.repository.CategoryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DefaultCategorySeederTest {

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private DefaultCategorySeeder seeder;

    @Test
    void seedsSixSystemCategoriesOnRegistration() {
        User user = User.builder().id(1L).email("nija@volako.mg").build();

        seeder.seedFor(user);

        ArgumentCaptor<Category> captor = ArgumentCaptor.forClass(Category.class);
        verify(categoryRepository, times(6)).save(captor.capture());

        List<Category> saved = captor.getAllValues();
        assertThat(saved).allMatch(Category::isSystem);
        assertThat(saved).allMatch(Category::isActive);
        assertThat(saved).allMatch(category -> category.getUser() == user);

        assertThat(saved).filteredOn(c -> c.getType() == TransactionType.EXPENSE)
                .extracting(Category::getName)
                .containsExactlyInAnyOrder("Nourriture", "Transport", "Logement", "Loisirs");

        assertThat(saved).filteredOn(c -> c.getType() == TransactionType.INCOME)
                .extracting(Category::getName)
                .containsExactlyInAnyOrder("Salaire", "Freelance");
    }
}
