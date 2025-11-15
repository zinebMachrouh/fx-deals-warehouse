package com.bloomberg.fxdeals.dtos.req;

public record FxDealReqDTO(String dealId,
                           String fromCurrency,
                           String toCurrency,
                           String dealTimestamp,
                           String dealAmount) {
}
