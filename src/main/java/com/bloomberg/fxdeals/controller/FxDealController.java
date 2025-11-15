package com.bloomberg.fxdeals.controller;

import com.bloomberg.fxdeals.dtos.req.FxDealReqDTO;
import com.bloomberg.fxdeals.dtos.res.FxDealResDTO;
import com.bloomberg.fxdeals.service.FxDealService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/deals")
public class FxDealController {
    private final FxDealService service;

    @PostMapping("/import/single")
    @ResponseStatus(HttpStatus.CREATED)
    public FxDealResDTO importSingleDeal(@RequestBody FxDealReqDTO fxDealReq) {
        return service.importSingleDeal(fxDealReq);
    }
}
