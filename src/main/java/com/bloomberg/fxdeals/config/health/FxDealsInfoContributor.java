package com.bloomberg.fxdeals.config.health;

import org.springframework.boot.actuate.info.Info;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Component
public class FxDealsInfoContributor implements InfoContributor {

    @Override
    public void contribute(Info.Builder builder) {
        Map<String, Object> appDetails = new HashMap<>();
        appDetails.put("name", "FX Deals Data Warehouse");
        appDetails.put("description", "Data warehouse service for importing and persisting FX deals");
        appDetails.put("version", "0.0.1-SNAPSHOT");
        appDetails.put("timestamp", LocalDateTime.now().toString());

        Map<String, Object> capabilities = new HashMap<>();
        capabilities.put("import", "Support for single and batch FX deal imports");
        capabilities.put("validation", "Comprehensive deal validation");
        capabilities.put("persistence", "PostgreSQL database with Liquibase migrations");

        builder.withDetail("application", appDetails);
        builder.withDetail("capabilities", capabilities);
    }
}

