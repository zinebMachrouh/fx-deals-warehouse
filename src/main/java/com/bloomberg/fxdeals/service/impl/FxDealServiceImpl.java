package com.bloomberg.fxdeals.service.impl;

import com.bloomberg.fxdeals.dtos.req.FxDealReqDTO;
import com.bloomberg.fxdeals.dtos.res.FxDealResDTO;
import com.bloomberg.fxdeals.dtos.res.RejectedFxDealResDTO;
import com.bloomberg.fxdeals.exception.FxDealBatchImportException;
import com.bloomberg.fxdeals.mappers.FxDealMapper;
import com.bloomberg.fxdeals.repository.FxDealRepository;
import com.bloomberg.fxdeals.service.FxDealService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FxDealServiceImpl implements FxDealService {
    private final FxDealRepository repo;
    private final FxDealMapper mapper;

    @Override
    public void importBatch(List<FxDealReqDTO> fxDealReqs) {
        var rejectedFxDeals = new ArrayList<RejectedFxDealResDTO>();
        var  validatedFxDeals = new ArrayList<FxDealResDTO>();
        fxDealReqs.forEach(fxDealReq -> {
            var validationMsgs = validateImport(fxDealReq);
            if(validationMsgs.isEmpty()){
                var savedFxDeal = repo.save(mapper.toEntity(fxDealReq));
                validatedFxDeals.add(mapper.toDto(savedFxDeal));
            } else {
                rejectedFxDeals.add(RejectedFxDealResDTO.builder()
                        .dealId(fxDealReq.dealId())
                        .validationMsgs(validationMsgs)
                        .build());
            }
        });

        if(rejectedFxDeals.isEmpty()) throw new FxDealBatchImportException(rejectedFxDeals, validatedFxDeals);
    }

    private List<String> validateImport(FxDealReqDTO fxDealReq) {
        return List.of();
    }
}
