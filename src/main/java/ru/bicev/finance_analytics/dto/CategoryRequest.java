package ru.bicev.finance_analytics.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import ru.bicev.finance_analytics.util.CategoryType;

public record CategoryRequest(@NotEmpty(message = "Category must have a name") String name,
        @NotNull(message = "Category type cannot be null") CategoryType type,
        String color) {

}
