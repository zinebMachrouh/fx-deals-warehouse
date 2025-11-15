package com.bloomberg.fxdeals.mappers;

import com.bloomberg.fxdeals.dtos.req.FxDealReqDTO;
import com.bloomberg.fxdeals.dtos.res.FxDealResDTO;
import com.bloomberg.fxdeals.entity.FxDeal;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Mapper(componentModel = "spring")
public interface FxDealMapper {
    DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Mapping(
            target = "dealTimestamp",
            expression = "java(java.time.LocalDateTime.parse(dto.dealTimestamp(), FORMATTER))"
    )
    @Mapping(
            target = "dealAmount",
            expression = "java(new java.math.BigDecimal(dto.dealAmount()))"
    )
    FxDeal toEntity(FxDealReqDTO dto);

    FxDealResDTO toDTO(FxDeal entity);

    List<FxDealResDTO> toDTOs(List<FxDeal> dtos);
}
