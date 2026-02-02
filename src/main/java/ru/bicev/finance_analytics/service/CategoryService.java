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

/**
 * Сервис для управлениями категориями пользователя
 */
@Service
public class CategoryService {

    private final UserService userService;
    private final CategoryRepository categoryRepository;

    private static final Logger logger = LoggerFactory.getLogger(CategoryService.class);

    public CategoryService(UserService userService, CategoryRepository categoryRepository) {
        this.userService = userService;
        this.categoryRepository = categoryRepository;
    }

    /**
     * Создает новую каетгорию для текущего пользователя
     * 
     * @param request запрос, содержащий данные, необходимые для создания новой
     *                категории
     * @return дто, содержащее данные созданной категории
     */
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

    /**
     * Возвращает все категории для текущего пользователя
     * 
     * @return список всех категорий текущего пользователя
     */
    public List<CategoryDto> getUserCategories() {
        Long userId = getCurrentUserId();
        logger.debug("getUserCategories() for user: {}", userId);
        return categoryRepository.findAllByUserId(userId)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Возвращает категорию по ее идентификатору
     * 
     * @param categoryId идентификатор искомой категории
     * @return дто, содержащее данные найденной категории
     * @throws NotFoundException если категории с таким идентификатором не
     *                           существует
     */
    public CategoryDto getCategoryById(UUID categoryId) {
        Category category = categoryRepository.findByIdAndUserId(categoryId, getCurrentUserId())
                .orElseThrow(() -> new NotFoundException("Category not found"));
        logger.debug("getCategoryById() with id: {}", categoryId);
        return toDto(category);
    }

    /**
     * Изменяет имя, тип, и цвет категории по ее идентификатору
     * 
     * @param categoryId идентификатор категории, подлежащей изменению
     * @param request    запрос с данными для изменения категории
     * @return дто, содержащее данные измененной категории
     * @throws NotFoundException если категории с таким идентификатором не
     *                           существует
     */
    @Transactional
    public CategoryDto updateCategory(UUID categoryId, CategoryRequest request) {
        Category category = categoryRepository.findByIdAndUserId(categoryId, getCurrentUserId())
                .orElseThrow(() -> new NotFoundException("Category not found"));

        category.setName(request.name());
        category.setType(request.type());
        category.setColor(request.color());
        logger.debug("updateCategory() with id: {}", categoryId);
        return toDto(categoryRepository.save(category));
    }

    /**
     * Удаляет категорию по ее идентификатору
     * 
     * @param categoryId идентфикатор категории, подлежащей удалению
     * @throws NotFoundException если категории с таким идентификатором не
     *                           существует
     */
    @Transactional
    public void deleteCategory(UUID categoryId) {
        Category category = categoryRepository.findByIdAndUserId(categoryId, getCurrentUserId())
                .orElseThrow(() -> new NotFoundException("Category not found"));
        logger.debug("deleteCategory() with id: {}", categoryId);
        categoryRepository.delete(category);
    }

    /**
     * Возвращает все категории с указанным типом
     * 
     * @param type тип искомых категорий
     * @return список всех категорий с указанным типом
     */
    public List<CategoryDto> getCategoriesByType(CategoryType type) {
        return categoryRepository.findAllByUserIdAndType(getCurrentUserId(), type).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Возвращает все категории для текущего пользователя
     * 
     * @return список всех категорий для текущего пользователя
     */
    public List<CategoryDto> getAllCategoriesForUser() {
        return categoryRepository.findAllByUserId(getCurrentUserId()).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Служебный метод, возвращающий идентификатор текущего пользователя
     * 
     * @return идентификатор текущего пользователя
     */
    private Long getCurrentUserId() {
        return userService.getCurrentUserId();
    }

    /**
     * Служебный метод, возвращающий текущего пользователя
     * 
     * @return текущий пользователь
     */
    private User getCurrentUser() {
        return userService.getCurrentUser();
    }

    /**
     * Служебный метод преобразующий категорию сущность в категорию дто
     * 
     * @param category категория для преобразования
     * @return преобразованная категория дто
     */
    private CategoryDto toDto(Category category) {
        return new CategoryDto(
                category.getId(),
                category.getName(),
                category.getType().name(),
                category.getColor());
    }

}
