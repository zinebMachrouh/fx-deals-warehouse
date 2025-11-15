package com.bloomberg.fxdeals.dtos;

public record FxDealRequestDTO(String dealId,
                               String fromCurrency,
                               String toCurrency,
                               String dealTimestamp,
                               String dealAmount) {
}
