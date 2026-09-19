package org.challenge.vulnseverityevaluator.datasource.repository;

import com.github.benmanes.caffeine.cache.Policy;
import jakarta.persistence.EntityManagerFactory;
import org.challenge.vulnseverityevaluator.configuration.DatabaseCacheProperties;
import org.challenge.vulnseverityevaluator.datasource.llm.SeverityReasoningModel;
import org.challenge.vulnseverityevaluator.domain.model.ApplicationContext;
import org.challenge.vulnseverityevaluator.domain.model.Vulnerability;
import org.challenge.vulnseverityevaluator.domain.model.VulnerabilityEvaluation;
import org.challenge.vulnseverityevaluator.domain.service.EvaluationPolicy;
import org.challenge.vulnseverityevaluator.domain.service.SeverityScheme;
import org.challenge.vulnseverityevaluator.domain.service.SpecificationCatalog;
import org.challenge.vulnseverityevaluator.domain.service.VulnerabilitySeverityService;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.challenge.vulnseverityevaluator.configuration.CacheConfiguration.*;
import static org.challenge.vulnseverityevaluator.domain.model.ApplicationContext.RiskProfile.createRiskProfile;
import static org.challenge.vulnseverityevaluator.domain.model.ApplicationContext.createApplicationContext;
import static org.challenge.vulnseverityevaluator.domain.model.BaselineVectorSource.CALLER_SUPPLIED;
import static org.challenge.vulnseverityevaluator.domain.model.BusinessCriticality.TIER_1;
import static org.challenge.vulnseverityevaluator.domain.model.DataClassification.PII;
import static org.challenge.vulnseverityevaluator.domain.model.EvaluationConfiguration.createEvaluationConfiguration;
import static org.challenge.vulnseverityevaluator.domain.model.Exposure.INTERNET_FACING;
import static org.challenge.vulnseverityevaluator.domain.model.VulnerabilityEvaluation.fingerprint;

@SpringBootTest(properties = {
        "spring.jpa.properties.hibernate.generate_statistics=true",
        "app.cache.catalog.ttl=17m",
        "app.cache.catalog.maximum-size=321",
        "app.cache.evaluation.ttl=23s",
        "app.cache.evaluation.maximum-size=654"
})
@ActiveProfiles("local")
class DatabaseCacheIntegrationTest {

    private static final String SCHEME = "CVSS:3.1";
    private static final String VECTOR = "CVSS:3.1/AV:N/AC:L/PR:N/UI:N/S:U/C:H/I:H/A:H";

    @Autowired
    private SchemeMetricRepository metrics;

    @Autowired
    private ContextAttributeRepository attributes;

    @Autowired
    private VulnerabilityEvaluationRepository evaluations;

    @Autowired
    private VulnerabilitySeverityService service;

    @Autowired
    private SeverityScheme scheme;

    @Autowired
    private SeverityReasoningModel model;

    @Autowired
    private EvaluationPolicy policy;

    @Autowired
    private SpecificationCatalog catalog;

    @Autowired
    @Qualifier(LONG_CACHE_MANAGER)
    private CacheManager longCacheManager;

    @Autowired
    @Qualifier(SHORT_CACHE_MANAGER)
    private CacheManager shortCacheManager;

    @Autowired
    private DatabaseCacheProperties properties;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    private Statistics statistics;

    @BeforeEach
    void clearCachesAndStatistics() {
        clear(longCacheManager);
        clear(shortCacheManager);
        statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.clear();
    }

    @Test
    void givenRepeatedCatalogReadsWhenQueryingThenReturnCachedResults() {
        assertSecondReadUsesNoStatements(longCacheManager, SCHEME_CATALOG_VERSIONS_CACHE,
                () -> metrics.findCatalogVersionsBySchemeId(SCHEME));
        assertSecondReadUsesNoStatements(longCacheManager, SCHEME_METRICS_CACHE,
                () -> metrics.findBySchemeIdOrderByOrdinal(SCHEME));
        assertSecondReadUsesNoStatements(longCacheManager, CONTEXT_ATTRIBUTES_CACHE,
                () -> attributes.findByKind(org.challenge.vulnseverityevaluator.domain.model.ContextAttribute.Kind.RUNTIME));
    }

