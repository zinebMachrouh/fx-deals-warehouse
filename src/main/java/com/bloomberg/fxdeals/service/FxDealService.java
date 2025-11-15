package com.bloomberg.fxdeals.service;

import com.bloomberg.fxdeals.dtos.req.FxDealReqDTO;
import com.bloomberg.fxdeals.dtos.res.FxDealResDTO;

import java.util.List;

public interface FxDealService {
    FxDealResDTO importSingleDeal(FxDealReqDTO fxDeal);
    List<FxDealResDTO> importBatchDeals(List<FxDealReqDTO> fxDeals);
    List<FxDealResDTO> getAllDeals();
}
