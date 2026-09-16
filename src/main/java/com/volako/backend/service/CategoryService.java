package com.volako.backend.service;

import com.volako.backend.domain.Category;
import com.volako.backend.domain.User;
import com.volako.backend.dto.category.CategoryRequest;
import com.volako.backend.dto.category.CategoryResponse;
import com.volako.backend.exception.BadRequestException;
import com.volako.backend.exception.ResourceNotFoundException;
import com.volako.backend.repository.CategoryRepository;
import com.volako.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<CategoryResponse> listCategories(Long userId) {
        return categoryRepository.findByUserIdOrderByNameAsc(userId).stream()
                .map(CategoryResponse::from)
                .toList();
    }

    @Transactional
    public CategoryResponse createCategory(Long userId, CategoryRequest request) {
        if (categoryRepository.existsByUserIdAndNameIgnoreCaseAndType(userId, request.name(), request.type())) {
            throw new BadRequestException("CATEGORY_ALREADY_EXISTS", "Une catégorie avec ce nom existe déjà pour ce type");
        }

        User user = userRepository.getReferenceById(userId);
        Category category = Category.builder()
                .user(user)
                .name(request.name())
                .type(request.type())
                .system(false)
                .active(true)
                .build();
        category = categoryRepository.save(category);

        return CategoryResponse.from(category);
    }

    @Transactional
    public CategoryResponse updateCategory(Long userId, Long categoryId, CategoryRequest request) {
        Category category = getOwnedCategory(userId, categoryId);
        category.setName(request.name());
        category.setType(request.type());
        category = categoryRepository.save(category);

        return CategoryResponse.from(category);
    }

    /** System categories can never be removed, only deactivated; custom categories follow the same
     *  soft-delete rule to keep historical transactions pointing at a valid category. */
    @Transactional
    public void deactivateCategory(Long userId, Long categoryId) {
        Category category = getOwnedCategory(userId, categoryId);
        category.setActive(false);
        categoryRepository.save(category);
    }

    private Category getOwnedCategory(Long userId, Long categoryId) {
        return categoryRepository.findByIdAndUserId(categoryId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Catégorie introuvable"));
    }
}
