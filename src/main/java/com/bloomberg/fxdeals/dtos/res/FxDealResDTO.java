package com.bloomberg.fxdeals.dtos.res;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record FxDealResDTO(String dealId,
                           String fromCurrency,
                           String toCurrency,
                           LocalDateTime dealTimestamp,
                           BigDecimal dealAmount) {
}
