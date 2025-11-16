package com.bloomberg.fxdeals.config.health;

import com.bloomberg.fxdeals.repository.FxDealRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;


@Component
@RequiredArgsConstructor
@Slf4j
public class FxDealsHealthIndicator implements HealthIndicator {

    private final FxDealRepository fxDealRepository;

    @Override
    public Health health() {
        try {
            long dealCount = fxDealRepository.count();

            log.debug("Health check - Total FX deals in database: {}", dealCount);

            return Health.up()
                    .withDetail("service", "FX Deals Data Warehouse")
                    .withDetail("database", "Connected")
                    .withDetail("totalDeals", dealCount)
                    .withDetail("status", "Operational")
                    .build();
        } catch (Exception e) {
            log.error("Health check failed", e);
            return Health.down()
                    .withDetail("service", "FX Deals Data Warehouse")
                    .withDetail("error", e.getMessage())
                    .withDetail("status", "Database connection failed")
                    .build();
        }
    }
}

