package ru.bicev.finance_analytics.service;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    public Budget createBudget(BudgetRequest request) {
        User user = getCurrentUser();
        Category category = categoryRepository.findByIdAndUserId(request.categoryId(), user.getId())
                .orElseThrow(() -> new NotFoundException("Category not found"));

        Budget budget = Budget.builder()
                .user(user)
                .category(category)
                .month(request.month())
                .limitAmount(request.amount())
                .createdAt(LocalDateTime.now())
                .build();

        return budgetRepository.save(budget);
    }

    public Budget getBudgetById(UUID budgetId) {
        return budgetRepository.findByIdAndUserId(budgetId, getCurrentUser().getId())
                .orElseThrow(() -> new NotFoundException("Budget not found"));
    }

    public List<Budget> getAllBudgetsForUser() {
        return budgetRepository.findAllByUserId(getCurrentUser().getId());
    }

    public List<Budget> getBudgetsForMonth(YearMonth month) {
        return budgetRepository.findByUserIdAndMonth(getCurrentUser().getId(), month);
    }

    @Transactional
    public Budget updateBudget(UUID budgetId, BudgetRequest request) {
        Budget budget = budgetRepository.findByIdAndUserId(budgetId, getCurrentUser().getId())
                .orElseThrow(() -> new NotFoundException("Budget not found"));

        budget.setMonth(request.month());
        budget.setLimitAmount(request.amount());
        return budgetRepository.save(budget);
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

}
