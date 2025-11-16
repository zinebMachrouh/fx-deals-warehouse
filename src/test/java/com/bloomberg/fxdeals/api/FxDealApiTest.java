package com.bloomberg.fxdeals.api;

import com.bloomberg.fxdeals.config.AbstractIntegrationTest;
import com.bloomberg.fxdeals.dtos.req.FxDealReqDTO;
import com.bloomberg.fxdeals.entity.FxDeal;
import com.bloomberg.fxdeals.repository.FxDealRepository;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * Integration tests for FxDealController using Testcontainers.
 * These tests verify the complete flow from REST API to database persistence.
 */
class FxDealApiTest extends AbstractIntegrationTest {

    @Autowired
    private FxDealRepository fxDealRepository;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        fxDealRepository.deleteAll();
    }

    @Test
    @DisplayName("Should import a single FX deal successfully")
    void testImportSingleDeal_Success() {
        // Given
        FxDealReqDTO dealRequest = new FxDealReqDTO(
                "DEAL-001",
                "USD",
                "EUR",
                "2024-11-16 10:30:00",
                "1000.50"
        );

        // When & Then
        given()
                .contentType(ContentType.JSON)
                .body(dealRequest)
                .when()
                .post(getBaseUrl() + "/api/v1/deals/import/single")
                .then()
                .statusCode(HttpStatus.CREATED.value())
                .body("dealId", equalTo("DEAL-001"))
                .body("fromCurrency", equalTo("USD"))
                .body("toCurrency", equalTo("EUR"))
                .body("dealTimestamp", equalTo("2024-11-16 10:30:00"))
                .body("dealAmount", equalTo(1000.50f));
    }

    @Test
    @DisplayName("Should reject duplicate deal with same ID")
    void testImportSingleDeal_DuplicateDealId() {
        // Given - First deal
        FxDeal existingDeal = new FxDeal();
        existingDeal.setDealId("DEAL-001");
        existingDeal.setFromCurrency("USD");
        existingDeal.setToCurrency("EUR");
        existingDeal.setDealTimestamp(LocalDateTime.now());
        existingDeal.setDealAmount(new BigDecimal("1000.00"));
        fxDealRepository.save(existingDeal);

        // Second deal with same ID
        FxDealReqDTO duplicateDealRequest = new FxDealReqDTO(
                "DEAL-001",
                "GBP",
                "JPY",
                "2024-11-16 11:30:00",
                "2000.00"
        );

        // When & Then
        given()
                .contentType(ContentType.JSON)
                .body(duplicateDealRequest)
                .when()
                .post(getBaseUrl() + "/api/v1/deals/import/single")
                .then()
                .statusCode(HttpStatus.BAD_REQUEST.value());
    }

    @Test
    @DisplayName("Should import batch deals successfully")
    void testImportBatchDeals_Success() {
        // Given
        List<FxDealReqDTO> batchDeals = Arrays.asList(
                new FxDealReqDTO("DEAL-101", "USD", "EUR", "2024-11-16 10:00:00", "1500.00"),
                new FxDealReqDTO("DEAL-102", "GBP", "JPY", "2024-11-16 11:00:00", "2500.00"),
                new FxDealReqDTO("DEAL-103", "EUR", "CHF", "2024-11-16 12:00:00", "3500.00")
        );

        // When & Then
        given()
                .contentType(ContentType.JSON)
                .body(batchDeals)
                .when()
                .post(getBaseUrl() + "/api/v1/deals/import/batch")
                .then()
                .statusCode(HttpStatus.CREATED.value())
                .body("size()", equalTo(3))
                .body("dealId", hasItems("DEAL-101", "DEAL-102", "DEAL-103"));
    }

    @Test
    @DisplayName("Should handle batch import with partial duplicates")
    void testImportBatchDeals_PartialDuplicates() {
        // Given - Pre-existing deal
        FxDeal existingDeal = new FxDeal();
        existingDeal.setDealId("DEAL-201");
        existingDeal.setFromCurrency("USD");
        existingDeal.setToCurrency("EUR");
        existingDeal.setDealTimestamp(LocalDateTime.now());
        existingDeal.setDealAmount(new BigDecimal("1000.00"));
        fxDealRepository.save(existingDeal);

        // Batch with one duplicate and one new
        List<FxDealReqDTO> batchDeals = Arrays.asList(
                new FxDealReqDTO("DEAL-201", "USD", "EUR", "2024-11-16 10:00:00", "1500.00"), // Duplicate
                new FxDealReqDTO("DEAL-202", "GBP", "JPY", "2024-11-16 11:00:00", "2500.00")  // New
        );

        // When & Then
        given()
                .contentType(ContentType.JSON)
                .body(batchDeals)
                .when()
                .post(getBaseUrl() + "/api/v1/deals/import/batch")
                .then()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .body("savedDeals.size()", equalTo(1))
                .body("savedDeals[0].dealId", equalTo("DEAL-202"))
                .body("rejectedDeals.size()", equalTo(1))
                .body("rejectedDeals[0].dealId", equalTo("DEAL-201"))
                .body("rejectedDeals[0].validationMsgs", hasItem("Deal with id DEAL-201 already exists"));
    }

    @Test
    @DisplayName("Should retrieve all deals")
    void testGetAllDeals() {
        // Given - Save test deals
        FxDeal deal1 = new FxDeal();
        deal1.setDealId("DEAL-301");
        deal1.setFromCurrency("USD");
        deal1.setToCurrency("EUR");
        deal1.setDealTimestamp(LocalDateTime.parse("2024-11-16T10:00:00"));
        deal1.setDealAmount(new BigDecimal("1000.00"));

        FxDeal deal2 = new FxDeal();
        deal2.setDealId("DEAL-302");
        deal2.setFromCurrency("GBP");
        deal2.setToCurrency("JPY");
        deal2.setDealTimestamp(LocalDateTime.parse("2024-11-16T11:00:00"));
        deal2.setDealAmount(new BigDecimal("2000.00"));

        fxDealRepository.saveAll(Arrays.asList(deal1, deal2));

        // When & Then
        given()
                .contentType(ContentType.JSON)
                .when()
                .get(getBaseUrl() + "/api/v1/deals")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("size()", equalTo(2))
                .body("dealId", hasItems("DEAL-301", "DEAL-302"))
                .body("fromCurrency", hasItems("USD", "GBP"))
                .body("toCurrency", hasItems("EUR", "JPY"));
    }

    @Test
    @DisplayName("Should validate required fields for single import")
    void testImportSingleDeal_ValidationError() {
        // Given - Invalid request with null fields
        FxDealReqDTO invalidDealRequest = new FxDealReqDTO(
                null,  // Missing dealId
                "USD",
                "EUR",
                "2024-11-16T10:30:00",
                "1000.50"
        );

        // When & Then
        given()
                .contentType(ContentType.JSON)
                .body(invalidDealRequest)
                .when()
                .post(getBaseUrl() + "/api/v1/deals/import/single")
                .then()
                .statusCode(HttpStatus.BAD_REQUEST.value());
    }

    @Test
    @DisplayName("Should validate invalid currency format")
    void testImportSingleDeal_InvalidCurrencyFormat() {
        // Given - Invalid currency code (more than 3 characters)
        FxDealReqDTO invalidDealRequest = new FxDealReqDTO(
                "DEAL-401",
                "USDD",  // Invalid - 4 characters
                "EUR",
                "2024-11-16T10:30:00",
                "1000.50"
        );

        // When & Then
        given()
                .contentType(ContentType.JSON)
                .body(invalidDealRequest)
                .when()
                .post(getBaseUrl() + "/api/v1/deals/import/single")
                .then()
                .statusCode(HttpStatus.BAD_REQUEST.value());
    }

    @Test
    @DisplayName("Should validate invalid amount format")
    void testImportSingleDeal_InvalidAmountFormat() {
        // Given - Invalid amount format
        FxDealReqDTO invalidDealRequest = new FxDealReqDTO(
                "DEAL-501",
                "USD",
                "EUR",
                "2024-11-16 10:30:00",
                "invalid-amount"
        );

        // When & Then
        given()
                .contentType(ContentType.JSON)
                .body(invalidDealRequest)
                .when()
                .post(getBaseUrl() + "/api/v1/deals/import/single")
                .then()
                .statusCode(HttpStatus.BAD_REQUEST.value());
    }

    @Test
    @DisplayName("Should validate invalid timestamp format")
    void testImportSingleDeal_InvalidTimestampFormat() {
        // Given - Invalid timestamp format
        FxDealReqDTO invalidDealRequest = new FxDealReqDTO(
                "DEAL-601",
                "USD",
                "EUR",
                "invalid-date",
                "1000.50"
        );

        // When & Then
        given()
                .contentType(ContentType.JSON)
                .body(invalidDealRequest)
                .when()
                .post(getBaseUrl() + "/api/v1/deals/import/single")
                .then()
                .statusCode(HttpStatus.BAD_REQUEST.value());
    }

    @Test
    @DisplayName("Should return empty list when no deals exist")
    void testGetAllDeals_EmptyDatabase() {
        // When & Then
        given()
                .contentType(ContentType.JSON)
                .when()
                .get(getBaseUrl() + "/api/v1/deals")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("size()", equalTo(0));
    }
}

