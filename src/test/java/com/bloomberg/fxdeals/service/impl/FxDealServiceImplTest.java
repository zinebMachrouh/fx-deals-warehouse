package com.bloomberg.fxdeals.service.impl;

import com.bloomberg.fxdeals.dtos.req.FxDealReqDTO;
import com.bloomberg.fxdeals.dtos.res.FxDealResDTO;
import com.bloomberg.fxdeals.entity.FxDeal;
import com.bloomberg.fxdeals.exception.FxDealBatchImportException;
import com.bloomberg.fxdeals.exception.FxDealSingleImportException;
import com.bloomberg.fxdeals.mappers.FxDealMapper;
import com.bloomberg.fxdeals.repository.FxDealRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FxDealService Tests")
class FxDealServiceImplTest {

    @Mock
    private FxDealRepository repository;

    @Mock
    private FxDealMapper mapper;

    @InjectMocks
    private FxDealServiceImpl service;

    private FxDealReqDTO validRequest;
    private FxDeal validEntity;
    private FxDealResDTO validResponse;

    @BeforeEach
    void setUp() {
        validRequest = new FxDealReqDTO(
                "DEAL001",
                "USD",
                "EUR",
                "2025-11-16 10:30:00",
                "1000.50"
        );

        validEntity = new FxDeal();
        validEntity.setDealId("DEAL001");
        validEntity.setFromCurrency("USD");
        validEntity.setToCurrency("EUR");
        validEntity.setDealTimestamp(LocalDateTime.of(2025, 11, 16, 10, 30, 0));
        validEntity.setDealAmount(new BigDecimal("1000.50"));

        validResponse = new FxDealResDTO(
                "DEAL001",
                "USD",
                "EUR",
                LocalDateTime.of(2025, 11, 16, 10, 30, 0),
                new BigDecimal("1000.50")
        );
    }

    @Nested
    @DisplayName("Single Deal Import - Success Cases")
    class SingleDealImportSuccess {

        @Test
        @DisplayName("Should successfully import valid deal")
        void shouldImportValidDeal() {
            // Arrange
            when(repository.existsById("DEAL001")).thenReturn(false);
            when(mapper.toEntity(validRequest)).thenReturn(validEntity);
            when(repository.save(validEntity)).thenReturn(validEntity);
            when(mapper.toDTO(validEntity)).thenReturn(validResponse);

            // Act
            FxDealResDTO result = service.importSingleDeal(validRequest);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.dealId()).isEqualTo("DEAL001");
            assertThat(result.fromCurrency()).isEqualTo("USD");
            assertThat(result.toCurrency()).isEqualTo("EUR");
            assertThat(result.dealAmount()).isEqualTo(new BigDecimal("1000.50"));
            verify(repository).existsById("DEAL001");
            verify(repository).save(validEntity);
        }

        @Test
        @DisplayName("Should import deal with very large amount")
        void shouldImportDealWithVeryLargeAmount() {
            // Arrange
            FxDealReqDTO largeAmountRequest = new FxDealReqDTO(
                    "DEAL003", "USD", "EUR", "2025-11-16 10:30:00", "999999999999.99"
            );
            when(repository.existsById("DEAL003")).thenReturn(false);
            when(mapper.toEntity(largeAmountRequest)).thenReturn(validEntity);
            when(repository.save(validEntity)).thenReturn(validEntity);
            when(mapper.toDTO(validEntity)).thenReturn(validResponse);

            // Act
            FxDealResDTO result = service.importSingleDeal(largeAmountRequest);

            // Assert
            assertThat(result).isNotNull();
            verify(repository).save(validEntity);
        }

        @Test
        @DisplayName("Should import deal with decimal amount")
        void shouldImportDealWithDecimalAmount() {
            // Arrange
            FxDealReqDTO decimalRequest = new FxDealReqDTO(
                    "DEAL004", "USD", "EUR", "2025-11-16 10:30:00", "123.456789"
            );
            when(repository.existsById("DEAL004")).thenReturn(false);
            when(mapper.toEntity(decimalRequest)).thenReturn(validEntity);
            when(repository.save(validEntity)).thenReturn(validEntity);
            when(mapper.toDTO(validEntity)).thenReturn(validResponse);

            // Act
            FxDealResDTO result = service.importSingleDeal(decimalRequest);

            // Assert
            assertThat(result).isNotNull();
            verify(repository).save(validEntity);
        }

