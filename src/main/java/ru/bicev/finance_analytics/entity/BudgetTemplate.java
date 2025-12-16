package ru.bicev.finance_analytics.entity;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BudgetTemplate {

    @Id
    private UUID id;

    @ManyToOne
    private User user;

    @ManyToOne
    private Category category;

    @Column(nullable = false)
    BigDecimal amount;

    @Column(nullable = false)
    boolean active;

    YearMonth startMonth;

}
