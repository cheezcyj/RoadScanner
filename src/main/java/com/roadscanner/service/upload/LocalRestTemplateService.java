package com.roadscanner.service.upload;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

/** Local fail-closed replacement used when the optional ML service is disabled. */
@Service
@Profile("local & !local-ml")
public class LocalRestTemplateService implements RestTemplateService {

    @Override
    public String callFlaskApi(String imageUrl) {
        if (imageUrl == null || imageUrl.trim().isEmpty()) {
            throw new IllegalArgumentException("Image URL must not be blank");
        }
        return "44";
    }
}
