package ru.bicev.finance_analytics.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Сервис для выполнения задач по расписанию
 */
@Service
public class ScheduledTasks {

    private final RecurringExecutionService recurringExecutionService;

    public ScheduledTasks(RecurringExecutionService recurringExecutionService) {
        this.recurringExecutionService = recurringExecutionService;
    }

    /**
     * Метод, который запускает выполнение задач по расписанию (каждый день в 1:00)
     */
    @Scheduled(cron = "0 0 1 * * *")
    public void processRecurringTransactions() {
        recurringExecutionService.executeDueTransactions();
    }

}
