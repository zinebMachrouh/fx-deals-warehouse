package com.bloomberg.fxdeals.dtos.res;

import lombok.Builder;

import java.util.List;

@Builder
public record RejectedFxDealResDTO(String dealId,
                                   List<String> validationMsgs) {
}
