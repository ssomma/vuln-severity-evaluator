package org.challenge.vulnseverityevaluator.configuration;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/**
 * Bounded cache policies. The values live in configuration so each deployment can trade freshness and memory for
 * database load without rebuilding the application.
 */
@Validated
@ConfigurationProperties("app.cache")
public record DatabaseCacheProperties(@Valid @NotNull Policy catalog,
                                      @Valid @NotNull Policy evaluation) {

    public record Policy(@NotNull Duration ttl, @Positive long maximumSize) {

        public Policy {
            if (ttl != null && (ttl.isZero() || ttl.isNegative())) {
                throw new IllegalArgumentException("cache ttl must be positive");
            }
        }
    }
}
