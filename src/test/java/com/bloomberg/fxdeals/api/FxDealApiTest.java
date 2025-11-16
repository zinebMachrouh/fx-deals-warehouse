package com.bloomberg.fxdeals.api;

import com.bloomberg.fxdeals.config.AbstractIntegrationTest;
import com.bloomberg.fxdeals.dtos.req.FxDealReqDTO;
import com.bloomberg.fxdeals.repository.FxDealRepository;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

import java.util.Arrays;
import java.util.List;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;


@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class FxDealApiTest extends AbstractIntegrationTest {

    private static final String SINGLE_IMPORT_ENDPOINT = "/api/v1/deals/import/single";
    private static final String BATCH_IMPORT_ENDPOINT = "/api/v1/deals/import/batch";
    private static final String GET_ALL_ENDPOINT = "/api/v1/deals";

    @Autowired
    private FxDealRepository fxDealRepository;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        RestAssured.baseURI = "http://localhost";
        RestAssured.basePath = "";
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
        fxDealRepository.deleteAll();
    }

    // ========== Helper Methods ==========

    private RequestSpecification givenJsonRequest() {
        return given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON);
    }

    private FxDealReqDTO createValidDeal(String dealId, String fromCurrency, String toCurrency,
                                          String timestamp, String amount) {
        return new FxDealReqDTO(dealId, fromCurrency, toCurrency, timestamp, amount);
    }

    private FxDealReqDTO createValidDeal(String dealId) {
        return createValidDeal(dealId, "USD", "EUR", "2024-11-16 10:30:00", "1000.50");
    }

    private Response importDeal(FxDealReqDTO deal) {
        return givenJsonRequest()
                .body(deal)
        .when()
                .post(getBaseUrl() + SINGLE_IMPORT_ENDPOINT);
    }

    // ========== Single Deal Import Tests ==========

    @Nested
    @DisplayName("Single Deal Import - Success Cases")
    @Order(1)
    class SingleDealImportSuccess {

        @Test
        @DisplayName("Should return 201 CREATED with complete response body for valid deal")
        void shouldAcceptValidDeal() {
            // Given
            FxDealReqDTO validDeal = createValidDeal("DEAL-001");

            // When & Then
            givenJsonRequest()
                    .body(validDeal)
            .when()
                    .post(getBaseUrl() + SINGLE_IMPORT_ENDPOINT)
            .then()
                    .statusCode(HttpStatus.CREATED.value())
                    .contentType(ContentType.JSON)
                    .body("dealId", equalTo("DEAL-001"))
                    .body("fromCurrency", equalTo("USD"))
                    .body("toCurrency", equalTo("EUR"))
                    .body("dealTimestamp", equalTo("2024-11-16 10:30:00"))
                    .body("dealAmount", equalTo(1000.50f))
                    .body("$", hasKey("dealId"))
                    .body("$", hasKey("fromCurrency"))
                    .body("$", hasKey("toCurrency"))
                    .body("$", hasKey("dealTimestamp"))
                    .body("$", hasKey("dealAmount"));
        }

        @Test
        @DisplayName("Should accept deals with different currency pairs")
        void shouldAcceptDifferentCurrencyPairs() {
            // Test various currency combinations
            String[][] currencyPairs = {
                {"GBP", "JPY"},
                {"EUR", "CHF"},
                {"AUD", "NZD"},
                {"CAD", "USD"}
            };

            for (int i = 0; i < currencyPairs.length; i++) {
                FxDealReqDTO deal = createValidDeal(
                    "DEAL-" + (i + 100),
                    currencyPairs[i][0],
                    currencyPairs[i][1],
                    "2024-11-16 10:00:00",
                    "500.00"
                );

                givenJsonRequest()
                        .body(deal)
                .when()
                        .post(getBaseUrl() + SINGLE_IMPORT_ENDPOINT)
                .then()
                        .statusCode(HttpStatus.CREATED.value())
                        .body("fromCurrency", equalTo(currencyPairs[i][0]))
                        .body("toCurrency", equalTo(currencyPairs[i][1]));
            }
        }

        @Test
        @DisplayName("Should accept deals with high precision amounts")
        void shouldAcceptHighPrecisionAmounts() {
            FxDealReqDTO deal = createValidDeal("DEAL-PRECISION", "USD", "EUR",
                                                 "2024-11-16 10:00:00", "12345.6789");

            givenJsonRequest()
                    .body(deal)
            .when()
                    .post(getBaseUrl() + SINGLE_IMPORT_ENDPOINT)
            .then()
                    .statusCode(HttpStatus.CREATED.value())
                    .body("dealAmount", equalTo(12345.6789f)); // Direct equality - JSON preserves the value
        }
    }

    @Nested
    @DisplayName("Single Deal Import - Duplicate Detection")
    @Order(2)
    class SingleDealImportDuplicates {

        @Test
        @DisplayName("Should return 400 BAD REQUEST when importing duplicate deal ID")
        void shouldRejectDuplicateDeal() {
            // Given - First import succeeds
            FxDealReqDTO originalDeal = createValidDeal("DEAL-DUP-001");
            importDeal(originalDeal)
                    .then()
                    .statusCode(HttpStatus.CREATED.value());

            // When & Then - Second import with same ID fails
            FxDealReqDTO duplicateDeal = createValidDeal("DEAL-DUP-001", "GBP", "JPY",
                                                          "2024-11-16 15:00:00", "2000.00");

            givenJsonRequest()
                    .body(duplicateDeal)
            .when()
                    .post(getBaseUrl() + SINGLE_IMPORT_ENDPOINT)
            .then()
                    .statusCode(HttpStatus.BAD_REQUEST.value())
                    .contentType(ContentType.JSON)
                    .body("error", containsString("failed to be validated"))
                    .body("rejectedDeal.dealId", equalTo("DEAL-DUP-001"))
                    .body("rejectedDeal.validationMsgs", hasItem(containsString("already exists")))
                    .body("rejectedDeal.validationMsgs", hasSize(1));
        }

        @Test
        @DisplayName("Should return specific error message for duplicate deal")
        void shouldProvideDetailedDuplicateError() {
            // Given
            importDeal(createValidDeal("DEAL-DUP-002"));

            // When & Then
            givenJsonRequest()
                    .body(createValidDeal("DEAL-DUP-002"))
            .when()
                    .post(getBaseUrl() + SINGLE_IMPORT_ENDPOINT)
            .then()
                    .statusCode(HttpStatus.BAD_REQUEST.value())
                    .body("rejectedDeal.validationMsgs[0]",
                          equalTo("Deal with id DEAL-DUP-002 already exists"));
        }
    }

    @Nested
    @DisplayName("Single Deal Import - Validation Errors")
    @Order(3)
    class SingleDealImportValidation {

        @Test
        @DisplayName("Should return 400 BAD REQUEST for null deal ID")
        void shouldRejectNullDealId() {
            // Given
            FxDealReqDTO invalidDeal = createValidDeal(null);

            // When & Then
            givenJsonRequest()
                    .body(invalidDeal)
            .when()
                    .post(getBaseUrl() + SINGLE_IMPORT_ENDPOINT)
            .then()
                    .statusCode(HttpStatus.BAD_REQUEST.value())
                    .body("error", notNullValue())
                    .body("rejectedDeal.validationMsgs", hasItem(containsString("Deal Id is required")));
        }

        @Test
        @DisplayName("Should return 400 BAD REQUEST for empty deal ID")
        void shouldRejectEmptyDealId() {
            FxDealReqDTO invalidDeal = createValidDeal("");

            givenJsonRequest()
                    .body(invalidDeal)
            .when()
                    .post(getBaseUrl() + SINGLE_IMPORT_ENDPOINT)
            .then()
                    .statusCode(HttpStatus.BAD_REQUEST.value())
                    .body("rejectedDeal.validationMsgs", hasItem(containsString("Deal Id is required")));
        }

        @Test
        @DisplayName("Should return 400 BAD REQUEST for invalid currency code")
        void shouldRejectInvalidCurrencyCode() {
            FxDealReqDTO invalidDeal = createValidDeal("DEAL-INV-001", "USDD", "EUR",
                                                        "2024-11-16 10:00:00", "1000.00");

            givenJsonRequest()
                    .body(invalidDeal)
            .when()
                    .post(getBaseUrl() + SINGLE_IMPORT_ENDPOINT)
            .then()
                    .statusCode(HttpStatus.BAD_REQUEST.value())
                    .body("rejectedDeal.validationMsgs",
                          hasItem(containsString("valid ISO currency")));
        }

        @Test
        @DisplayName("Should return 400 BAD REQUEST when from and to currencies are the same")
        void shouldRejectSameCurrencies() {
            FxDealReqDTO invalidDeal = createValidDeal("DEAL-SAME-001", "USD", "USD",
                                                        "2024-11-16 10:00:00", "1000.00");

            givenJsonRequest()
                    .body(invalidDeal)
            .when()
                    .post(getBaseUrl() + SINGLE_IMPORT_ENDPOINT)
            .then()
                    .statusCode(HttpStatus.BAD_REQUEST.value())
                    .body("rejectedDeal.validationMsgs",
                          hasItem(containsString("must be different")));
        }

        @Test
        @DisplayName("Should return 400 BAD REQUEST for invalid amount format")
        void shouldRejectInvalidAmountFormat() {
            FxDealReqDTO invalidDeal = createValidDeal("DEAL-AMT-001", "USD", "EUR",
                                                        "2024-11-16 10:00:00", "invalid-amount");

            givenJsonRequest()
                    .body(invalidDeal)
            .when()
                    .post(getBaseUrl() + SINGLE_IMPORT_ENDPOINT)
            .then()
                    .statusCode(HttpStatus.BAD_REQUEST.value())
                    .body("rejectedDeal.validationMsgs",
                          hasItem(containsString("valid decimal number")));
        }

        @Test
        @DisplayName("Should return 400 BAD REQUEST for negative amount")
        void shouldRejectNegativeAmount() {
            FxDealReqDTO invalidDeal = createValidDeal("DEAL-NEG-001", "USD", "EUR",
                                                        "2024-11-16 10:00:00", "-1000.00");

            givenJsonRequest()
                    .body(invalidDeal)
            .when()
                    .post(getBaseUrl() + SINGLE_IMPORT_ENDPOINT)
            .then()
                    .statusCode(HttpStatus.BAD_REQUEST.value())
                    .body("rejectedDeal.validationMsgs",
                          hasItem(containsString("positive number")));
        }

        @Test
        @DisplayName("Should return 400 BAD REQUEST for zero amount")
        void shouldRejectZeroAmount() {
            FxDealReqDTO invalidDeal = createValidDeal("DEAL-ZERO-001", "USD", "EUR",
                                                        "2024-11-16 10:00:00", "0");

            givenJsonRequest()
                    .body(invalidDeal)
            .when()
                    .post(getBaseUrl() + SINGLE_IMPORT_ENDPOINT)
            .then()
                    .statusCode(HttpStatus.BAD_REQUEST.value())
                    .body("rejectedDeal.validationMsgs",
                          hasItem(containsString("positive number")));
        }

        @Test
        @DisplayName("Should return 400 BAD REQUEST for invalid timestamp format")
        void shouldRejectInvalidTimestampFormat() {
            FxDealReqDTO invalidDeal = createValidDeal("DEAL-TS-001", "USD", "EUR",
                                                        "invalid-timestamp", "1000.00");

            givenJsonRequest()
                    .body(invalidDeal)
            .when()
                    .post(getBaseUrl() + SINGLE_IMPORT_ENDPOINT)
            .then()
                    .statusCode(HttpStatus.BAD_REQUEST.value())
                    .body("rejectedDeal.validationMsgs",
                          hasItem(containsString("Invalid deal timestamp format")));
        }

        @Test
        @DisplayName("Should return all validation errors for completely invalid deal")
        void shouldReturnMultipleValidationErrors() {
            FxDealReqDTO invalidDeal = createValidDeal("", "INVALID", "EUR",
                                                        "bad-date", "-100");

            givenJsonRequest()
                    .body(invalidDeal)
            .when()
                    .post(getBaseUrl() + SINGLE_IMPORT_ENDPOINT)
            .then()
                    .statusCode(HttpStatus.BAD_REQUEST.value())
                    .body("rejectedDeal.validationMsgs", hasSize(greaterThan(1)));
        }
    }

    // ========== Batch Deal Import Tests ==========

    @Nested
    @DisplayName("Batch Deal Import - Success Cases")
    @Order(4)
    class BatchDealImportSuccess {

        @Test
        @DisplayName("Should return 201 CREATED for valid batch import")
        void shouldAcceptValidBatch() {
            // Given
            List<FxDealReqDTO> batchDeals = Arrays.asList(
                    createValidDeal("BATCH-001", "USD", "EUR", "2024-11-16 10:00:00", "1500.00"),
                    createValidDeal("BATCH-002", "GBP", "JPY", "2024-11-16 11:00:00", "2500.00"),
                    createValidDeal("BATCH-003", "EUR", "CHF", "2024-11-16 12:00:00", "3500.00")
            );

            // When & Then
            givenJsonRequest()
                    .body(batchDeals)
            .when()
                    .post(getBaseUrl() + BATCH_IMPORT_ENDPOINT)
            .then()
                    .statusCode(HttpStatus.CREATED.value())
                    .contentType(ContentType.JSON)
                    .body("size()", equalTo(3))
                    .body("dealId", hasItems("BATCH-001", "BATCH-002", "BATCH-003"))
                    .body("[0].dealId", equalTo("BATCH-001"))
                    .body("[1].dealId", equalTo("BATCH-002"))
                    .body("[2].dealId", equalTo("BATCH-003"));
        }

        @Test
        @DisplayName("Should return complete details for each deal in batch")
        void shouldReturnAllFieldsForBatchDeals() {
            List<FxDealReqDTO> batchDeals = List.of(
                    createValidDeal("BATCH-FULL-001", "USD", "EUR", "2024-11-16 10:00:00", "1000.00")
            );

            givenJsonRequest()
                    .body(batchDeals)
            .when()
                    .post(getBaseUrl() + BATCH_IMPORT_ENDPOINT)
            .then()
                    .statusCode(HttpStatus.CREATED.value())
                    .body("[0]", hasKey("dealId"))
                    .body("[0]", hasKey("fromCurrency"))
                    .body("[0]", hasKey("toCurrency"))
                    .body("[0]", hasKey("dealTimestamp"))
                    .body("[0]", hasKey("dealAmount"));
        }
    }

    @Nested
    @DisplayName("Batch Deal Import - Partial Failures")
    @Order(5)
    class BatchDealImportPartialFailures {

        @Test
        @DisplayName("Should return 400 BAD REQUEST with saved and rejected deals for partial duplicates")
        void shouldHandlePartialDuplicates() {
            // Given - Import first deal
            importDeal(createValidDeal("BATCH-DUP-001"));

            // When - Batch with one duplicate and one new
            List<FxDealReqDTO> batchDeals = Arrays.asList(
                    createValidDeal("BATCH-DUP-001"), // Duplicate
                    createValidDeal("BATCH-NEW-001")  // New
            );

            // Then
            givenJsonRequest()
                    .body(batchDeals)
            .when()
                    .post(getBaseUrl() + BATCH_IMPORT_ENDPOINT)
            .then()
                    .statusCode(HttpStatus.BAD_REQUEST.value())
                    .body("error", containsString("failed to be validated"))
                    .body("savedDeals", hasSize(1))
                    .body("savedDeals[0].dealId", equalTo("BATCH-NEW-001"))
                    .body("rejectedDeals", hasSize(1))
                    .body("rejectedDeals[0].dealId", equalTo("BATCH-DUP-001"))
                    .body("rejectedDeals[0].validationMsgs",
                          hasItem(containsString("already exists")));
        }

        @Test
        @DisplayName("Should save valid deals and reject invalid ones in batch")
        void shouldSaveValidAndRejectInvalid() {
            List<FxDealReqDTO> batchDeals = Arrays.asList(
                    createValidDeal("BATCH-MIX-001", "USD", "EUR", "2024-11-16 10:00:00", "1000.00"), // Valid
                    createValidDeal("", "USD", "EUR", "2024-11-16 10:00:00", "2000.00"),              // Invalid ID
                    createValidDeal("BATCH-MIX-002", "USD", "EUR", "2024-11-16 10:00:00", "3000.00")  // Valid
            );

            givenJsonRequest()
                    .body(batchDeals)
            .when()
                    .post(getBaseUrl() + BATCH_IMPORT_ENDPOINT)
            .then()
                    .statusCode(HttpStatus.BAD_REQUEST.value())
                    .body("savedDeals", hasSize(2))
                    .body("savedDeals*.dealId", hasItems("BATCH-MIX-001", "BATCH-MIX-002"))
                    .body("rejectedDeals", hasSize(1))
                    .body("rejectedDeals[0].dealId", equalTo(""));
        }
    }

    // ========== Get All Deals Tests ==========

    @Nested
    @DisplayName("Get All Deals")
    @Order(6)
    class GetAllDeals {

        @Test
        @DisplayName("Should return 200 OK with all deals")
        void shouldReturnAllDeals() {
            // Given - Import some deals
            importDeal(createValidDeal("GET-001"));
            importDeal(createValidDeal("GET-002"));

            // When & Then
            givenJsonRequest()
            .when()
                    .get(getBaseUrl() + GET_ALL_ENDPOINT)
            .then()
                    .statusCode(HttpStatus.OK.value())
                    .contentType(ContentType.JSON)
                    .body("size()", equalTo(2))
                    .body("dealId", hasItems("GET-001", "GET-002"));
        }

        @Test
        @DisplayName("Should return 200 OK with empty array when no deals exist")
        void shouldReturnEmptyArrayWhenNoDeals() {
            givenJsonRequest()
            .when()
                    .get(getBaseUrl() + GET_ALL_ENDPOINT)
            .then()
                    .statusCode(HttpStatus.OK.value())
                    .contentType(ContentType.JSON)
                    .body("size()", equalTo(0))
                    .body("$", hasSize(0));
        }

        @Test
        @DisplayName("Should return all fields for each deal")
        void shouldReturnCompleteDeals() {
            // Given
            importDeal(createValidDeal("GET-FULL-001", "USD", "EUR",
                                       "2024-11-16 10:00:00", "1234.56"));

            // When & Then
            givenJsonRequest()
            .when()
                    .get(getBaseUrl() + GET_ALL_ENDPOINT)
            .then()
                    .statusCode(HttpStatus.OK.value())
                    .body("[0].dealId", equalTo("GET-FULL-001"))
                    .body("[0].fromCurrency", equalTo("USD"))
                    .body("[0].toCurrency", equalTo("EUR"))
                    .body("[0].dealTimestamp", notNullValue())
                    .body("[0].dealAmount", equalTo(1234.56f)); // Use direct equality for cleaner assertion
        }
    }

    // ========== Edge Cases and Negative Tests ==========

    @Nested
    @DisplayName("Edge Cases and Negative Tests")
    @Order(7)
    class EdgeCasesAndNegativeTests {

        @Test
        @DisplayName("Should handle empty batch array")
        void shouldHandleEmptyBatchArray() {
            givenJsonRequest()
                    .body(List.of())
            .when()
                    .post(getBaseUrl() + BATCH_IMPORT_ENDPOINT)
            .then()
                    .statusCode(anyOf(equalTo(200), equalTo(201), equalTo(400)));
        }

        @Test
        @DisplayName("Should reject malformed JSON")
        void shouldRejectMalformedJson() {
            given()
                    .contentType(ContentType.JSON)
                    .body("{invalid json")
            .when()
                    .post(getBaseUrl() + SINGLE_IMPORT_ENDPOINT)
            .then()
                    .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value()) // Spring returns 500 for JSON parse errors
                    .body("error", containsString("JSON parse error"));
        }

        @Test
        @DisplayName("Should handle very large deal amounts")
        void shouldHandleLargeAmounts() {
            FxDealReqDTO deal = createValidDeal("DEAL-LARGE-001", "USD", "EUR",
                                                 "2024-11-16 10:00:00", "999999999.99");

            givenJsonRequest()
                    .body(deal)
            .when()
                    .post(getBaseUrl() + SINGLE_IMPORT_ENDPOINT)
            .then()
                    .statusCode(HttpStatus.CREATED.value())
                    .body("dealAmount", equalTo(1.0E9f)); // Direct equality - JSON returns 1.0E9F
        }

        @Test
        @DisplayName("Should handle very small decimal amounts")
        void shouldHandleSmallDecimals() {
            FxDealReqDTO deal = createValidDeal("DEAL-SMALL-001", "USD", "EUR",
                                                 "2024-11-16 10:00:00", "0.0001");

            givenJsonRequest()
                    .body(deal)
            .when()
                    .post(getBaseUrl() + SINGLE_IMPORT_ENDPOINT)
            .then()
                    .statusCode(HttpStatus.CREATED.value())
                    .body("dealAmount", equalTo(1.0E-4f)); // Direct equality - JSON returns 1.0E-4F
        }
    }
}