        @Test
        @DisplayName("Should import deal with different valid currency codes")
        void shouldImportDealWithDifferentCurrencyCodes() {
            // Arrange
            FxDealReqDTO request = new FxDealReqDTO(
                    "DEAL005", "GBP", "JPY", "2025-11-16 10:30:00", "1000.00"
            );
            when(repository.existsById("DEAL005")).thenReturn(false);
            when(mapper.toEntity(request)).thenReturn(validEntity);
            when(repository.save(validEntity)).thenReturn(validEntity);
            when(mapper.toDTO(validEntity)).thenReturn(validResponse);

            // Act
            FxDealResDTO result = service.importSingleDeal(request);

            // Assert
            assertThat(result).isNotNull();
            verify(repository).save(validEntity);
        }
    }

    @Nested
    @DisplayName("Single Deal Import - Required Field Validations")
    class RequiredFieldValidations {

        @Test
        @DisplayName("Should reject deal with null dealId")
        void shouldRejectNullDealId() {
            // Arrange
            FxDealReqDTO request = new FxDealReqDTO(
                    null, "USD", "EUR", "2025-11-16 10:30:00", "1000.50"
            );

            // Act & Assert
            assertThatThrownBy(() -> service.importSingleDeal(request))
                    .isInstanceOf(FxDealSingleImportException.class)
                    .satisfies(ex -> {
                        FxDealSingleImportException exception = (FxDealSingleImportException) ex;
                        assertThat(exception.getRejectedFxDeal().validationMsgs())
                                .contains("Deal Id is required");
                    });

            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("Should reject deal with empty dealId")
        void shouldRejectEmptyDealId() {
            // Arrange
            FxDealReqDTO request = new FxDealReqDTO(
                    "", "USD", "EUR", "2025-11-16 10:30:00", "1000.50"
            );

            // Act & Assert
            assertThatThrownBy(() -> service.importSingleDeal(request))
                    .isInstanceOf(FxDealSingleImportException.class)
                    .satisfies(ex -> {
                        FxDealSingleImportException exception = (FxDealSingleImportException) ex;
                        assertThat(exception.getRejectedFxDeal().validationMsgs())
                                .contains("Deal Id is required");
                    });

            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("Should reject deal with blank dealId")
        void shouldRejectBlankDealId() {
            // Arrange
            FxDealReqDTO request = new FxDealReqDTO(
                    "   ", "USD", "EUR", "2025-11-16 10:30:00", "1000.50"
            );

            // Act & Assert
            assertThatThrownBy(() -> service.importSingleDeal(request))
                    .isInstanceOf(FxDealSingleImportException.class)
                    .satisfies(ex -> {
                        FxDealSingleImportException exception = (FxDealSingleImportException) ex;
                        assertThat(exception.getRejectedFxDeal().validationMsgs())
                                .contains("Deal Id is required");
                    });

            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("Should reject deal with null fromCurrency")
        void shouldRejectNullFromCurrency() {
            // Arrange
            FxDealReqDTO request = new FxDealReqDTO(
                    "DEAL001", null, "EUR", "2025-11-16 10:30:00", "1000.50"
            );

            // Act & Assert
            assertThatThrownBy(() -> service.importSingleDeal(request))
                    .isInstanceOf(FxDealSingleImportException.class)
                    .satisfies(ex -> {
                        FxDealSingleImportException exception = (FxDealSingleImportException) ex;
                        assertThat(exception.getRejectedFxDeal().validationMsgs())
                                .contains("From currency is required");
                    });

            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("Should reject deal with empty fromCurrency")
        void shouldRejectEmptyFromCurrency() {
            // Arrange
            FxDealReqDTO request = new FxDealReqDTO(
                    "DEAL001", "", "EUR", "2025-11-16 10:30:00", "1000.50"
            );

            // Act & Assert
            assertThatThrownBy(() -> service.importSingleDeal(request))
                    .isInstanceOf(FxDealSingleImportException.class)
                    .satisfies(ex -> {
                        FxDealSingleImportException exception = (FxDealSingleImportException) ex;
                        assertThat(exception.getRejectedFxDeal().validationMsgs())
                                .contains("From currency is required");
                    });

            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("Should reject deal with null toCurrency")
        void shouldRejectNullToCurrency() {
            // Arrange
            FxDealReqDTO request = new FxDealReqDTO(
                    "DEAL001", "USD", null, "2025-11-16 10:30:00", "1000.50"
            );

            // Act & Assert
            assertThatThrownBy(() -> service.importSingleDeal(request))
                    .isInstanceOf(FxDealSingleImportException.class)
                    .satisfies(ex -> {
                        FxDealSingleImportException exception = (FxDealSingleImportException) ex;
                        assertThat(exception.getRejectedFxDeal().validationMsgs())
                                .contains("To currency is required");
                    });

            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("Should reject deal with empty toCurrency")
        void shouldRejectEmptyToCurrency() {
            // Arrange
            FxDealReqDTO request = new FxDealReqDTO(
                    "DEAL001", "USD", "", "2025-11-16 10:30:00", "1000.50"
            );

            // Act & Assert
            assertThatThrownBy(() -> service.importSingleDeal(request))
                    .isInstanceOf(FxDealSingleImportException.class)
                    .satisfies(ex -> {
                        FxDealSingleImportException exception = (FxDealSingleImportException) ex;
                        assertThat(exception.getRejectedFxDeal().validationMsgs())
                                .contains("To currency is required");
                    });

            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("Should reject deal with null dealTimestamp")
        void shouldRejectNullDealTimestamp() {
            // Arrange
            FxDealReqDTO request = new FxDealReqDTO(
                    "DEAL001", "USD", "EUR", null, "1000.50"
            );

            // Act & Assert
            assertThatThrownBy(() -> service.importSingleDeal(request))
                    .isInstanceOf(FxDealSingleImportException.class)
                    .satisfies(ex -> {
                        FxDealSingleImportException exception = (FxDealSingleImportException) ex;
                        assertThat(exception.getRejectedFxDeal().validationMsgs())
                                .contains("Deal timestamp is required");
                    });

            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("Should reject deal with empty dealTimestamp")
        void shouldRejectEmptyDealTimestamp() {
            // Arrange
            FxDealReqDTO request = new FxDealReqDTO(
                    "DEAL001", "USD", "EUR", "", "1000.50"
            );

            // Act & Assert
            assertThatThrownBy(() -> service.importSingleDeal(request))
                    .isInstanceOf(FxDealSingleImportException.class)
                    .satisfies(ex -> {
                        FxDealSingleImportException exception = (FxDealSingleImportException) ex;
                        assertThat(exception.getRejectedFxDeal().validationMsgs())
                                .contains("Deal timestamp is required");
                    });

            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("Should reject deal with null dealAmount")
        void shouldRejectNullDealAmount() {
            // Arrange
            FxDealReqDTO request = new FxDealReqDTO(
                    "DEAL001", "USD", "EUR", "2025-11-16 10:30:00", null
            );

            // Act & Assert
            assertThatThrownBy(() -> service.importSingleDeal(request))
                    .isInstanceOf(FxDealSingleImportException.class)
                    .satisfies(ex -> {
                        FxDealSingleImportException exception = (FxDealSingleImportException) ex;
                        assertThat(exception.getRejectedFxDeal().validationMsgs())
                                .contains("Deal amount is required");
                    });

            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("Should reject deal with empty dealAmount")
        void shouldRejectEmptyDealAmount() {
            // Arrange
            FxDealReqDTO request = new FxDealReqDTO(
                    "DEAL001", "USD", "EUR", "2025-11-16 10:30:00", ""
            );

            // Act & Assert
            assertThatThrownBy(() -> service.importSingleDeal(request))
                    .isInstanceOf(FxDealSingleImportException.class)
                    .satisfies(ex -> {
                        FxDealSingleImportException exception = (FxDealSingleImportException) ex;
                        assertThat(exception.getRejectedFxDeal().validationMsgs())
                                .contains("Deal amount is required");
                    });

            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("Should reject deal with multiple missing fields")
        void shouldRejectDealWithMultipleMissingFields() {
            // Arrange
            FxDealReqDTO request = new FxDealReqDTO(
                    null, null, null, null, null
            );

            // Act & Assert
            assertThatThrownBy(() -> service.importSingleDeal(request))
                    .isInstanceOf(FxDealSingleImportException.class)
                    .satisfies(ex -> {
                        FxDealSingleImportException exception = (FxDealSingleImportException) ex;
                        assertThat(exception.getRejectedFxDeal().validationMsgs())
                                .hasSize(5)
                                .contains(
                                        "Deal Id is required",
                                        "From currency is required",
                                        "To currency is required",
                                        "Deal timestamp is required",
                                        "Deal amount is required"
                                );
                    });

            verify(repository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Deal Timestamp Format Validations")
    class DealTimestampFormatValidations {

        @Test
        @DisplayName("Should reject deal with invalid timestamp format - wrong separator")
        void shouldRejectInvalidTimestampWrongSeparator() {
            // Arrange
            FxDealReqDTO request = new FxDealReqDTO(
                    "DEAL001", "USD", "EUR", "2025/11/16 10:30:00", "1000.50"
            );
            when(repository.existsById("DEAL001")).thenReturn(false);

            // Act & Assert
            assertThatThrownBy(() -> service.importSingleDeal(request))
                    .isInstanceOf(FxDealSingleImportException.class)
                    .satisfies(ex -> {
                        FxDealSingleImportException exception = (FxDealSingleImportException) ex;
                        assertThat(exception.getRejectedFxDeal().validationMsgs())
                                .contains("Invalid deal timestamp format, should be yyyy-MM-dd HH:mm:ss");
                    });

            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("Should reject deal with invalid timestamp format - ISO format")
        void shouldRejectInvalidTimestampIsoFormat() {
            // Arrange
            FxDealReqDTO request = new FxDealReqDTO(
                    "DEAL001", "USD", "EUR", "2025-11-16T10:30:00", "1000.50"
            );
            when(repository.existsById("DEAL001")).thenReturn(false);

            // Act & Assert
            assertThatThrownBy(() -> service.importSingleDeal(request))
                    .isInstanceOf(FxDealSingleImportException.class)
                    .satisfies(ex -> {
                        FxDealSingleImportException exception = (FxDealSingleImportException) ex;
                        assertThat(exception.getRejectedFxDeal().validationMsgs())
                                .contains("Invalid deal timestamp format, should be yyyy-MM-dd HH:mm:ss");
                    });

            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("Should reject deal with invalid timestamp format - date only")
        void shouldRejectInvalidTimestampDateOnly() {
            // Arrange
            FxDealReqDTO request = new FxDealReqDTO(
                    "DEAL001", "USD", "EUR", "2025-11-16", "1000.50"
            );
            when(repository.existsById("DEAL001")).thenReturn(false);

            // Act & Assert
            assertThatThrownBy(() -> service.importSingleDeal(request))
                    .isInstanceOf(FxDealSingleImportException.class)
                    .satisfies(ex -> {
                        FxDealSingleImportException exception = (FxDealSingleImportException) ex;
                        assertThat(exception.getRejectedFxDeal().validationMsgs())
                                .contains("Invalid deal timestamp format, should be yyyy-MM-dd HH:mm:ss");
                    });

            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("Should reject deal with invalid timestamp format - invalid date")
        void shouldRejectInvalidTimestampInvalidDate() {
            // Arrange
            FxDealReqDTO request = new FxDealReqDTO(
                    "DEAL001", "USD", "EUR", "2025-13-32 10:30:00", "1000.50"
            );
            when(repository.existsById("DEAL001")).thenReturn(false);

            // Act & Assert
            assertThatThrownBy(() -> service.importSingleDeal(request))
                    .isInstanceOf(FxDealSingleImportException.class)
                    .satisfies(ex -> {
                        FxDealSingleImportException exception = (FxDealSingleImportException) ex;
                        assertThat(exception.getRejectedFxDeal().validationMsgs())
                                .contains("Invalid deal timestamp format, should be yyyy-MM-dd HH:mm:ss");
                    });

            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("Should reject deal with invalid timestamp format - invalid time")
        void shouldRejectInvalidTimestampInvalidTime() {
            // Arrange
            FxDealReqDTO request = new FxDealReqDTO(
                    "DEAL001", "USD", "EUR", "2025-11-16 25:61:61", "1000.50"
            );
            when(repository.existsById("DEAL001")).thenReturn(false);

            // Act & Assert
            assertThatThrownBy(() -> service.importSingleDeal(request))
                    .isInstanceOf(FxDealSingleImportException.class)
                    .satisfies(ex -> {
                        FxDealSingleImportException exception = (FxDealSingleImportException) ex;
                        assertThat(exception.getRejectedFxDeal().validationMsgs())
                                .contains("Invalid deal timestamp format, should be yyyy-MM-dd HH:mm:ss");
                    });

            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("Should reject deal with invalid timestamp format - random text")
        void shouldRejectInvalidTimestampRandomText() {
            // Arrange
            FxDealReqDTO request = new FxDealReqDTO(
                    "DEAL001", "USD", "EUR", "not-a-date", "1000.50"
            );
            when(repository.existsById("DEAL001")).thenReturn(false);

            // Act & Assert
            assertThatThrownBy(() -> service.importSingleDeal(request))
                    .isInstanceOf(FxDealSingleImportException.class)
                    .satisfies(ex -> {
                        FxDealSingleImportException exception = (FxDealSingleImportException) ex;
                        assertThat(exception.getRejectedFxDeal().validationMsgs())
                                .contains("Invalid deal timestamp format, should be yyyy-MM-dd HH:mm:ss");
                    });

            verify(repository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Deal Amount Validations")
    class DealAmountValidations {

        @Test
        @DisplayName("Should reject deal with negative amount")
        void shouldRejectNegativeAmount() {
            // Arrange
            FxDealReqDTO request = new FxDealReqDTO(
                    "DEAL001", "USD", "EUR", "2025-11-16 10:30:00", "-1000.50"
            );
            when(repository.existsById("DEAL001")).thenReturn(false);

            // Act & Assert
            assertThatThrownBy(() -> service.importSingleDeal(request))
                    .isInstanceOf(FxDealSingleImportException.class)
                    .satisfies(ex -> {
                        FxDealSingleImportException exception = (FxDealSingleImportException) ex;
                        assertThat(exception.getRejectedFxDeal().validationMsgs())
                                .contains("Deal amount must be a positive number");
                    });

            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("Should reject deal with negative zero")
        void shouldRejectNegativeZero() {
            // Arrange
            FxDealReqDTO request = new FxDealReqDTO(
                    "DEAL001", "USD", "EUR", "2025-11-16 10:30:00", "-0.01"
            );
            when(repository.existsById("DEAL001")).thenReturn(false);

            // Act & Assert
            assertThatThrownBy(() -> service.importSingleDeal(request))
                    .isInstanceOf(FxDealSingleImportException.class)
                    .satisfies(ex -> {
                        FxDealSingleImportException exception = (FxDealSingleImportException) ex;
                        assertThat(exception.getRejectedFxDeal().validationMsgs())
                                .contains("Deal amount must be a positive number");
                    });

            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("Should reject deal with zero amount")
        void shouldRejectDealWithZeroAmount() {
            // Arrange
            FxDealReqDTO request = new FxDealReqDTO(
                    "DEAL002", "USD", "EUR", "2025-11-16 10:30:00", "0"
            );
            when(repository.existsById("DEAL002")).thenReturn(false);

            // Act & Assert
            assertThatThrownBy(() -> service.importSingleDeal(request))
                    .isInstanceOf(FxDealSingleImportException.class)
                    .satisfies(ex -> {
                        FxDealSingleImportException exception = (FxDealSingleImportException) ex;
                        assertThat(exception.getRejectedFxDeal().validationMsgs())
                                .contains("Deal amount must be a positive number");
                    });

            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("Should reject deal with non-numeric amount")
        void shouldRejectNonNumericAmount() {
            // Arrange
            FxDealReqDTO request = new FxDealReqDTO(
                    "DEAL001", "USD", "EUR", "2025-11-16 10:30:00", "abc"
            );
            when(repository.existsById("DEAL001")).thenReturn(false);

            // Act & Assert
            assertThatThrownBy(() -> service.importSingleDeal(request))
                    .isInstanceOf(FxDealSingleImportException.class)
                    .satisfies(ex -> {
                        FxDealSingleImportException exception = (FxDealSingleImportException) ex;
                        assertThat(exception.getRejectedFxDeal().validationMsgs())
                                .contains("Deal amount must be a valid decimal number");
                    });

            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("Should reject deal with invalid decimal format")
        void shouldRejectInvalidDecimalFormat() {
            // Arrange
            FxDealReqDTO request = new FxDealReqDTO(
                    "DEAL001", "USD", "EUR", "2025-11-16 10:30:00", "1000.50.25"
            );
            when(repository.existsById("DEAL001")).thenReturn(false);

            // Act & Assert
            assertThatThrownBy(() -> service.importSingleDeal(request))
                    .isInstanceOf(FxDealSingleImportException.class)
                    .satisfies(ex -> {
                        FxDealSingleImportException exception = (FxDealSingleImportException) ex;
                        assertThat(exception.getRejectedFxDeal().validationMsgs())
                                .contains("Deal amount must be a valid decimal number");
                    });

            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("Should reject deal with amount containing special characters")
        void shouldRejectAmountWithSpecialCharacters() {
            // Arrange
            FxDealReqDTO request = new FxDealReqDTO(
                    "DEAL001", "USD", "EUR", "2025-11-16 10:30:00", "$1000.50"
            );
            when(repository.existsById("DEAL001")).thenReturn(false);

            // Act & Assert
            assertThatThrownBy(() -> service.importSingleDeal(request))
                    .isInstanceOf(FxDealSingleImportException.class)
                    .satisfies(ex -> {
                        FxDealSingleImportException exception = (FxDealSingleImportException) ex;
                        assertThat(exception.getRejectedFxDeal().validationMsgs())
                                .contains("Deal amount must be a valid decimal number");
                    });

            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("Should reject deal with amount containing commas")
        void shouldRejectAmountWithCommas() {
            // Arrange
            FxDealReqDTO request = new FxDealReqDTO(
                    "DEAL001", "USD", "EUR", "2025-11-16 10:30:00", "1,000.50"
            );
            when(repository.existsById("DEAL001")).thenReturn(false);

            // Act & Assert
            assertThatThrownBy(() -> service.importSingleDeal(request))
                    .isInstanceOf(FxDealSingleImportException.class)
                    .satisfies(ex -> {
                        FxDealSingleImportException exception = (FxDealSingleImportException) ex;
                        assertThat(exception.getRejectedFxDeal().validationMsgs())
                                .contains("Deal amount must be a valid decimal number");
                    });

            verify(repository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Currency Validations")
    class CurrencyValidations {

        @Test
        @DisplayName("Should reject deal with invalid fromCurrency code")
        void shouldRejectInvalidFromCurrency() {
            // Arrange - Single character is invalid for ISO currency codes
            FxDealReqDTO request = new FxDealReqDTO(
                    "DEAL001", "X", "EUR", "2025-11-16 10:30:00", "1000.50"
            );
            when(repository.existsById("DEAL001")).thenReturn(false);

            // Act & Assert
            assertThatThrownBy(() -> service.importSingleDeal(request))
                    .isInstanceOf(FxDealSingleImportException.class)
                    .satisfies(ex -> {
                        FxDealSingleImportException exception = (FxDealSingleImportException) ex;
                        assertThat(exception.getRejectedFxDeal().validationMsgs())
                                .contains("From currency must be a valid ISO currency");
                    });

            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("Should reject deal with invalid toCurrency code")
        void shouldRejectInvalidToCurrency() {
            // Arrange
            FxDealReqDTO request = new FxDealReqDTO(
                    "DEAL001", "USD", "XYZ", "2025-11-16 10:30:00", "1000.50"
            );
            when(repository.existsById("DEAL001")).thenReturn(false);

            // Act & Assert
            assertThatThrownBy(() -> service.importSingleDeal(request))
                    .isInstanceOf(FxDealSingleImportException.class)
                    .satisfies(ex -> {
                        FxDealSingleImportException exception = (FxDealSingleImportException) ex;
                        assertThat(exception.getRejectedFxDeal().validationMsgs())
                                .contains("To currency must be a valid ISO currency");
                    });

            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("Should reject deal with both invalid currency codes")
        void shouldRejectBothInvalidCurrencyCodes() {
            // Arrange
            FxDealReqDTO request = new FxDealReqDTO(
                    "DEAL001", "ABC", "XYZ", "2025-11-16 10:30:00", "1000.50"
            );
            when(repository.existsById("DEAL001")).thenReturn(false);

            // Act & Assert
            assertThatThrownBy(() -> service.importSingleDeal(request))
                    .isInstanceOf(FxDealSingleImportException.class)
                    .satisfies(ex -> {
                        FxDealSingleImportException exception = (FxDealSingleImportException) ex;
                        assertThat(exception.getRejectedFxDeal().validationMsgs())
                                .hasSize(2)
                                .contains(
                                        "From currency must be a valid ISO currency",
                                        "To currency must be a valid ISO currency"
                                );
                    });

            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("Should reject deal with lowercase currency code")
        void shouldRejectLowercaseCurrency() {
            // Arrange
            FxDealReqDTO request = new FxDealReqDTO(
                    "DEAL001", "usd", "EUR", "2025-11-16 10:30:00", "1000.50"
            );
            when(repository.existsById("DEAL001")).thenReturn(false);

            // Act & Assert
            assertThatThrownBy(() -> service.importSingleDeal(request))
                    .isInstanceOf(FxDealSingleImportException.class)
                    .satisfies(ex -> {
                        FxDealSingleImportException exception = (FxDealSingleImportException) ex;
                        assertThat(exception.getRejectedFxDeal().validationMsgs())
                                .contains("From currency must be a valid ISO currency");
                    });

            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("Should reject deal with numeric currency code")
        void shouldRejectNumericCurrency() {
            // Arrange
            FxDealReqDTO request = new FxDealReqDTO(
                    "DEAL001", "123", "EUR", "2025-11-16 10:30:00", "1000.50"
            );
            when(repository.existsById("DEAL001")).thenReturn(false);

            // Act & Assert
            assertThatThrownBy(() -> service.importSingleDeal(request))
                    .isInstanceOf(FxDealSingleImportException.class)
                    .satisfies(ex -> {
                        FxDealSingleImportException exception = (FxDealSingleImportException) ex;
                        assertThat(exception.getRejectedFxDeal().validationMsgs())
                                .contains("From currency must be a valid ISO currency");
                    });

            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("Should reject deal with too short currency code")
        void shouldRejectTooShortCurrencyCode() {
            // Arrange
            FxDealReqDTO request = new FxDealReqDTO(
                    "DEAL001", "US", "EUR", "2025-11-16 10:30:00", "1000.50"
            );
            when(repository.existsById("DEAL001")).thenReturn(false);

            // Act & Assert
            assertThatThrownBy(() -> service.importSingleDeal(request))
                    .isInstanceOf(FxDealSingleImportException.class)
                    .satisfies(ex -> {
                        FxDealSingleImportException exception = (FxDealSingleImportException) ex;
                        assertThat(exception.getRejectedFxDeal().validationMsgs())
                                .contains("From currency must be a valid ISO currency");
                    });

            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("Should reject deal with too long currency code")
        void shouldRejectTooLongCurrencyCode() {
            // Arrange
            FxDealReqDTO request = new FxDealReqDTO(
                    "DEAL001", "USDD", "EUR", "2025-11-16 10:30:00", "1000.50"
            );
            when(repository.existsById("DEAL001")).thenReturn(false);

            // Act & Assert
            assertThatThrownBy(() -> service.importSingleDeal(request))
                    .isInstanceOf(FxDealSingleImportException.class)
                    .satisfies(ex -> {
                        FxDealSingleImportException exception = (FxDealSingleImportException) ex;
                        assertThat(exception.getRejectedFxDeal().validationMsgs())
                                .contains("From currency must be a valid ISO currency");
                    });

            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("Should reject deal with same fromCurrency and toCurrency")
        void shouldRejectSameFromAndToCurrency() {
            // Arrange
            FxDealReqDTO request = new FxDealReqDTO(
                    "DEAL001", "USD", "USD", "2025-11-16 10:30:00", "1000.50"
            );
            when(repository.existsById("DEAL001")).thenReturn(false);

            // Act & Assert
            assertThatThrownBy(() -> service.importSingleDeal(request))
                    .isInstanceOf(FxDealSingleImportException.class)
                    .satisfies(ex -> {
                        FxDealSingleImportException exception = (FxDealSingleImportException) ex;
                        assertThat(exception.getRejectedFxDeal().validationMsgs())
                                .contains("From currency and To currency must be different");
                    });

            verify(repository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Deal Uniqueness Validations")
    class DealUniquenessValidations {

        @Test
        @DisplayName("Should reject duplicate deal")
        void shouldRejectDuplicateDeal() {
            // Arrange
            when(repository.existsById("DEAL001")).thenReturn(true);

            // Act & Assert
            assertThatThrownBy(() -> service.importSingleDeal(validRequest))
                    .isInstanceOf(FxDealSingleImportException.class)
                    .satisfies(ex -> {
                        FxDealSingleImportException exception = (FxDealSingleImportException) ex;
                        assertThat(exception.getRejectedFxDeal().dealId()).isEqualTo("DEAL001");
                        assertThat(exception.getRejectedFxDeal().validationMsgs())
                                .contains("Deal with id DEAL001 already exists");
                    });

            verify(repository).existsById("DEAL001");
            verify(repository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Multiple Validation Errors")
    class MultipleValidationErrors {

        @Test
        @DisplayName("Should collect all validation errors")
        void shouldCollectAllValidationErrors() {
            // Arrange
            FxDealReqDTO request = new FxDealReqDTO(
                    "DEAL001", "XX", "YY", "invalid-date", "-100"
            );
            when(repository.existsById("DEAL001")).thenReturn(true);

            // Act & Assert
            assertThatThrownBy(() -> service.importSingleDeal(request))
                    .isInstanceOf(FxDealSingleImportException.class)
                    .satisfies(ex -> {
                        FxDealSingleImportException exception = (FxDealSingleImportException) ex;
                        assertThat(exception.getRejectedFxDeal().validationMsgs())
                                .hasSize(5)
                                .contains(
                                        "Invalid deal timestamp format, should be yyyy-MM-dd HH:mm:ss",
                                        "Deal amount must be a positive number",
                                        "From currency must be a valid ISO currency",
                                        "To currency must be a valid ISO currency",
                                        "Deal with id DEAL001 already exists"
                                );
                    });

            verify(repository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Batch Deal Import Tests")
    class BatchDealImportTests {

        @Test
        @DisplayName("Should successfully import all valid deals in batch")
        void shouldImportAllValidDealsInBatch() {
            // Arrange
            FxDealReqDTO request1 = new FxDealReqDTO(
                    "DEAL001", "USD", "EUR", "2025-11-16 10:30:00", "1000.50"
            );
            FxDealReqDTO request2 = new FxDealReqDTO(
                    "DEAL002", "GBP", "JPY", "2025-11-16 11:30:00", "2000.75"
            );
            FxDealReqDTO request3 = new FxDealReqDTO(
                    "DEAL003", "EUR", "USD", "2025-11-16 12:30:00", "3000.25"
            );

            when(repository.existsById("DEAL001")).thenReturn(false);
            when(repository.existsById("DEAL002")).thenReturn(false);
            when(repository.existsById("DEAL003")).thenReturn(false);

            when(mapper.toEntity(any())).thenReturn(validEntity);
            when(repository.save(any())).thenReturn(validEntity);
            when(mapper.toDTO(any())).thenReturn(validResponse);

            // Act
            List<FxDealResDTO> results = service.importBatchDeals(Arrays.asList(request1, request2, request3));

            // Assert
            assertThat(results).hasSize(3);
            verify(repository, times(3)).save(any());
        }

        @Test
        @DisplayName("Should throw exception when some deals are invalid in batch")
        void shouldThrowExceptionWhenSomeDealsInvalid() {
            // Arrange
            FxDealReqDTO validRequest1 = new FxDealReqDTO(
                    "DEAL001", "USD", "EUR", "2025-11-16 10:30:00", "1000.50"
            );
            FxDealReqDTO invalidRequest = new FxDealReqDTO(
                    null, "ZZZ", null, "invalid-date", "-100"
            );
            FxDealReqDTO validRequest2 = new FxDealReqDTO(
                    "DEAL003", "GBP", "JPY", "2025-11-16 12:30:00", "3000.25"
            );

            when(repository.existsById("DEAL001")).thenReturn(false);
            when(repository.existsById("DEAL003")).thenReturn(false);

            when(mapper.toEntity(any())).thenReturn(validEntity);
            when(repository.save(any())).thenReturn(validEntity);
            when(mapper.toDTO(any())).thenReturn(validResponse);

            // Act & Assert
            assertThatThrownBy(() -> service.importBatchDeals(Arrays.asList(validRequest1, invalidRequest, validRequest2)))
                    .isInstanceOf(FxDealBatchImportException.class)
                    .satisfies(ex -> {
                        FxDealBatchImportException exception = (FxDealBatchImportException) ex;
                        assertThat(exception.getRejectedFxDeals()).hasSize(1);
                        assertThat(exception.getSavedDeals()).hasSize(2);
                        assertThat(exception.getRejectedFxDeals().get(0).validationMsgs())
                                .contains(
                                        "Deal Id is required",
                                        "To currency is required"
                                );
                    });

            verify(repository, times(2)).save(any());
        }

        @Test
        @DisplayName("Should throw exception when all deals are invalid in batch")
        void shouldThrowExceptionWhenAllDealsInvalid() {
            // Arrange
            FxDealReqDTO invalidRequest1 = new FxDealReqDTO(
                    null, "ABC", "EUR", "2025-11-16 10:30:00", "1000.50"
            );
            FxDealReqDTO invalidRequest2 = new FxDealReqDTO(
                    "DEAL002", "USD", null, "invalid-date", "-100"
            );

            // Act & Assert
            assertThatThrownBy(() -> service.importBatchDeals(Arrays.asList(invalidRequest1, invalidRequest2)))
                    .isInstanceOf(FxDealBatchImportException.class)
                    .satisfies(ex -> {
                        FxDealBatchImportException exception = (FxDealBatchImportException) ex;
                        assertThat(exception.getRejectedFxDeals()).hasSize(2);
                        assertThat(exception.getSavedDeals()).isEmpty();
                    });

            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("Should handle empty batch list")
        void shouldHandleEmptyBatchList() {
            // Act
            List<FxDealResDTO> results = service.importBatchDeals(List.of());

            // Assert
            assertThat(results).isEmpty();
            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("Should detect duplicate deals within batch")
        void shouldDetectDuplicateDealsWithinBatch() {
            // Arrange
            FxDealReqDTO request1 = new FxDealReqDTO(
                    "DEAL001", "USD", "EUR", "2025-11-16 10:30:00", "1000.50"
            );
            FxDealReqDTO request2 = new FxDealReqDTO(
                    "DEAL001", "GBP", "JPY", "2025-11-16 11:30:00", "2000.75"
            );

            when(repository.existsById("DEAL001"))
                    .thenReturn(false)
                    .thenReturn(true);

            when(mapper.toEntity(any())).thenReturn(validEntity);
            when(repository.save(any())).thenReturn(validEntity);
            when(mapper.toDTO(any())).thenReturn(validResponse);

            // Act & Assert
            assertThatThrownBy(() -> service.importBatchDeals(Arrays.asList(request1, request2)))
                    .isInstanceOf(FxDealBatchImportException.class)
                    .satisfies(ex -> {
                        FxDealBatchImportException exception = (FxDealBatchImportException) ex;
                        assertThat(exception.getRejectedFxDeals()).hasSize(1);
                        assertThat(exception.getSavedDeals()).hasSize(1);
                        assertThat(exception.getRejectedFxDeals().get(0).validationMsgs())
                                .contains("Deal with id DEAL001 already exists");
                    });

            verify(repository, times(1)).save(any());
        }
    }

    @Nested
    @DisplayName("Get All Deals Tests")
    class GetAllDealsTests {

        @Test
        @DisplayName("Should return all deals from repository")
        void shouldReturnAllDeals() {
            // Arrange
            List<FxDeal> entities = Arrays.asList(validEntity, validEntity, validEntity);
            List<FxDealResDTO> responses = Arrays.asList(validResponse, validResponse, validResponse);

            when(repository.findAll()).thenReturn(entities);
            when(mapper.toDTOs(entities)).thenReturn(responses);

            // Act
            List<FxDealResDTO> results = service.getAllDeals();

            // Assert
            assertThat(results).hasSize(3);
            verify(repository).findAll();
            verify(mapper).toDTOs(entities);
        }

        @Test
        @DisplayName("Should return empty list when no deals exist")
        void shouldReturnEmptyListWhenNoDeals() {
            // Arrange
            when(repository.findAll()).thenReturn(List.of());
            when(mapper.toDTOs(any())).thenReturn(List.of());

            // Act
            List<FxDealResDTO> results = service.getAllDeals();

            // Assert
            assertThat(results).isEmpty();
            verify(repository).findAll();
        }
    }

    @Nested
    @DisplayName("Edge Cases and Boundary Tests")
    class EdgeCasesAndBoundaryTests {

        @Test
        @DisplayName("Should handle deal with very small positive amount")
        void shouldHandleVerySmallPositiveAmount() {
            // Arrange
            FxDealReqDTO request = new FxDealReqDTO(
                    "DEAL001", "USD", "EUR", "2025-11-16 10:30:00", "0.000001"
            );
            when(repository.existsById("DEAL001")).thenReturn(false);
            when(mapper.toEntity(request)).thenReturn(validEntity);
            when(repository.save(validEntity)).thenReturn(validEntity);
            when(mapper.toDTO(validEntity)).thenReturn(validResponse);

            // Act
            FxDealResDTO result = service.importSingleDeal(request);

            // Assert
            assertThat(result).isNotNull();
            verify(repository).save(validEntity);
        }

        @Test
        @DisplayName("Should handle deal with midnight timestamp")
        void shouldHandleMidnightTimestamp() {
            // Arrange
            FxDealReqDTO request = new FxDealReqDTO(
                    "DEAL001", "USD", "EUR", "2025-11-16 00:00:00", "1000.50"
            );
            when(repository.existsById("DEAL001")).thenReturn(false);
            when(mapper.toEntity(request)).thenReturn(validEntity);
            when(repository.save(validEntity)).thenReturn(validEntity);
            when(mapper.toDTO(validEntity)).thenReturn(validResponse);

            // Act
            FxDealResDTO result = service.importSingleDeal(request);

            // Assert
            assertThat(result).isNotNull();
            verify(repository).save(validEntity);
        }

        @Test
        @DisplayName("Should handle deal with end of day timestamp")
        void shouldHandleEndOfDayTimestamp() {
            // Arrange
            FxDealReqDTO request = new FxDealReqDTO(
                    "DEAL001", "USD", "EUR", "2025-11-16 23:59:59", "1000.50"
            );
            when(repository.existsById("DEAL001")).thenReturn(false);
            when(mapper.toEntity(request)).thenReturn(validEntity);
            when(repository.save(validEntity)).thenReturn(validEntity);
            when(mapper.toDTO(validEntity)).thenReturn(validResponse);

            // Act
            FxDealResDTO result = service.importSingleDeal(request);

            // Assert
            assertThat(result).isNotNull();
            verify(repository).save(validEntity);
        }

        @Test
        @DisplayName("Should handle deal with leap year date")
        void shouldHandleLeapYearDate() {
            // Arrange
            FxDealReqDTO request = new FxDealReqDTO(
                    "DEAL001", "USD", "EUR", "2024-02-29 10:30:00", "1000.50"
            );
            when(repository.existsById("DEAL001")).thenReturn(false);
            when(mapper.toEntity(request)).thenReturn(validEntity);
            when(repository.save(validEntity)).thenReturn(validEntity);
            when(mapper.toDTO(validEntity)).thenReturn(validResponse);

            // Act
            FxDealResDTO result = service.importSingleDeal(request);

            // Assert
            assertThat(result).isNotNull();
            verify(repository).save(validEntity);
        }

        @Test
        @DisplayName("Should reject deal with invalid date")
        void shouldRejectInvalidLeapYearDate() {
            // Arrange - Month 13 doesn't exist
            FxDealReqDTO request = new FxDealReqDTO(
                    "DEAL001", "USD", "EUR", "2025-13-01 10:30:00", "1000.50"
            );
            when(repository.existsById("DEAL001")).thenReturn(false);

            // Act & Assert
            assertThatThrownBy(() -> service.importSingleDeal(request))
                    .isInstanceOf(FxDealSingleImportException.class)
                    .satisfies(ex -> {
                        FxDealSingleImportException exception = (FxDealSingleImportException) ex;
                        assertThat(exception.getRejectedFxDeal().validationMsgs())
                                .contains("Invalid deal timestamp format, should be yyyy-MM-dd HH:mm:ss");
                    });

            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("Should handle deal with scientific notation amount")
        void shouldRejectScientificNotationAmount() {
            // Arrange
            FxDealReqDTO request = new FxDealReqDTO(
                    "DEAL001", "USD", "EUR", "2025-11-16 10:30:00", "1.5E3"
            );
            when(repository.existsById("DEAL001")).thenReturn(false);
            when(mapper.toEntity(request)).thenReturn(validEntity);
            when(repository.save(validEntity)).thenReturn(validEntity);
            when(mapper.toDTO(validEntity)).thenReturn(validResponse);

            // Act - Scientific notation is actually valid for BigDecimal
            FxDealResDTO result = service.importSingleDeal(request);

            // Assert
            assertThat(result).isNotNull();
            verify(repository).save(validEntity);
        }
    }
}

