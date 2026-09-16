package org.challenge.vulnseverityevaluator.configuration;

import org.challenge.vulnseverityevaluator.infrastructure.EvaluationMetrics;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Cross cutting beans that do not belong to any single layer.
 */
@Configuration
public class InfrastructureConfiguration {

    @Bean
    public EvaluationMetrics evaluationMetrics() {
        return new EvaluationMetrics();
    }
}
