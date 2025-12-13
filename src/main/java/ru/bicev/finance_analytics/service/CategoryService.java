package ru.bicev.finance_analytics.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ru.bicev.finance_analytics.dto.CategoryDto;
import ru.bicev.finance_analytics.dto.CategoryRequest;
import ru.bicev.finance_analytics.entity.Category;
import ru.bicev.finance_analytics.entity.User;
import ru.bicev.finance_analytics.exception.NotFoundException;
import ru.bicev.finance_analytics.repo.CategoryRepository;
import ru.bicev.finance_analytics.util.CategoryType;

@Service
public class CategoryService {

    private final UserService userService;
    private final CategoryRepository categoryRepository;

    private static final Logger logger = LoggerFactory.getLogger(CategoryService.class);

    public CategoryService(UserService userService, CategoryRepository categoryRepository) {
        this.userService = userService;
        this.categoryRepository = categoryRepository;
    }

    @Transactional
    public CategoryDto createCategory(CategoryRequest request) {
        User user = getCurrentUser();

        Category category = Category.builder()
                .user(user)
                .name(request.name())
                .type(request.type())
                .color(request.color())
                .createdAt(LocalDateTime.now())
                .build();
        logger.debug("createCategory() for user: {}", user.getId());
        return toDto(categoryRepository.save(category));

    }

    public List<CategoryDto> getUserCategories() {
        Long userId = getUserId();
        logger.debug("getUserCategories() for user: {}", userId);
        return categoryRepository.findAllByUserId(userId)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public CategoryDto getCategoryById(UUID categoryId) {
        Category category = categoryRepository.findByIdAndUserId(categoryId, getUserId())
                .orElseThrow(() -> new NotFoundException("Category not found"));
        logger.debug("getCategoryById() with id: {}", categoryId);
        return toDto(category);
    }

    @Transactional
    public CategoryDto updateCategory(UUID categoryId, CategoryRequest request) {
        Category category = categoryRepository.findByIdAndUserId(categoryId, getUserId())
                .orElseThrow(() -> new NotFoundException("Category not found"));

        category.setName(request.name());
        category.setType(request.type());
        category.setColor(request.color());
        logger.debug("updateCategory() with id: {}", categoryId);
        return toDto(categoryRepository.save(category));
    }

    @Transactional
    public void deleteCategory(UUID categoryId) {
        Category category = categoryRepository.findByIdAndUserId(categoryId, getUserId())
                .orElseThrow(() -> new NotFoundException("Category not found"));
        logger.debug("deleteCategory() with id: {}", categoryId);
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
                category.getName(),
                category.getType().name(),
                category.getColor());
    }

}
