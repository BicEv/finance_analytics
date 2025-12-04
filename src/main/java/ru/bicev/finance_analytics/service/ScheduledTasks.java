package ru.bicev.finance_analytics.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class ScheduledTasks {

    private final RecurringExecutionService recurringExecutionService;

    public ScheduledTasks(RecurringExecutionService recurringExecutionService) {
        this.recurringExecutionService = recurringExecutionService;
    }

    @Scheduled(cron = "0 0 1 * * *")
    public void processRecurringTransactions() {
        recurringExecutionService.executeDueTransactions();
    }

}
