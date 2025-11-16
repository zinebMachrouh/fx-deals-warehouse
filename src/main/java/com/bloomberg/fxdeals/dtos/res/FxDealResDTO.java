package com.bloomberg.fxdeals.dtos.res;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record FxDealResDTO(String dealId,
                           String fromCurrency,
                           String toCurrency,

                           @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
                           LocalDateTime dealTimestamp,
                           BigDecimal dealAmount) {
}
