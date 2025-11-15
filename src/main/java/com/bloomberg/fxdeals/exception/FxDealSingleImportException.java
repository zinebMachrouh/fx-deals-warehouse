package com.bloomberg.fxdeals.exception;

import com.bloomberg.fxdeals.dtos.res.RejectedFxDealResDTO;
import lombok.Getter;

@Getter
public class FxDealSingleImportException extends RuntimeException {
    private final RejectedFxDealResDTO rejectedFxDeal;
    public FxDealSingleImportException(RejectedFxDealResDTO rejectedFxDeal) {
        this.rejectedFxDeal = rejectedFxDeal;
    }
}