    @Test
    void givenRepeatedEvaluationReadsWhenQueryingThenReturnCachedResults() {
        VulnerabilityEvaluation evaluation = service.evaluate(
                new Vulnerability("CVE-CACHE-" + UUID.randomUUID(), "cache integration test", VECTOR), context());
        String fingerprint = jdbc.queryForObject(
                "select fingerprint from vulnerability_evaluation where id = ?", String.class, evaluation.id());

        assertSecondReadUsesNoStatements(shortCacheManager, EVALUATIONS_BY_FINGERPRINT_CACHE,
                () -> assertThat(evaluations.findByFingerprint(fingerprint)).isPresent());
        assertSecondReadUsesNoStatements(shortCacheManager, EVALUATIONS_BY_ID_CACHE,
                () -> assertThat(evaluations.findById(evaluation.id())).isPresent());
    }

    @Test
    void givenMissingEvaluationWhenQueryingThenReturnDatabaseMissEveryTime() {
        UUID missing = UUID.randomUUID();

        assertThat(evaluations.findById(missing)).isEmpty();
        long statementsAfterFirstRead = statistics.getPrepareStatementCount();
        assertThat(statementsAfterFirstRead).isPositive();

        assertThat(evaluations.findById(missing)).isEmpty();

        assertThat(statistics.getPrepareStatementCount()).isGreaterThan(statementsAfterFirstRead);
        assertThat(requiredCache(shortCacheManager, EVALUATIONS_BY_ID_CACHE).get(missing)).isNull();
    }

    @Test
    void givenMissingEvaluationWhenPersistingLaterThenReturnNewEvaluation() {
        Vulnerability vulnerability = new Vulnerability(
                "CVE-CACHE-" + UUID.randomUUID(), "negative cache test", VECTOR);
        ApplicationContext applicationContext = context();
        String schemeId = scheme.id();
        String key = fingerprint(vulnerability, applicationContext,
                createEvaluationConfiguration(model.identifier(), model.promptVersion(), catalog.version(schemeId),
                        policy.version()), CALLER_SUPPLIED, schemeId);

        assertThat(evaluations.findByFingerprint(key)).isEmpty();

        VulnerabilityEvaluation recorded = service.evaluate(vulnerability, applicationContext);

        assertThat(evaluations.findByFingerprint(key)).map(VulnerabilityEvaluation::id)
                .contains(recorded.id());
    }

    @Test
    void givenEnvironmentOverridesWhenStartingThenSetConfiguredCachePolicies() {
        assertThat(properties.catalog().ttl()).isEqualTo(Duration.ofMinutes(17));
        assertThat(properties.catalog().maximumSize()).isEqualTo(321);
        assertThat(properties.evaluation().ttl()).isEqualTo(Duration.ofSeconds(23));
        assertThat(properties.evaluation().maximumSize()).isEqualTo(654);

        assertPolicy(longCacheManager, SCHEME_METRICS_CACHE, Duration.ofMinutes(17), 321);
        assertPolicy(shortCacheManager, EVALUATIONS_BY_ID_CACHE, Duration.ofSeconds(23), 654);
    }

    private void assertSecondReadUsesNoStatements(CacheManager manager, String cacheName, Runnable query) {
        requiredCache(manager, cacheName).clear();
        statistics.clear();

        query.run();
        long statementsAfterFirstRead = statistics.getPrepareStatementCount();
        assertThat(statementsAfterFirstRead).isPositive();

        query.run();

        assertThat(statistics.getPrepareStatementCount()).isEqualTo(statementsAfterFirstRead);
    }

    private void assertPolicy(CacheManager manager, String cacheName, Duration ttl, long maximumSize) {
        CaffeineCache cache = (CaffeineCache) requiredCache(manager, cacheName);
        Policy<Object, Object> policy = cache.getNativeCache().policy();

        assertThat(policy.eviction().orElseThrow().getMaximum()).isEqualTo(maximumSize);
        assertThat(policy.expireAfterWrite().orElseThrow().getExpiresAfter(TimeUnit.NANOSECONDS))
                .isEqualTo(ttl.toNanos());
    }

    private static void clear(CacheManager manager) {
        manager.getCacheNames().forEach(name -> requiredCache(manager, name).clear());
    }

    private static Cache requiredCache(CacheManager manager, String name) {
        return java.util.Objects.requireNonNull(manager.getCache(name), "missing cache " + name);
    }

    private static ApplicationContext context() {
        return createApplicationContext("cache-test-app-" + UUID.randomUUID(),
                createRiskProfile(INTERNET_FACING, PII, TIER_1), Set.of("JAVA"), Set.of());
    }
}
