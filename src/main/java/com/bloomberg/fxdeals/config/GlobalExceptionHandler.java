package com.bloomberg.fxdeals.config;

import com.bloomberg.fxdeals.exception.FxDealBatchImportException;
import com.bloomberg.fxdeals.exception.FxDealSingleImportException;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;


@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(Exception.class)
    public Map<String, String> handleGeneralException(Exception ex) {
        log.error("Unexpected internal server error occurred - Type: {}, Message: {}",
                  ex.getClass().getSimpleName(),
                  ex.getMessage(),
                  ex);
        return Map.of("error", ex.getMessage());
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(FxDealBatchImportException.class)
    public Map<String, Object> handleFxDealBatchImportException(FxDealBatchImportException ex) {
        var rejectedDeals = ex.getRejectedFxDeals();
        var savedDeals = ex.getSavedDeals();
        var errorMsg = "All fx deals in the batch failed to be validated";

        Map<String, Object> errorRes = new LinkedHashMap<>();
        errorRes.put("rejectedDeals", rejectedDeals);

        if (!savedDeals.isEmpty()) {
            errorMsg = "Some fx deals in the batch failed to be validated";
            errorRes.put("savedDeals", savedDeals);
            log.error("Batch import partial failure - {} deals rejected, {} deals saved. Rejected deal IDs: {}",
                      rejectedDeals.size(),
                      savedDeals.size(),
                      rejectedDeals.stream().map(d -> d.dealId()).toList());
        } else {
            log.error("Batch import complete failure - All {} deals rejected. Rejected deal IDs: {}",
                      rejectedDeals.size(),
                      rejectedDeals.stream().map(d -> d.dealId()).toList());
        }

        errorRes.put("error", errorMsg);
        return errorRes;
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(FxDealSingleImportException.class)
    public Map<String, Object> handleFxDealSingleImportException(FxDealSingleImportException ex) {
        var rejectedDeal = ex.getRejectedFxDeal();
        var errorMsg = "Fx deal failed to be validated";
        log.error("Deal ID: {} - {} - Validation errors: {}",
                  rejectedDeal.dealId(),
                  errorMsg,
                  rejectedDeal.validationMsgs());
        return Map.of(
                "error", errorMsg,
                "rejectedDeal", rejectedDeal);
    }
}
