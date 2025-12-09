package ru.bicev.finance_analytics.dto;

import java.util.UUID;

public record CategoryDto(UUID id, UUID accountId, String accountName, String name, String type, String color) {

}
