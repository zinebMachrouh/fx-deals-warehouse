package com.bloomberg.fxdeals.repository;

import com.bloomberg.fxdeals.entity.FxDeal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FxDealRepository extends JpaRepository<FxDeal, String> {
    Optional<FxDeal> findByDealId(String dealId);
}
