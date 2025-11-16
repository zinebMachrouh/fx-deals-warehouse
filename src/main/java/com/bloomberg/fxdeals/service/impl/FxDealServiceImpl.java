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
import java.util.stream.Stream;

import static java.util.Objects.isNull;

@Service
@RequiredArgsConstructor
public class FxDealServiceImpl implements FxDealService {
    public static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
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
        return mapper.toDTO(savedFxDeal);
    }

    @Override
    public List<FxDealResDTO> importBatchDeals(List<FxDealReqDTO> fxDealReqs) {
        var rejectedFxDeals = new ArrayList<RejectedFxDealResDTO>();
        var  validatedFxDeals = new ArrayList<FxDealResDTO>();
        fxDealReqs.forEach(fxDealReq -> {
            var validationMsgs = validateImport(fxDealReq);
            if(validationMsgs.isEmpty()){
                var savedFxDeal = repo.save(mapper.toEntity(fxDealReq));
                validatedFxDeals.add(mapper.toDTO(savedFxDeal));
            } else {
                rejectedFxDeals.add(RejectedFxDealResDTO.builder()
                        .dealId(fxDealReq.dealId())
                        .validationMsgs(validationMsgs)
                        .build());
            }
        });

        if(!rejectedFxDeals.isEmpty()) throw new FxDealBatchImportException(rejectedFxDeals, validatedFxDeals);
        return validatedFxDeals;
    }

    @Override
    public List<FxDealResDTO> getAllDeals() {
        return mapper.toDTOs(repo.findAll());
    }

    private List<String> validateImport(FxDealReqDTO req) {
        return Stream.of(
                validateDealId(req.dealId()),
                validateFromCurrency(req.fromCurrency()),
                validateToCurrency(req.toCurrency()),
                validateCurrenciesNotSame(req),
                validateDealTimestamp(req.dealTimestamp()),
                validateDealAmount(req.dealAmount())
        ).filter(msg -> !msg.isBlank()).toList();
    }

    private String validateDealTimestamp(String dealTimestamp) {
        if (isBlank(dealTimestamp))
            return "Deal timestamp is required";
        else {
            try {
                LocalDateTime.parse(dealTimestamp, FORMATTER);
            } catch (DateTimeParseException e) {
                return "Invalid deal timestamp format, should be yyyy-MM-dd HH:mm:ss";
            }
        }
        return "";
    }

    private String validateCurrenciesNotSame(FxDealReqDTO req) {
        if(!isBlank(req.fromCurrency()) && !isBlank(req.toCurrency())
            && req.fromCurrency().equals(req.toCurrency()))
            return "From currency and To currency must be different";
        return "";
    }

    private String validateFromCurrency(String fromCurrency) {
        if(isBlank(fromCurrency))
            return "From currency is required";
        else {
            try {
                Currency.getInstance(fromCurrency);
            } catch (IllegalArgumentException e) {
                return "From currency must be a valid ISO currency";
            }
        }
        return "";
    }

    private String validateToCurrency(String toCurrency) {
        if(isBlank(toCurrency))
            return "To currency is required";
        try {
            Currency.getInstance(toCurrency);
        } catch (IllegalArgumentException e) {
            return "To currency must be a valid ISO currency";
        }
        return "";
    }

    private String validateDealId(String dealId) {
        if(isBlank(dealId))
            return "Deal Id is required";
        else if(repo.existsById(dealId))
            return "Deal with id " + dealId + " already exists";

        return "";
    }

    private String validateDealAmount(String dealAmount) {
        if (isBlank(dealAmount))
            return "Deal amount is required";
        else {
            try {
                var amount = new BigDecimal(dealAmount);
                if(amount.compareTo(BigDecimal.ZERO) <= 0)
                    return "Deal amount must be a positive number";
            } catch (NumberFormatException e) {
                return "Deal amount must be a valid decimal number";
            }
        }
        return "";
    }

    private boolean isBlank(String string) {
        return isNull(string) || string.isBlank();
    }
}
