package ru.bicev.finance_analytics.controller;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
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

import jakarta.validation.Valid;
import ru.bicev.finance_analytics.dto.RecurringTransactionDto;
import ru.bicev.finance_analytics.dto.RecurringTransactionRequest;
import ru.bicev.finance_analytics.service.RecurringTransactionService;

@Validated
@RestController
@RequestMapping("/api/recurring")
public class RecurringTransactionRestController {

    private final RecurringTransactionService service;

    public RecurringTransactionRestController(RecurringTransactionService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<RecurringTransactionDto> createTransaction(
            @RequestBody @Valid RecurringTransactionRequest request) {
        var created = service.createTransaction(request);
        URI uri = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();

        return ResponseEntity.created(uri).body(created);

    }

    @GetMapping
    public ResponseEntity<List<RecurringTransactionDto>> getTransactions(
            @RequestParam(required = false) LocalDate date) {
        List<RecurringTransactionDto> transactions;
        if (date != null) {
            transactions = service.getAllRecurringTransactionsAndDate(date);
        } else {
            transactions = service.getAllRecurringTransactions();
        }
        return ResponseEntity.ok(transactions);
    }

    @GetMapping("/{transactionId}")
    public ResponseEntity<RecurringTransactionDto> getTransactionById(@PathVariable UUID transactionId) {
        var transaction = service.getTransactionById(transactionId);
        return ResponseEntity.ok(transaction);
    }

    @PutMapping("/{transactionId}")
    public ResponseEntity<RecurringTransactionDto> updateTransaction(@PathVariable UUID transactionId,
            @RequestBody RecurringTransactionRequest request) {
        var updated = service.updateTransaction(transactionId, request);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{transactionId}")
    public ResponseEntity<Void> deleteTransaction(@PathVariable UUID transactionId) {
        service.deleteTransaction(transactionId);
        return ResponseEntity.noContent().build();
    }

}
