package org.challenge.vulnseverityevaluator.datasource.repository;

import org.challenge.vulnseverityevaluator.domain.model.ContextAttribute;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

import static org.challenge.vulnseverityevaluator.configuration.CacheConfiguration.CONTEXT_ATTRIBUTES_CACHE;
import static org.challenge.vulnseverityevaluator.configuration.CacheConfiguration.LONG_CACHE_MANAGER;

/**
 * The catalog of admitted context values, read from the database. It is both the allowlist the request is validated
 * against and the source of the meanings the language model reads.
 */
@Repository
public interface ContextAttributeRepository extends JpaRepository<ContextAttribute, String> {

    @Cacheable(cacheNames = CONTEXT_ATTRIBUTES_CACHE, cacheManager = LONG_CACHE_MANAGER, sync = true)
    List<ContextAttribute> findByKind(ContextAttribute.Kind kind);
}
