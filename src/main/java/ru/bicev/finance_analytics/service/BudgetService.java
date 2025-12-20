package ru.bicev.finance_analytics.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ru.bicev.finance_analytics.dto.BudgetDto;
import ru.bicev.finance_analytics.dto.BudgetRequest;
import ru.bicev.finance_analytics.dto.BudgetUpdateRequest;
import ru.bicev.finance_analytics.entity.Budget;
import ru.bicev.finance_analytics.entity.Category;
import ru.bicev.finance_analytics.entity.User;
import ru.bicev.finance_analytics.exception.NotFoundException;
import ru.bicev.finance_analytics.repo.BudgetRepository;
import ru.bicev.finance_analytics.repo.CategoryRepository;

/**
 * Сервис для управления бюджетами пользователя
 */
@Service
public class BudgetService {

    private final BudgetRepository budgetRepository;
    private final CategoryRepository categoryRepository;
    private final UserService userService;

    private static final Logger logger = LoggerFactory.getLogger(BudgetService.class);

    public BudgetService(BudgetRepository budgetRepository, CategoryRepository categoryRepository,
            UserService userService) {
        this.budgetRepository = budgetRepository;
        this.categoryRepository = categoryRepository;
        this.userService = userService;
    }

    /**
     * Создает новый бюджет для текущего пользователя
     * 
     * @param request запрос, для создания нового бюджета
     * @return дто, содержащее идетнификатор бюджета, имя категории, идентификатор
     *         категории, месяц и лимит для созданного бюджета
     * @throws NotFoundException если в запросе передан идентификатор не
     *                           существующей категории
     */
    @Transactional
    public BudgetDto createBudget(BudgetRequest request) {
        User user = getCurrentUser();
        Category category = categoryRepository.findByIdAndUserId(request.categoryId(), user.getId())
                .orElseThrow(() -> new NotFoundException("Category not found"));

        logger.debug("createBudget() for user: {}", user.getId());
        Budget budget = Budget.builder()
                .user(user)
                .category(category)
                .month(request.month())
                .limitAmount(request.amount().setScale(2, RoundingMode.HALF_UP))
                .createdAt(LocalDateTime.now())
                .build();

        return toDto(budgetRepository.save(budget));
    }

    /**
     * Возвращает бюджет по его идентификатору
     * 
     * @param budgetId идентификатор бюджета
     * @return дто, содеражащее идетнификатор бюджета, имя категории, идентификатор
     *         категории, месяц и лимит найденного бюждета
     * @throws NotFoundException если бюджета с указанным идентификатором не
     *                           существует
     */
    public BudgetDto getBudgetById(UUID budgetId) {
        Budget budget = budgetRepository.findByIdAndUserId(budgetId, getCurrentUser().getId())
                .orElseThrow(() -> new NotFoundException("Budget not found"));
        logger.debug("getBudget() withid: {}", budgetId.toString());
        return toDto(budget);
    }

    /**
     * Возвращает все бюджеты для текущего пользователя
     * 
     * @return список всех дто бюджетов для текущего пользователя
     */
    @Transactional(readOnly = true)
    public List<BudgetDto> getAllBudgetsForUser() {
        logger.debug("getAllBudgetsForUser()");
        return budgetRepository.findAllByUserId(getCurrentUser().getId()).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Возвращает все бюджеты для текущего пользователя за указанный месяц
     * 
     * @param month месяц, за который идет поиск бюджетов
     * @return список всех дто бюджетов для текущего пользователя за указанный месяц
     */
    @Transactional(readOnly = true)
    public List<BudgetDto> getBudgetsForMonth(YearMonth month) {
        logger.debug("getBudgetsForMonth(): {}", month.toString());
        return budgetRepository.findByUserIdAndMonth(getCurrentUser().getId(), month).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Изменяет месяц и лимит для указанного бюджета
     * 
     * @param budgetId идентификатор изменяемого бюджета
     * @param request  запрос с данными для изменения бюджета
     * @return дто, содержащее данные измененного бюджета
     * @throws NotFoundException если бюджета с указанным идентификатором не
     *                           существует
     */
    @Transactional
    public BudgetDto updateBudget(UUID budgetId, BudgetUpdateRequest request) {
        Budget budget = budgetRepository.findByIdAndUserId(budgetId, getCurrentUser().getId())
                .orElseThrow(() -> new NotFoundException("Budget not found"));
        logger.debug("updateBudget() with id: {}", budgetId.toString());
        if (request.categoryId() != null) {
            Category category = categoryRepository.findByIdAndUserId(request.categoryId(), getCurrentUser().getId())
                    .orElseThrow(() -> new NotFoundException("Category not found"));
            budget.setCategory(category);
        }
        if (request.month() != null) {
            budget.setMonth(request.month());
        }
        if (request.limitAmount() != null) {
            budget.setLimitAmount(request.limitAmount().setScale(2, RoundingMode.HALF_UP));
        }
        return toDto(budgetRepository.save(budget));
    }

    /**
     * Удаляет бюджет с указанным идентификатором
     * 
     * @param budgetId идентификатор бюджета, подлежащего удалению
     * @throws NotFoundException если бюджета с указанным идентификатором не
     *                           существует
     */
    @Transactional
    public void deleteBudget(UUID budgetId) {
        Budget budget = budgetRepository.findByIdAndUserId(budgetId, getCurrentUser().getId())
                .orElseThrow(() -> new NotFoundException("Budget not found"));
        logger.debug("deleteBudget() with id: {}", budgetId.toString());
        budgetRepository.delete(budget);
    }

    /**
     * Метод для создания бюджетов для указанного пользователя и категории
     * 
     * @param user     пользователь для которого создается бюджет
     * @param category категория бюджета
     * @param amount   лимит расходов для бюджета
     * @param month    месяц бюджета
     */
    @Transactional
    public void createBudgetForCategoryAndUser(User user, Category category, BigDecimal amount, YearMonth month) {
        if (budgetRepository.existsByUserIdAndCategoryIdAndMonth(user.getId(), category.getId(), month)) {
            return;
        }

        Budget budget = Budget.builder()
                .user(user)
                .category(category)
                .limitAmount(amount)
                .month(month)
                .createdAt(LocalDateTime.now())
                .build();

        budgetRepository.save(budget);
    }

    /**
     * Служебный метод, который получает теущего пользователя
     * 
     * @return текущий пользователь
     */
    private User getCurrentUser() {
        return userService.getCurrentUser();
    }

    /**
     * Служебный метод для преобразования сущности бюджета в дто бюджета
     * 
     * @param budget сущность бюджета, которая должна быть перобразована
     * @return дто бюджета, которое будет отдано пользователю
     */
    private BudgetDto toDto(Budget budget) {
        return new BudgetDto(
                budget.getId(),
                budget.getCategory().getId(),
                budget.getCategory().getName(),
                budget.getMonth(),
                budget.getLimitAmount());
    }

}
