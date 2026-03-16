package ru.bicev.finance_analytics.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "app_user")
@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_gen")
    @SequenceGenerator(
        name = "seq_gen",
        sequenceName = "transaction_id_seq",
        allocationSize = 50
    )
    private Long id;

    private String provider;

    private String providerId;

    private String email;

    private String name;

    private String avatarUrl;

    private LocalDateTime createdAt;

    private LocalDateTime lastLoginAt;

}
