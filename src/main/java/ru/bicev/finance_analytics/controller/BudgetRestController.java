package ru.bicev.finance_analytics.controller;

import java.net.URI;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import ru.bicev.finance_analytics.dto.BudgetDto;
import ru.bicev.finance_analytics.dto.BudgetRequest;
import ru.bicev.finance_analytics.service.BudgetService;

@RestController
@RequestMapping("/api/budgets")
public class BudgetRestController {

    private final BudgetService budgetService;

    public BudgetRestController(BudgetService budgetService) {
        this.budgetService = budgetService;
    }

    @PostMapping
    public ResponseEntity<BudgetDto> createBudget(@RequestBody BudgetRequest request) {
        BudgetDto created = budgetService.createBudget(request);
        URI uri = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(uri).body(created);
    }

    @GetMapping("/{budgetId}")
    public ResponseEntity<BudgetDto> getBudgetById(@PathVariable UUID budgetId) {
        BudgetDto budget = budgetService.getBudgetById(budgetId);
        return ResponseEntity.ok(budget);
    }

    @GetMapping
    public ResponseEntity<List<BudgetDto>> getAllUsersBudgets(@RequestParam(required = false) YearMonth yearMonth) {
        List<BudgetDto> budgets;
        if (yearMonth != null) {
            budgets = budgetService.getBudgetsForMonth(yearMonth);
        } else {
            budgets = budgetService.getAllBudgetsForUser();
        }
        return ResponseEntity.ok(budgets);
    }

    @PutMapping("/{budgetId}")
    public ResponseEntity<BudgetDto> updateBudget(@PathVariable UUID budgetId, @RequestBody BudgetRequest request) {
        BudgetDto updated = budgetService.updateBudget(budgetId, request);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{budgetId}")
    public ResponseEntity<Void> deleteBudget(@PathVariable UUID budgetId) {
        budgetService.deleteBudget(budgetId);
        return ResponseEntity.noContent().build();
    }

}
