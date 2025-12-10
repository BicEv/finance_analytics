package ru.bicev.finance_analytics.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.bicev.finance_analytics.util.Frequency;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class RecurringTransaction {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    private Frequency frequency;

    private LocalDate nextExecutionDate;

    private String description;

    private LocalDateTime createdAt;

    private boolean isActive;

    private LocalDate lastExecutionDate;

}
