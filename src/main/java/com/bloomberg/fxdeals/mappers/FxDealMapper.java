package com.bloomberg.fxdeals.mappers;

import com.bloomberg.fxdeals.dtos.req.FxDealReqDTO;
import com.bloomberg.fxdeals.dtos.res.FxDealResDTO;
import com.bloomberg.fxdeals.entity.FxDeal;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface FxDealMapper {
    FxDeal toEntity(FxDealReqDTO dto);
    FxDealResDTO toDto(FxDeal entity);
}
