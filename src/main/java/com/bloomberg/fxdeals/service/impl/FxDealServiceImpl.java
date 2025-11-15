package com.bloomberg.fxdeals.service.impl;

import com.bloomberg.fxdeals.dtos.req.FxDealReqDTO;
import com.bloomberg.fxdeals.dtos.res.FxDealResDTO;
import com.bloomberg.fxdeals.dtos.res.RejectedFxDealResDTO;
import com.bloomberg.fxdeals.exception.FxDealBatchImportException;
import com.bloomberg.fxdeals.exception.FxDealSingleImportException;
import com.bloomberg.fxdeals.mappers.FxDealMapper;
import com.bloomberg.fxdeals.repository.FxDealRepository;
import com.bloomberg.fxdeals.service.FxDealService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;

import static java.util.Objects.isNull;

@Service
@RequiredArgsConstructor
public class FxDealServiceImpl implements FxDealService {
    private final FxDealRepository repo;
    private final FxDealMapper mapper;

    @Override
    public FxDealResDTO importSingleDeal(FxDealReqDTO fxDealReq) {
        var validationMsgs = validateImport(fxDealReq);
        if (!validationMsgs.isEmpty()) {
            throw new FxDealSingleImportException(RejectedFxDealResDTO.builder()
                    .dealId(fxDealReq.dealId())
                    .validationMsgs(validationMsgs)
                    .build());
        }
        var savedFxDeal = repo.save(mapper.toEntity(fxDealReq));
        return mapper.toDto(savedFxDeal);
    }

    @Override
    public List<FxDealResDTO> importBatchDeals(List<FxDealReqDTO> fxDealReqs) {
        var rejectedFxDeals = new ArrayList<RejectedFxDealResDTO>();
        var  validatedFxDeals = new ArrayList<FxDealResDTO>();
        fxDealReqs.forEach(fxDealReq -> {
            var validationMsgs = validateImport(fxDealReq);
            if(validationMsgs.isEmpty()){
                var savedFxDeal = repo.save(mapper.toEntity(fxDealReq));
                validatedFxDeals.add(mapper.toDto(savedFxDeal));
            } else {
                rejectedFxDeals.add(RejectedFxDealResDTO.builder()
                        .dealId(fxDealReq.dealId())
                        .validationMsgs(validationMsgs)
                        .build());
            }
        });

        if(rejectedFxDeals.isEmpty()) throw new FxDealBatchImportException(rejectedFxDeals, validatedFxDeals);
        return validatedFxDeals;
    }

    private List<String> validateImport(FxDealReqDTO req) {
        var validationMsgs = new ArrayList<String>();
        validateRequiredFields(req, validationMsgs);
        validateDealTimestampFormat(req.dealTimestamp(), validationMsgs);
        validateDealAmountFormat(req.dealAmount(), validationMsgs);
        validateCurrency(req.fromCurrency(), true, validationMsgs);
        validateCurrency(req.toCurrency(), false, validationMsgs);
        validateDealUniqueness(req.dealId(), validationMsgs);
        return validationMsgs;
    }

    private void validateDealUniqueness(String dealId, List<String> validationMsgs) {
        if(repo.findByDealId(dealId).isPresent())
            validationMsgs.add("Deal with id " + dealId + " already exists");
    }

    private void validateCurrency(String currency, boolean isFrom, List<String> validationMsgs) {
        try {
            Currency.getInstance(currency);
        } catch (IllegalArgumentException e) {
            validationMsgs.add(isFrom ? "From currency must be a valid ISO currency" : "To currency must be a valid ISO currency");
        }
    }

    private void validateDealAmountFormat(String dealAmount, List<String> validationMsgs) {
        try {
            new BigDecimal(dealAmount);
        } catch (NumberFormatException e) {
            validationMsgs.add("Deal amount must be a valid decimal number");
        }
    }

    private void validateDealTimestampFormat(String dealTimestamp, List<String> validationMsgs) {
        try {
            LocalDateTime.parse(dealTimestamp, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        } catch (DateTimeParseException e) {
            validationMsgs.add("Invalid deal timestamp format, should be yyyy-MM-dd HH:mm:ss");
        }
    }

    private void validateRequiredFields(FxDealReqDTO req, List<String> validationMsgs) {
        if(isBlank(req.dealId()))
            validationMsgs.add("Deal Id is required");
        if(isBlank(req.fromCurrency()))
            validationMsgs.add("From currency is required");
        if(isBlank(req.toCurrency()))
            validationMsgs.add("To currency is required");
        if(isBlank(req.dealTimestamp()))
            validationMsgs.add("Deal timestamp is required");
        if(isBlank(req.dealAmount()))
            validationMsgs.add("Deal amount is required");
    }

    private boolean isBlank(String string) {
        return isNull(string)  || string.isBlank();
    }
}
