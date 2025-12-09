package ru.bicev.finance_analytics.dto;

import java.util.UUID;

public record AccountDto(UUID id, String name, String currency) {

}
