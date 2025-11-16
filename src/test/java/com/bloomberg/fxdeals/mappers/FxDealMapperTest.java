package com.bloomberg.fxdeals.mappers;

import com.bloomberg.fxdeals.dtos.req.FxDealReqDTO;
import com.bloomberg.fxdeals.dtos.res.FxDealResDTO;
import com.bloomberg.fxdeals.entity.FxDeal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@DisplayName("FxDealMapper Tests")
class FxDealMapperTest {

    @Autowired
    private FxDealMapper mapper;

    @Test
    @DisplayName("Should map FxDealReqDTO to FxDeal entity")
    void shouldMapReqDtoToEntity() {
        // Arrange
        FxDealReqDTO dto = new FxDealReqDTO(
                "DEAL001",
                "USD",
                "EUR",
                "2025-11-16 10:30:00",
                "1000.50"
        );

        // Act
        FxDeal entity = mapper.toEntity(dto);

        // Assert
        assertThat(entity).isNotNull();
        assertThat(entity.getDealId()).isEqualTo("DEAL001");
        assertThat(entity.getFromCurrency()).isEqualTo("USD");
        assertThat(entity.getToCurrency()).isEqualTo("EUR");
        assertThat(entity.getDealTimestamp()).isEqualTo(LocalDateTime.of(2025, 11, 16, 10, 30, 0));
        assertThat(entity.getDealAmount()).isEqualByComparingTo(new BigDecimal("1000.50"));
    }

    @Test
    @DisplayName("Should map FxDeal entity to FxDealResDTO")
    void shouldMapEntityToResDto() {
        // Arrange
        FxDeal entity = new FxDeal();
        entity.setDealId("DEAL001");
        entity.setFromCurrency("USD");
        entity.setToCurrency("EUR");
        entity.setDealTimestamp(LocalDateTime.of(2025, 11, 16, 10, 30, 0));
        entity.setDealAmount(new BigDecimal("1000.50"));

        // Act
        FxDealResDTO dto = mapper.toDTO(entity);

        // Assert
        assertThat(dto).isNotNull();
        assertThat(dto.dealId()).isEqualTo("DEAL001");
        assertThat(dto.fromCurrency()).isEqualTo("USD");
        assertThat(dto.toCurrency()).isEqualTo("EUR");
        assertThat(dto.dealTimestamp()).isEqualTo(LocalDateTime.of(2025, 11, 16, 10, 30, 0));
        assertThat(dto.dealAmount()).isEqualByComparingTo(new BigDecimal("1000.50"));
    }

    @Test
    @DisplayName("Should map list of FxDeal entities to list of FxDealResDTO")
    void shouldMapEntityListToResDtoList() {
        // Arrange
        FxDeal entity1 = new FxDeal();
        entity1.setDealId("DEAL001");
        entity1.setFromCurrency("USD");
        entity1.setToCurrency("EUR");
        entity1.setDealTimestamp(LocalDateTime.of(2025, 11, 16, 10, 30, 0));
        entity1.setDealAmount(new BigDecimal("1000.50"));

        FxDeal entity2 = new FxDeal();
        entity2.setDealId("DEAL002");
        entity2.setFromCurrency("GBP");
        entity2.setToCurrency("JPY");
        entity2.setDealTimestamp(LocalDateTime.of(2025, 11, 16, 11, 30, 0));
        entity2.setDealAmount(new BigDecimal("2000.75"));

        List<FxDeal> entities = Arrays.asList(entity1, entity2);

        // Act
        List<FxDealResDTO> dtos = mapper.toDTOs(entities);

        // Assert
        assertThat(dtos).isNotNull().hasSize(2);
        assertThat(dtos.get(0).dealId()).isEqualTo("DEAL001");
        assertThat(dtos.get(1).dealId()).isEqualTo("DEAL002");
    }

    @Test
    @DisplayName("Should handle null entity when mapping to DTO")
    void shouldHandleNullEntityWhenMappingToDto() {
        // Act
        FxDealResDTO dto = mapper.toDTO(null);

        // Assert
        assertThat(dto).isNull();
    }

    @Test
    @DisplayName("Should handle null DTO when mapping to entity")
    void shouldHandleNullDtoWhenMappingToEntity() {
        // Act
        FxDeal entity = mapper.toEntity(null);

        // Assert
        assertThat(entity).isNull();
    }

    @Test
    @DisplayName("Should handle null list when mapping entities to DTOs")
    void shouldHandleNullListWhenMappingToDtos() {
        // Act
        List<FxDealResDTO> dtos = mapper.toDTOs(null);

        // Assert
        assertThat(dtos).isNull();
    }

    @Test
    @DisplayName("Should handle empty list when mapping entities to DTOs")
    void shouldHandleEmptyListWhenMappingToDtos() {
        // Act
        List<FxDealResDTO> dtos = mapper.toDTOs(List.of());

        // Assert
        assertThat(dtos).isNotNull().isEmpty();
    }

    @Test
    @DisplayName("Should map decimal amount with high precision")
    void shouldMapDecimalAmountWithHighPrecision() {
        // Arrange
        FxDealReqDTO dto = new FxDealReqDTO(
                "DEAL001",
                "USD",
                "EUR",
                "2025-11-16 10:30:00",
                "1234567.89012345"
        );

        // Act
        FxDeal entity = mapper.toEntity(dto);

        // Assert
        assertThat(entity.getDealAmount()).isEqualByComparingTo(new BigDecimal("1234567.89012345"));
    }

    @Test
    @DisplayName("Should map timestamp with different time values")
    void shouldMapTimestampWithDifferentTimeValues() {
        // Arrange
        FxDealReqDTO dto = new FxDealReqDTO(
                "DEAL001",
                "USD",
                "EUR",
                "2025-01-01 00:00:00",
                "1000.00"
        );

        // Act
        FxDeal entity = mapper.toEntity(dto);

        // Assert
        assertThat(entity.getDealTimestamp()).isEqualTo(LocalDateTime.of(2025, 1, 1, 0, 0, 0));
    }
}

