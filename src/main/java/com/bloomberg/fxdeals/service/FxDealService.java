package com.bloomberg.fxdeals.service;

import com.bloomberg.fxdeals.dtos.req.FxDealReqDTO;

import java.util.List;

public interface FxDealService {
    void importBatch(List<FxDealReqDTO> fxDeals);
}
