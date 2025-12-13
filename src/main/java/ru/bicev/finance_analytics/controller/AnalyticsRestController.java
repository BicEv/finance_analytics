package ru.bicev.finance_analytics.controller;

import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.constraints.Min;
import ru.bicev.finance_analytics.dto.CategoryBudgetStatusDto;
import ru.bicev.finance_analytics.dto.CategoryExpenseDto;
import ru.bicev.finance_analytics.dto.DailyExpenseDto;
import ru.bicev.finance_analytics.dto.DateRange;
import ru.bicev.finance_analytics.dto.MonthlyExpenseDto;
import ru.bicev.finance_analytics.dto.RecurringForecastDto;
import ru.bicev.finance_analytics.dto.SummaryDto;
import ru.bicev.finance_analytics.dto.TopCategoryDto;
import ru.bicev.finance_analytics.service.AnalyticsService;

@Validated
@RestController
@RequestMapping("/api/analytics")
public class AnalyticsRestController {

    private final AnalyticsService analyticsService;

    public AnalyticsRestController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/categories")
    public ResponseEntity<List<CategoryExpenseDto>> getExpensesByCategory(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth month) {
        return ResponseEntity.ok(analyticsService.getExpensesByCategory(month));
    }

    @GetMapping("/categories/top")
    public ResponseEntity<List<TopCategoryDto>> getTopCategories(@RequestParam @Min(1) int limit,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth month) {
        return ResponseEntity.ok(analyticsService.getTopCategories(month, limit));
    }

    @GetMapping("/daily")
    public ResponseEntity<List<DailyExpenseDto>> getDailyExpenses(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth month) {
        return ResponseEntity.ok(analyticsService.getDailyExpenses(month));
    }

    @PostMapping("/monthly")
    public ResponseEntity<List<MonthlyExpenseDto>> getMonthlyExpenses(@RequestBody DateRange range) {
        return ResponseEntity.ok(analyticsService.getMonthlyExpenses(range));
    }

    @GetMapping("/summary")
    public ResponseEntity<SummaryDto> getSummary(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth month) {
        return ResponseEntity.ok(analyticsService.getSummary(month));
    }

    @GetMapping("/budget/{budgetId}")
    public ResponseEntity<CategoryBudgetStatusDto> getCategoryBudgetStatus(
            @PathVariable UUID budgetId) {
        return ResponseEntity.ok(analyticsService.getCategoryBudgetStatus(budgetId));
    }

    @GetMapping("/upcoming")
    public ResponseEntity<List<RecurringForecastDto>> getUpcomingRecurringPayments() {
        return ResponseEntity.ok(analyticsService.getUpcomingRecurringPayments());
    }

}
