package com.bloomberg.fxdeals.exception;

import com.bloomberg.fxdeals.dtos.res.FxDealResDTO;
import com.bloomberg.fxdeals.dtos.res.RejectedFxDealResDTO;
import lombok.Getter;

import java.util.List;

@Getter
public class FxDealBatchImportException extends RuntimeException {
    private final List<RejectedFxDealResDTO> rejectedFxDeals;
    private final List<FxDealResDTO> validatedFxDeals;
    public FxDealBatchImportException(List<RejectedFxDealResDTO> rejectedFxDeals, List<FxDealResDTO> validatedFxDeals) {
        this.rejectedFxDeals = rejectedFxDeals ;
        this.validatedFxDeals = validatedFxDeals;
    }
}
