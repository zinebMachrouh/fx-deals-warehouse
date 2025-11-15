package com.bloomberg.fxdeals.dtos;

import java.math.BigInteger;
import java.time.LocalDateTime;

public record FxDealResponseDTO(String dealId,
                                String fromCurrency,
                                String toCurrency,
                                LocalDateTime dealTimestamp,
                                BigInteger dealAmount) {
}
