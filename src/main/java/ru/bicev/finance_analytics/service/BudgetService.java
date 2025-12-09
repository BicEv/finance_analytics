package ru.bicev.finance_analytics.service;

import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ru.bicev.finance_analytics.dto.BudgetDto;
import ru.bicev.finance_analytics.dto.BudgetRequest;
import ru.bicev.finance_analytics.entity.Budget;
import ru.bicev.finance_analytics.entity.Category;
import ru.bicev.finance_analytics.entity.User;
import ru.bicev.finance_analytics.exception.NotFoundException;
import ru.bicev.finance_analytics.repo.BudgetRepository;
import ru.bicev.finance_analytics.repo.CategoryRepository;

@Service
public class BudgetService {

    private final BudgetRepository budgetRepository;
    private final CategoryRepository categoryRepository;
    private final UserService userService;

    public BudgetService(BudgetRepository budgetRepository, CategoryRepository categoryRepository,
            UserService userService) {
        this.budgetRepository = budgetRepository;
        this.categoryRepository = categoryRepository;
        this.userService = userService;
    }

    @Transactional
    public BudgetDto createBudget(BudgetRequest request) {
        User user = getCurrentUser();
        Category category = categoryRepository.findByIdAndUserId(request.categoryId(), user.getId())
                .orElseThrow(() -> new NotFoundException("Category not found"));

        Budget budget = Budget.builder()
                .user(user)
                .category(category)
                .month(request.month())
                .limitAmount(request.amount().setScale(2, RoundingMode.HALF_UP))
                .createdAt(LocalDateTime.now())
                .build();

        return toDto(budgetRepository.save(budget));
    }

    public BudgetDto getBudgetById(UUID budgetId) {
        Budget budget = budgetRepository.findByIdAndUserId(budgetId, getCurrentUser().getId())
                .orElseThrow(() -> new NotFoundException("Budget not found"));
        return toDto(budget);
    }

    public List<BudgetDto> getAllBudgetsForUser() {
        return budgetRepository.findAllByUserId(getCurrentUser().getId()).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public List<BudgetDto> getBudgetsForMonth(YearMonth month) {
        return budgetRepository.findByUserIdAndMonth(getCurrentUser().getId(), month).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public BudgetDto updateBudget(UUID budgetId, BudgetRequest request) {
        Budget budget = budgetRepository.findByIdAndUserId(budgetId, getCurrentUser().getId())
                .orElseThrow(() -> new NotFoundException("Budget not found"));

        budget.setMonth(request.month());
        budget.setLimitAmount(request.amount().setScale(2, RoundingMode.HALF_UP));
        return toDto(budgetRepository.save(budget));
    }

    @Transactional
    public void deleteBudget(UUID budgetId) {
        Budget budget = budgetRepository.findByIdAndUserId(budgetId, getCurrentUser().getId())
                .orElseThrow(() -> new NotFoundException("Budget not found"));
        budgetRepository.delete(budget);
    }

    private User getCurrentUser() {
        return userService.getCurrentUser();
    }

    private BudgetDto toDto(Budget budget) {
        return new BudgetDto(
                budget.getId(),
                budget.getCategory().getId(),
                budget.getCategory().getName(),
                budget.getMonth(),
                budget.getLimitAmount());
    }

}
