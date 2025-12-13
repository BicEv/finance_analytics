package ru.bicev.finance_analytics.dto;

import java.util.List;

public record ValidationErrorResponse(List<FieldErrorDto> errors) {

}
