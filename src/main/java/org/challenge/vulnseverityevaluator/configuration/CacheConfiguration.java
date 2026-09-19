package org.challenge.vulnseverityevaluator.configuration;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Local, bounded caches for database reads. Catalog data and persisted evaluations use separate managers because
 * they have different freshness and capacity requirements.
 */
@Configuration(proxyBeanMethods = false)
@EnableCaching
@EnableConfigurationProperties(DatabaseCacheProperties.class)
public class CacheConfiguration {

    public static final String LONG_CACHE_MANAGER = "longCacheManager";
    public static final String SHORT_CACHE_MANAGER = "shortCacheManager";
    public static final String SCHEME_CATALOG_VERSIONS_CACHE = "schemeCatalogVersions";
    public static final String SCHEME_METRICS_CACHE = "schemeMetrics";
    public static final String CONTEXT_ATTRIBUTES_CACHE = "contextAttributes";
    public static final String EVALUATIONS_BY_FINGERPRINT_CACHE = "evaluationsByFingerprint";
    public static final String EVALUATIONS_BY_ID_CACHE = "evaluationsById";

    @Bean(LONG_CACHE_MANAGER)
    @Primary
    public CacheManager longCacheManager(DatabaseCacheProperties properties) {
        return cacheManager(properties.catalog());
    }

    @Bean(SHORT_CACHE_MANAGER)
    public CacheManager shortCacheManager(DatabaseCacheProperties properties) {
        return cacheManager(properties.evaluation());
    }

    private static CacheManager cacheManager(DatabaseCacheProperties.Policy policy) {
        CaffeineCacheManager manager = new CaffeineCacheManager();
        manager.setAllowNullValues(false);
        manager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(policy.maximumSize())
                .expireAfterWrite(policy.ttl()));
        return manager;
    }
}
