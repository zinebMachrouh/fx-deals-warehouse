package com.bloomberg.fxdeals.mappers;

import com.bloomberg.fxdeals.dtos.FxDealRequestDTO;
import com.bloomberg.fxdeals.dtos.FxDealResponseDTO;
import com.bloomberg.fxdeals.entity.FxDeal;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface FxDealMapper {
    FxDeal toEntity(FxDealRequestDTO dto);
    FxDealResponseDTO toDto(FxDeal entity);
}
