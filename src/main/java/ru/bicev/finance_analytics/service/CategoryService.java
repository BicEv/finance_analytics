package ru.bicev.finance_analytics.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ru.bicev.finance_analytics.dto.CategoryDto;
import ru.bicev.finance_analytics.dto.CategoryRequest;
import ru.bicev.finance_analytics.entity.Account;
import ru.bicev.finance_analytics.entity.Category;
import ru.bicev.finance_analytics.entity.User;
import ru.bicev.finance_analytics.exception.NotFoundException;
import ru.bicev.finance_analytics.repo.AccountRepository;
import ru.bicev.finance_analytics.repo.CategoryRepository;
import ru.bicev.finance_analytics.util.CategoryType;

@Service
public class CategoryService {

    private final UserService userService;
    private final CategoryRepository categoryRepository;
    private final AccountRepository accountRepository;

    public CategoryService(UserService userService, CategoryRepository categoryRepository,
            AccountRepository accountRepository) {
        this.userService = userService;
        this.categoryRepository = categoryRepository;
        this.accountRepository = accountRepository;
    }

    @Transactional
    public CategoryDto createCategory(CategoryRequest request) {
        User user = getCurrentUser();

        Account account = accountRepository.findByIdAndUserId(request.accountId(), user.getId())
                .orElseThrow(() -> new NotFoundException("Account not found"));

        Category category = Category.builder()
                .user(user)
                .account(account)
                .name(request.name())
                .type(request.type())
                .color(request.color())
                .createdAt(LocalDateTime.now())
                .build();

        return toDto(categoryRepository.save(category));

    }

    public List<CategoryDto> getUserCategories(UUID accountId) {
        Long userId = getUserId();
        if (accountId == null) {
            return categoryRepository.findAllByUserId(userId).stream()
                    .map(this::toDto)
                    .collect(Collectors.toList());
        }
        return categoryRepository.findAllByUserIdAndAccountId(userId, accountId).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public CategoryDto getCategoryById(UUID categoryId) {
        Category category = categoryRepository.findByIdAndUserId(categoryId, getUserId())
                .orElseThrow(() -> new NotFoundException("Category not found"));
        return toDto(category);
    }

    @Transactional
    public CategoryDto updateCategory(UUID categoryId, CategoryRequest request) {
        Category category = categoryRepository.findByIdAndUserId(categoryId, getUserId())
                .orElseThrow(() -> new NotFoundException("Category not found"));

        category.setName(request.name());
        category.setColor(request.color());

        return toDto(categoryRepository.save(category));
    }

    @Transactional
    public void deleteCategory(UUID categoryId) {
        Category category = categoryRepository.findByIdAndUserId(categoryId, getUserId())
                .orElseThrow(() -> new NotFoundException("Category not found"));

        categoryRepository.delete(category);
    }

    public List<CategoryDto> getCategoriesByType(CategoryType type) {
        return categoryRepository.findAllByUserIdAndType(getUserId(), type).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public List<CategoryDto> getAllCategoriesForUser() {
        return categoryRepository.findAllByUserId(getUserId()).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    private Long getUserId() {
        return getCurrentUser().getId();
    }

    private User getCurrentUser() {
        return userService.getCurrentUser();
    }

    private CategoryDto toDto(Category category) {
        return new CategoryDto(
                category.getId(),
                category.getAccount().getId(),
                category.getAccount().getName(),
                category.getName(),
                category.getType().name(),
                category.getColor());
    }

}
