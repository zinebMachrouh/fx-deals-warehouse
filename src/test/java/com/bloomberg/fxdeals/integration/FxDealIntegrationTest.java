package com.bloomberg.fxdeals.integration;

import com.bloomberg.fxdeals.config.AbstractIntegrationTest;
import com.bloomberg.fxdeals.dtos.req.FxDealReqDTO;
import com.bloomberg.fxdeals.entity.FxDeal;
import com.bloomberg.fxdeals.exception.FxDealBatchImportException;
import com.bloomberg.fxdeals.exception.FxDealSingleImportException;
import com.bloomberg.fxdeals.repository.FxDealRepository;
import com.bloomberg.fxdeals.service.FxDealService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.InstanceOfAssertFactories.type;

class FxDealIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private FxDealService fxDealService;

    @Autowired
    private FxDealRepository fxDealRepository;

    @BeforeEach
    void setUp() {
        fxDealRepository.deleteAll();
    }

    @Test
    @DisplayName("Integration: Should correctly map fields and persist single deal to database")
    void testImportSingleDeal_MapsCorrectly() {
        // Given
        FxDealReqDTO dealRequest = new FxDealReqDTO(
                "DEAL-001",
                "USD",
                "EUR",
                "2024-11-16 10:30:00",
                "1000.50"
        );

        // When
        fxDealService.importSingleDeal(dealRequest);

        // Then - Verify database persistence
        List<FxDeal> deals = fxDealRepository.findAll();
        assertThat(deals).hasSize(1);

        FxDeal savedDeal = deals.get(0);
        assertThat(savedDeal.getDealId()).isEqualTo("DEAL-001");
        assertThat(savedDeal.getFromCurrency()).isEqualTo("USD");
        assertThat(savedDeal.getToCurrency()).isEqualTo("EUR");
        assertThat(savedDeal.getDealTimestamp()).isEqualTo(LocalDateTime.parse("2024-11-16T10:30:00"));
        assertThat(savedDeal.getDealAmount()).isEqualByComparingTo(new BigDecimal("1000.50"));
    }

    @Test
    @DisplayName("Integration: Should reject duplicate deal and preserve original data")
    void testImportSingleDeal_DuplicatePreservesOriginal() {
        // Given - First deal
        FxDeal existingDeal = new FxDeal();
        existingDeal.setDealId("DEAL-002");
        existingDeal.setFromCurrency("USD");
        existingDeal.setToCurrency("EUR");
        existingDeal.setDealTimestamp(LocalDateTime.parse("2024-11-16T10:00:00"));
        existingDeal.setDealAmount(new BigDecimal("1000.00"));
        fxDealRepository.save(existingDeal);

        // When & Then - Second deal with same ID should be rejected
        FxDealReqDTO duplicateRequest = new FxDealReqDTO(
                "DEAL-002",
                "GBP",
                "JPY",
                "2024-11-16 11:30:00",
                "2000.00"
        );

        assertThatThrownBy(() -> fxDealService.importSingleDeal(duplicateRequest))
                .asInstanceOf(type(FxDealSingleImportException.class))
                .extracting((e) -> e.getRejectedFxDeal().validationMsgs())
                .isEqualTo(List.of("Deal with id DEAL-002 already exists"));

        // Verify only one deal exists with original values
        List<FxDeal> deals = fxDealRepository.findAll();
        assertThat(deals).hasSize(1);
        assertThat(deals.get(0).getFromCurrency()).isEqualTo("USD"); // Original value
        assertThat(deals.get(0).getToCurrency()).isEqualTo("EUR");   // Original value
        assertThat(deals.get(0).getDealAmount()).isEqualByComparingTo(new BigDecimal("1000.00")); // Original value
    }

    @Test
    @DisplayName("Integration: Should correctly map all fields when importing batch deals")
    void testImportBatchDeals_MapCorrectly() {
        // Given
        List<FxDealReqDTO> batchDeals = Arrays.asList(
                new FxDealReqDTO("DEAL-101", "USD", "EUR", "2024-11-16 10:00:00", "1500.00"),
                new FxDealReqDTO("DEAL-102", "GBP", "JPY", "2024-11-16 11:00:00", "2500.00"),
                new FxDealReqDTO("DEAL-103", "EUR", "CHF", "2024-11-16 12:00:00", "3500.00")
        );

        // When
        fxDealService.importBatchDeals(batchDeals);

        // Then - Verify all deals persisted
        List<FxDeal> deals = fxDealRepository.findAll();
        assertThat(deals).hasSize(3);

        // Verify first deal
        FxDeal deal1 = fxDealRepository.findById("DEAL-101").orElseThrow();
        assertThat(deal1.getDealId()).isEqualTo("DEAL-101");
        assertThat(deal1.getFromCurrency()).isEqualTo("USD");
        assertThat(deal1.getToCurrency()).isEqualTo("EUR");
        assertThat(deal1.getDealTimestamp()).isEqualTo(LocalDateTime.parse("2024-11-16T10:00:00"));
        assertThat(deal1.getDealAmount()).isEqualByComparingTo(new BigDecimal("1500.00"));

        // Verify second deal
        FxDeal deal2 = fxDealRepository.findById("DEAL-102").orElseThrow();
        assertThat(deal2.getDealId()).isEqualTo("DEAL-102");
        assertThat(deal2.getFromCurrency()).isEqualTo("GBP");
        assertThat(deal2.getToCurrency()).isEqualTo("JPY");
        assertThat(deal2.getDealTimestamp()).isEqualTo(LocalDateTime.parse("2024-11-16T11:00:00"));
        assertThat(deal2.getDealAmount()).isEqualByComparingTo(new BigDecimal("2500.00"));

        // Verify third deal
        FxDeal deal3 = fxDealRepository.findById("DEAL-103").orElseThrow();
        assertThat(deal3.getDealId()).isEqualTo("DEAL-103");
        assertThat(deal3.getFromCurrency()).isEqualTo("EUR");
        assertThat(deal3.getToCurrency()).isEqualTo("CHF");
        assertThat(deal3.getDealTimestamp()).isEqualTo(LocalDateTime.parse("2024-11-16T12:00:00"));
        assertThat(deal3.getDealAmount()).isEqualByComparingTo(new BigDecimal("3500.00"));
    }
}

