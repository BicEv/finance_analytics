package ru.bicev.finance_analytics.controller;

import java.net.URI;
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
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import jakarta.validation.Valid;
import ru.bicev.finance_analytics.dto.BudgetTemplateDto;
import ru.bicev.finance_analytics.dto.BudgetTemplateRequest;
import ru.bicev.finance_analytics.dto.BudgetTemplateUpdateRequest;
import ru.bicev.finance_analytics.service.BudgetTemplateService;

@RestController
@RequestMapping("/api/templates")
public class BudgetTemplateRestController {

    private final BudgetTemplateService budgetTemplateService;

    public BudgetTemplateRestController(BudgetTemplateService budgetTemplateService) {
        this.budgetTemplateService = budgetTemplateService;
    }

    @PostMapping
    public ResponseEntity<BudgetTemplateDto> createTemplate(@Valid @RequestBody BudgetTemplateRequest request) {
        BudgetTemplateDto created = budgetTemplateService.createBudgetTemplate(request);
        URI uri = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(uri).body(created);
    }

    @GetMapping("/{templateId}")
    public ResponseEntity<BudgetTemplateDto> getTemplateById(@PathVariable UUID templateId) {
        var template = budgetTemplateService.getBudgetTemplateById(templateId);
        return ResponseEntity.ok(template);
    }

    @GetMapping
    public ResponseEntity<List<BudgetTemplateDto>> getAllTemplates() {
        var templates = budgetTemplateService.findAllBudgetTemplatesForCurrentUser();
        return ResponseEntity.ok(templates);
    }

    @PutMapping("/{templateId}")
    public ResponseEntity<BudgetTemplateDto> updateTemplate(@PathVariable UUID templateId,
           @RequestBody BudgetTemplateUpdateRequest request) {
        var updated = budgetTemplateService.updateBudgetTemplate(templateId, request);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("{templateId}")
    public ResponseEntity<Void> deleteTemplate(@PathVariable UUID templateId) {
        budgetTemplateService.deleteBudgetTemplate(templateId);
        return ResponseEntity.noContent().build();
    }

}
