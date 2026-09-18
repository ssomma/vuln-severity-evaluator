package org.challenge.vulnseverityevaluator.configuration;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import static org.challenge.vulnseverityevaluator.infrastructure.metric.ApplicationMetricCollector.ResourceMetrics.collectResourceHttpIncomingRequest;

/**
 * Cross cutting beans that do not belong to any single layer.
 * <p>
 * Metrics need no bean and are never instantiated: the collector is static and stateless, because aggregation
 * belongs to the log backend and not to this process. Only the HTTP edge needs a hook, and it hangs off
 * {@code afterCompletion} — an interceptor rather than a filter because it runs after handler mapping, which is what
 * makes the matched route pattern available. That pattern, not the raw URI, is the only form of the path that may
 * become a dimension.
 */
@Configuration
public class InfrastructureConfiguration implements WebMvcConfigurer {

    @Bean
    public HandlerInterceptor handlerInterceptor() {
        return new HandlerInterceptor() {

            @Override
            public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler,
                                        Exception exception) {
                collectResourceHttpIncomingRequest(request, response);
            }
        };
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(handlerInterceptor());
    }
}
