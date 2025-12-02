package ru.bicev.finance_analytics.dto;

import java.util.UUID;

import ru.bicev.finance_analytics.util.CategoryType;

public record CategoryRequest(UUID accountId, String name, CategoryType type, String color) {

}
