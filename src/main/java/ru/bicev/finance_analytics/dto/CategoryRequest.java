package ru.bicev.finance_analytics.dto;


import ru.bicev.finance_analytics.util.CategoryType;

public record CategoryRequest(String name, CategoryType type, String color) {

}
