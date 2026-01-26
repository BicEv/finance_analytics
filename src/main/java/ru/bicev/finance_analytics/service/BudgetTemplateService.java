package ru.bicev.finance_analytics.service;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ru.bicev.finance_analytics.dto.BudgetTemplateDto;
import ru.bicev.finance_analytics.dto.BudgetTemplateRequest;
import ru.bicev.finance_analytics.dto.BudgetTemplateUpdateRequest;
import ru.bicev.finance_analytics.entity.BudgetTemplate;
import ru.bicev.finance_analytics.entity.Category;
import ru.bicev.finance_analytics.entity.User;
import ru.bicev.finance_analytics.exception.NotFoundException;
import ru.bicev.finance_analytics.repo.BudgetTemplateRepository;
import ru.bicev.finance_analytics.repo.CategoryRepository;

/**
 * Сервис по созданию правил для автоматического создания и продления бюджетов
 * пользователя
 */
@Service
public class BudgetTemplateService {

    private final BudgetTemplateRepository budgetTemplateRepository;
    private final CategoryRepository categoryRepository;
    private final UserService userService;

    private static final Logger logger = LoggerFactory.getLogger(BudgetTemplateService.class);
    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("MM.yyyy");

    public BudgetTemplateService(BudgetTemplateRepository budgetTemplateRepository,
            CategoryRepository categoryRepository, UserService userService) {
        this.budgetTemplateRepository = budgetTemplateRepository;
        this.categoryRepository = categoryRepository;
        this.userService = userService;
    }

    /**
     * Метод создает новое правило бюджета
     * 
     * @param request запрос, содержащий данные для создания правила бюджета
     * @return дто, содержащее данные созданного правила
     * @throws IllegalStateException если правило для данного пользователя и
     *                               категории уже существует
     * @throws NotFoundException     если категории с указанным в запросе
     *                               идентификатором не существует
     */
    @Transactional
    public BudgetTemplateDto createBudgetTemplate(BudgetTemplateRequest request) {
        User user = getCurrentUser();
        Category category = categoryRepository.findByIdAndUserId(request.categoryId(), user.getId())
                .orElseThrow(() -> new NotFoundException("Category not found"));

        if (budgetTemplateRepository.existsByUserIdAndCategoryId(user.getId(), category.getId())) {
            logger.warn("Duplicate budget template creation for category: {}; categoryId:{}", category.getName(),
                    category.getId());
            throw new IllegalStateException("Budget template already exists for this category");
        }

        BudgetTemplate template = BudgetTemplate.builder()
                .user(user)
                .category(category)
                .amount(request.amount())
                .active(request.active() != null ? request.active() : true)
                .startMonth(request.startMonth())
                .build();

        var savedTemplate = budgetTemplateRepository.save(template);
        logger.debug("Budget template created: {}", savedTemplate.getId());
        return toDto(savedTemplate);
    }

    /**
     * Метод возвращает правило бюджета по идентификатору
     * 
     * @param templateId идентификатор искомого правила
     * @return дто, содеражащее данные правила бюджета
     * @throws NotFoundException если правила с таким идентификатором не существует
     */
    public BudgetTemplateDto getBudgetTemplateById(UUID templateId) {
        var template = budgetTemplateRepository.findByIdAndUserId(templateId, getCurrentUserId())
                .orElseThrow(() -> new NotFoundException("Budget template not found"));
        logger.debug("Budget template found: {}", template.getId());
        return toDto(template);
    }

    /**
     * Метод возвращает все правила бюджета текущего пользователя
     * 
     * @return список всех правил-дто текущего пользователя
     */
    @Transactional(readOnly = true)
    public List<BudgetTemplateDto> findAllBudgetTemplatesForCurrentUser() {
        Long userId = getCurrentUserId();
        return budgetTemplateRepository.findAllByUserId(userId).stream().map(this::toDto).toList();
    }

    /**
     * Метод обновляет правило бюджета в соответствии с данными запроса на
     * обновление
     * 
     * @param templateId идентификатор обновляемого правила
     * @param request    запрос, содержащий данные, которые нужно обновить
     * @return дто, содежращее данные обновленного правила
     * @throws NotFoundException если правила с таким идентификатором не существует
     *                           или категории с идентификатором, указанным в
     *                           запросе не существует
     */
    @Transactional
    public BudgetTemplateDto updateBudgetTemplate(UUID templateId, BudgetTemplateUpdateRequest request) {
        Long userId = getCurrentUserId();
        var template = budgetTemplateRepository.findByIdAndUserId(templateId, userId)
                .orElseThrow(() -> new NotFoundException("Budget template not found"));

        if (request.categoryId() != null) {
            var category = categoryRepository.findByIdAndUserId(request.categoryId(), userId)
                    .orElseThrow(() -> new NotFoundException("Category not found"));
            template.setCategory(category);
        }

        if (request.amount() != null) {
            template.setAmount(request.amount());
        }

        if (request.startMonth() != null) {
            template.setStartMonth(request.startMonth());
        }

        if (request.active() != null) {
            template.setActive(request.active());
        }

        logger.debug("Budget template updated: {}", template.getId());
        return toDto(budgetTemplateRepository.save(template));

    }

    /**
     * Метод удаляет правило бюджета по его идентификатору
     * 
     * @param templateId идентификатор правила, подлежащего удалению
     * @throws NotFoundException если правила с таким идентификатором не существует
     */
    @Transactional
    public void deleteBudgetTemplate(UUID templateId) {
        var template = budgetTemplateRepository.findByIdAndUserId(templateId, getCurrentUserId())
                .orElseThrow(() -> new NotFoundException("Budget template not found"));
        logger.debug("Budget template deleted: {}", templateId);
        budgetTemplateRepository.delete(template);
    }

    /**
     * Служебный метод возвращающий текущего пользователя
     * 
     * @return текущий пользователь
     */
    private User getCurrentUser() {
        return userService.getCurrentUser();
    }

    /**
     * Служебный метод возвращающий идентификатор текущего пользователя
     * 
     * @return идентификатор текущего пользователя
     */
    private Long getCurrentUserId() {
        return userService.getCurrentUser().getId();
    }

    /**
     * Служебный метод для преобразования правила-сущности в правило-дто
     * 
     * @param budgetTemplate сущность для преобразования
     * @return преобразованное правило-дто
     */
    private BudgetTemplateDto toDto(BudgetTemplate budgetTemplate) {
        return new BudgetTemplateDto(
                budgetTemplate.getId(),
                budgetTemplate.getCategory().getId(),
                budgetTemplate.getCategory().getName(),
                budgetTemplate.getAmount(),
                budgetTemplate.isActive(),
                budgetTemplate.getStartMonth().format(FORMAT));
    }

}
