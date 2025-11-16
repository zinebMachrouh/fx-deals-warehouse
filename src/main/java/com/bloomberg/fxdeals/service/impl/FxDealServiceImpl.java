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
import lombok.extern.slf4j.Slf4j;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class FxDealServiceImpl implements FxDealService {
    public static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final FxDealRepository repo;
    private final FxDealMapper mapper;

    @Override
    public FxDealResDTO importSingleDeal(FxDealReqDTO fxDealReq) {
        log.info("Starting import for deal ID: {}", fxDealReq.dealId());
        log.debug("Deal details - From: {}, To: {}, Amount: {}, Timestamp: {}",
                  fxDealReq.fromCurrency(), fxDealReq.toCurrency(),
                  fxDealReq.dealAmount(), fxDealReq.dealTimestamp());

        var validationMsgs = validateImport(fxDealReq);
        if (!validationMsgs.isEmpty()) {
            log.warn("Validation failed for deal ID: {} - Errors: {}", fxDealReq.dealId(), validationMsgs);
            throw new FxDealSingleImportException(RejectedFxDealResDTO.builder()
                    .dealId(fxDealReq.dealId())
                    .validationMsgs(validationMsgs)
                    .build());
        }

        var savedFxDeal = repo.save(mapper.toEntity(fxDealReq));
        log.info("Successfully saved deal with ID: {}", savedFxDeal.getDealId());
        return mapper.toDTO(savedFxDeal);
    }

    @Override
    public List<FxDealResDTO> importBatchDeals(List<FxDealReqDTO> fxDealReqs) {
        log.info("Starting batch import for {} deals", fxDealReqs.size());
        var rejectedFxDeals = new ArrayList<RejectedFxDealResDTO>();
        var  validatedFxDeals = new ArrayList<FxDealResDTO>();

        fxDealReqs.forEach(fxDealReq -> {
            log.debug("Processing deal ID: {} in batch", fxDealReq.dealId());
            var validationMsgs = validateImport(fxDealReq);
            if(validationMsgs.isEmpty()){
                try {
                    var savedFxDeal = repo.save(mapper.toEntity(fxDealReq));
                    validatedFxDeals.add(mapper.toDTO(savedFxDeal));
                    log.info("Successfully saved deal ID: {} in batch", savedFxDeal.getDealId());
                } catch (Exception e) {
                    log.error("Failed to save deal ID: {} in batch - Error: {}", fxDealReq.dealId(), e.getMessage(), e);
                    rejectedFxDeals.add(RejectedFxDealResDTO.builder()
                            .dealId(fxDealReq.dealId())
                            .validationMsgs(List.of("Database error: " + e.getMessage()))
                            .build());
                }
            } else {
                log.warn("Validation failed for deal ID: {} in batch - Errors: {}", fxDealReq.dealId(), validationMsgs);
                rejectedFxDeals.add(RejectedFxDealResDTO.builder()
                        .dealId(fxDealReq.dealId())
                        .validationMsgs(validationMsgs)
                        .build());
            }
        });

        if(!rejectedFxDeals.isEmpty()) {
            log.warn("Batch import completed with {} rejected deals and {} successful deals",
                     rejectedFxDeals.size(), validatedFxDeals.size());
            throw new FxDealBatchImportException(rejectedFxDeals, validatedFxDeals);
        }

        log.info("Batch import completed successfully with {} deals", validatedFxDeals.size());
        return validatedFxDeals;
    }

    @Override
    public List<FxDealResDTO> getAllDeals() {
        log.info("Fetching all deals from database");
        var deals = mapper.toDTOs(repo.findAll());
        log.info("Retrieved {} deals from database", deals.size());
        return deals;
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
        if(isBlank(fromCurrency)) {
            log.warn("From currency validation failed: From currency is missing");
            return "From currency is required";
        } else {
            try {
                Currency.getInstance(fromCurrency);
            } catch (IllegalArgumentException e) {
                log.warn("From currency validation failed: Invalid ISO currency code: {}", fromCurrency);
                return "From currency must be a valid ISO currency";
            }
        }
        return "";
    }

    private String validateToCurrency(String toCurrency) {
        if(isBlank(toCurrency)) {
            log.warn("To currency validation failed: To currency is missing");
            return "To currency is required";
        }
        try {
            Currency.getInstance(toCurrency);
        } catch (IllegalArgumentException e) {
            log.warn("To currency validation failed: Invalid ISO currency code: {}", toCurrency);
            return "To currency must be a valid ISO currency";
        }
        return "";
    }

    private String validateDealId(String dealId) {
        if(isBlank(dealId)) {
            log.warn("Deal ID validation failed: Deal ID is missing");
            return "Deal Id is required";
        } else if(repo.existsById(dealId)) {
            log.warn("Deal ID validation failed: Duplicate deal ID detected: {}", dealId);
            return "Deal with id " + dealId + " already exists";
        }

        return "";
    }

    private String validateDealAmount(String dealAmount) {
        if (isBlank(dealAmount)) {
            log.warn("Deal amount validation failed: Deal amount is missing");
            return "Deal amount is required";
        } else {
            try {
                var amount = new BigDecimal(dealAmount);
                if(amount.compareTo(BigDecimal.ZERO) <= 0) {
                    log.warn("Deal amount validation failed: Amount {} is not positive", dealAmount);
                    return "Deal amount must be a positive number";
                }
            } catch (NumberFormatException e) {
                log.warn("Deal amount validation failed: Invalid decimal format: {}", dealAmount);
                return "Deal amount must be a valid decimal number";
            }
        }
        return "";
    }

    private boolean isBlank(String string) {
        return isNull(string) || string.isBlank();
    }
}
