package ru.bicev.finance_analytics.service;

import java.time.YearMonth;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import ru.bicev.finance_analytics.entity.BudgetTemplate;
import ru.bicev.finance_analytics.repo.BudgetTemplateRepository;

/**
 * Сервис для выполнения задач по расписанию
 */
@Service
public class ScheduledTasks {

    private final RecurringExecutionService recurringExecutionService;
    private final BudgetService budgetService;
    private final BudgetTemplateRepository budgetTemplateRepository;

    public ScheduledTasks(RecurringExecutionService recurringExecutionService, BudgetService budgetService,
            BudgetTemplateRepository budgetTemplateRepository) {
        this.recurringExecutionService = recurringExecutionService;
        this.budgetService = budgetService;
        this.budgetTemplateRepository = budgetTemplateRepository;
    }

    /**
     * Метод, который запускает выполнение задач по расписанию (каждый день в 1:00)
     */
    @Scheduled(cron = "0 0 1 * * *")
    public void processRecurringTransactions() {
        recurringExecutionService.executeDueTransactions();
    }

    /**
     * Метод который запускает выполнение задач (первого числа каждого месяца в
     * 0:00)
     */
    @Scheduled(cron = "0 0 0 1 * *")
    public void generateMonthlyBudgets() {
        YearMonth currentMonth = YearMonth.now();

        List<BudgetTemplate> templates = budgetTemplateRepository.findActiveTrue();

        for (BudgetTemplate template : templates) {
            budgetService.createBudgetForCategoryAndUser(
                    template.getUser(),
                    template.getCategory(),
                    template.getAmount(),
                    currentMonth);
        }
    }

}
