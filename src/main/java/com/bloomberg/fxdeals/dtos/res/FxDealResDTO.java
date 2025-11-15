package com.bloomberg.fxdeals.dtos.res;

import java.math.BigInteger;
import java.time.LocalDateTime;

public record FxDealResDTO(String dealId,
                           String fromCurrency,
                           String toCurrency,
                           LocalDateTime dealTimestamp,
                           BigInteger dealAmount) {
}
