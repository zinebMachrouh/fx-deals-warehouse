package com.bloomberg.fxdeals.controller;

import com.bloomberg.fxdeals.dtos.req.FxDealReqDTO;
import com.bloomberg.fxdeals.dtos.res.FxDealResDTO;
import com.bloomberg.fxdeals.service.FxDealService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/deals")
public class FxDealController {
    private final FxDealService service;

    @PostMapping("/import/single")
    @ResponseStatus(HttpStatus.CREATED)
    public FxDealResDTO importSingleDeal(@RequestBody FxDealReqDTO fxDealReq) {
        log.info("Received request to import single deal with ID: {}", fxDealReq.dealId());
        FxDealResDTO result = service.importSingleDeal(fxDealReq);
        log.info("Successfully imported deal with ID: {}", result.dealId());
        return result;
    }

    @PostMapping("/import/batch")
    @ResponseStatus(HttpStatus.CREATED)
    public List<FxDealResDTO> importBatchDeals(@RequestBody List<FxDealReqDTO> fxDealReqs) {
        log.info("Received request to import batch of {} deals", fxDealReqs.size());
        List<FxDealResDTO> results = service.importBatchDeals(fxDealReqs);
        log.info("Successfully imported {} deals in batch", results.size());
        return results;
    }


    @GetMapping
    public List<FxDealResDTO> getAllDeals() {
        log.info("Received request to retrieve all deals");
        List<FxDealResDTO> deals = service.getAllDeals();
        log.info("Retrieved {} deals", deals.size());
        return deals;
    }
}
