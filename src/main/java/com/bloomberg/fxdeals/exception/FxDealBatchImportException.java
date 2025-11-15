package com.bloomberg.fxdeals.exception;

import com.bloomberg.fxdeals.dtos.res.FxDealResDTO;
import com.bloomberg.fxdeals.dtos.res.RejectedFxDealResDTO;
import lombok.Getter;

import java.util.List;

@Getter
public class FxDealBatchImportException extends RuntimeException {
    private final List<RejectedFxDealResDTO> rejectedFxDeals;
    private final List<FxDealResDTO> savedDeals;
    public FxDealBatchImportException(List<RejectedFxDealResDTO> rejectedFxDeals, List<FxDealResDTO> savedDeals) {
        this.rejectedFxDeals = rejectedFxDeals ;
        this.savedDeals = savedDeals;
    }
}
