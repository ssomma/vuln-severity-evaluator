package org.challenge.vulnseverityevaluator.configuration;

import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY;
import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static tools.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;
import static tools.jackson.databind.PropertyNamingStrategies.SNAKE_CASE;

/**
 * How the HTTP edge serialises.
 * <p>
 * Expressed as a customizer rather than as a replacement mapper, so the modules Spring Boot registers — date and time
 * support among them — stay in place and only the decisions that belong to this service are stated.
 * <p>
 * {@code FAIL_ON_UNKNOWN_PROPERTIES} is enabled deliberately. Silently ignoring an unrecognised field would let a
 * caller believe it declared a context the service never read, and in this service the declared context moves the
 * severity — so a misspelled field has to fail loudly instead of quietly changing the score.
 * <p>
 * Field visibility is opened so the domain types the response carries serialise from their state. They expose
 * fluent accessors rather than bean getters, and reading the fields is what lets the response reuse them instead of
 * being mirrored by a parallel set of records.
 * <p>
 * Timestamps need no setting here: Jackson 3 already writes dates as ISO-8601 by default.
 */
@Configuration
public class PresentationConfiguration {

    @Bean
    public JsonMapperBuilderCustomizer jsonCustomizer() {
        return builder -> builder
                .propertyNamingStrategy(SNAKE_CASE)
                .changeDefaultVisibility(visibility -> visibility.withFieldVisibility(ANY))
                .changeDefaultPropertyInclusion(inclusion -> inclusion.withValueInclusion(NON_NULL))
                .enable(FAIL_ON_UNKNOWN_PROPERTIES);
    }
}
