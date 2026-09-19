package org.challenge.vulnseverityevaluator.infrastructure.metric;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.challenge.vulnseverityevaluator.datasource.llm.SeverityReasoningModel;
import org.challenge.vulnseverityevaluator.datasource.repository.ContextAttributeRepository;
import org.challenge.vulnseverityevaluator.domain.model.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.challenge.vulnseverityevaluator.domain.model.ApplicationContext.RiskProfile.createRiskProfile;
import static org.challenge.vulnseverityevaluator.domain.model.ApplicationContext.createApplicationContext;
import static org.challenge.vulnseverityevaluator.domain.model.BaselineVectorSource.CALLER_SUPPLIED;
import static org.challenge.vulnseverityevaluator.domain.model.BusinessCriticality.TIER_1;
import static org.challenge.vulnseverityevaluator.domain.model.Confidence.LOW;
import static org.challenge.vulnseverityevaluator.domain.model.ContextAttribute.Kind.COMPENSATING_CONTROL;
import static org.challenge.vulnseverityevaluator.domain.model.ContextAttribute.Kind.RUNTIME;
import static org.challenge.vulnseverityevaluator.domain.model.DataClassification.FINANCIAL;
import static org.challenge.vulnseverityevaluator.domain.model.EvaluationConfiguration.createEvaluationConfiguration;
import static org.challenge.vulnseverityevaluator.domain.model.Exposure.INTERNET_FACING;
import static org.challenge.vulnseverityevaluator.domain.model.MetricChoice.createMetricChoice;
import static org.challenge.vulnseverityevaluator.domain.model.Provenance.createProvenance;
import static org.challenge.vulnseverityevaluator.domain.model.SeverityScore.createSeverityScore;
import static org.challenge.vulnseverityevaluator.domain.model.VulnerabilityEvaluation.createVulnerabilityEvaluation;
import static org.challenge.vulnseverityevaluator.infrastructure.metric.ApplicationMetricCollector.EventMetrics.*;
import static org.challenge.vulnseverityevaluator.infrastructure.metric.ApplicationMetricCollector.ResourceMetrics.collectResourceHttpIncomingRequest;
import static org.challenge.vulnseverityevaluator.infrastructure.metric.ApplicationMetricCollector.ResourceMetrics.collectResourceModelCall;
import static org.challenge.vulnseverityevaluator.infrastructure.metric.ApplicationMetricCollector.WorkMetrics.*;

/**
 * What every metric actually puts on the line, read from the {@code application.metric} logger itself rather than
 * from the console, so the assertions do not depend on how logging happens to be configured.
 * <p>
 * It runs against the seeded catalog, which is the only way to obtain a real {@link ContextAttribute} — the type has
 * no public constructor because it is a catalog row, not something the application builds.
 * <p>
 * The case that matters most is the last one. Every other test states what a metric emits; that one states what none
 * of them may emit, and it is the regression test for the free text and unbounded values listed in
 * {@link ApplicationMetricCollector}.
 */
@SpringBootTest
@ActiveProfiles("local")
class ApplicationMetricCollectorTest {

    private static final String SCHEME = "CVSS:3.1";
    private static final String APPLICATION = "payments-api";
    private static final String DESCRIPTION = "remote code execution in the request parser";
    private static final String SUMMARY = "reachable from the internet and handling payment data";
    private static final String RAW_ANSWER = "value AV:Z not admitted for AV";
    private static final String PROPOSAL = "proposal";
    private static final String VECTOR = "CVSS:3.1/AV:N/AC:L/PR:N/UI:N/S:U/C:H/I:H/A:H";

    private static final Map<SeverityRating, BigDecimal> SCORES = Map.of(
            SeverityRating.NONE, new BigDecimal("0.0"),
            SeverityRating.LOW, new BigDecimal("2.0"),
            SeverityRating.MEDIUM, new BigDecimal("5.5"),
            SeverityRating.HIGH, new BigDecimal("7.5"),
            SeverityRating.CRITICAL, new BigDecimal("9.1"));

    @Autowired
    private ContextAttributeRepository attributes;

    @Autowired
    private SeverityReasoningModel model;

    private Logger metricLogger;
    private ListAppender<ILoggingEvent> appender;

    @BeforeEach
    void setUp() {
        metricLogger = (Logger) LoggerFactory.getLogger("application.metric");
        appender = new ListAppender<>();
        appender.start();
        metricLogger.addAppender(appender);
    }

    @AfterEach
    void tearDown() {
        metricLogger.detachAppender(appender);
    }

    @Test
    void givenEvaluationRequestWhenCollectingThenSetDeclaredRiskProfileDimensions() {
        collectWorkInputEvaluationRequest(vulnerability(VECTOR), context(), SCHEME);

        assertThat(emitted()).containsExactly("work.input.evaluation.request scheme:CVSS:3.1 "
                + "baseline_source:CALLER_SUPPLIED exposure:INTERNET_FACING data_classification:FINANCIAL "
                + "business_criticality:TIER_1 runtime_count:1 control_count:2");
    }

    /**
     * A caller that sent no vector is the weakest path of the system, so the metric has to say so at the moment the
     * request arrives rather than only once the model has been asked.
     */
    @Test
    void givenRequestWithoutVectorWhenCollectingThenSetModelDerivedSource() {
        collectWorkInputEvaluationRequest(vulnerability(null), context(), SCHEME);

        assertThat(emitted()).singleElement().asString().contains("baseline_source:MODEL_DERIVED");
    }

    @Test
    void givenReusedEvaluationWhenCollectingThenSetLookupDimensions() {
        collectWorkInputEvaluationLookup(evaluation());

        assertThat(emitted()).containsExactly("work.input.evaluation.lookup scheme:CVSS:3.1 "
                + "contextual_rating:CRITICAL confidence:LOW review_required:true");
    }

    @Test
    void givenContextAttributeWhenCollectingThenSetKindAndCode() {
        collectWorkInputContextAttribute(attribute(RUNTIME, "JAVA"));

        assertThat(emitted()).containsExactly("work.input.context.attribute attribute_kind:RUNTIME "
                + "attribute_code:JAVA");
    }

    @Test
    void givenDerivedBaselineWhenCollectingThenSetSchemeDimension() {
        collectWorkProcessBaselineDerived(SCHEME);

        assertThat(emitted()).containsExactly("work.process.baseline.derived scheme:CVSS:3.1");
    }

    @Test
    void givenAssessmentWhenCollectingThenSetRatingsAndBucketedDelta() {
        collectWorkProcessSeverityAssessed(assessment(SeverityRating.MEDIUM, SeverityRating.CRITICAL),
                proposal());

        assertThat(emitted()).containsExactly("work.process.severity.assessed scheme:CVSS:3.1 "
                + "baseline_rating:MEDIUM contextual_rating:CRITICAL direction:up delta:significant abstentions:1");
    }

    @Test
    void givenRecordedEvaluationWhenCollectingThenSetProvenanceDimensions() {
        collectWorkOutputEvaluationRecorded(evaluation());

        assertThat(emitted()).containsExactly("work.output.evaluation.recorded scheme:CVSS:3.1 "
                + "contextual_rating:CRITICAL baseline_source:CALLER_SUPPLIED confidence:LOW review_required:true");
    }

    @Test
    void givenMovedRatingWhenCollectingThenSetDirection() {
        collectEventSeverityRatingMoved(assessment(SeverityRating.CRITICAL, SeverityRating.MEDIUM));

        assertThat(emitted()).containsExactly("event.severity.rating.moved scheme:CVSS:3.1 "
                + "baseline_rating:CRITICAL contextual_rating:MEDIUM direction:down");
    }

    /**
     * The rule lives with the metric rather than at the call site, so an evaluation whose rating did not move emits
     * nothing at all instead of a line a dashboard would have to filter out.
     */
    @Test
    void givenUnmovedRatingWhenCollectingThenDoNotThrowAndEmitNothing() {
        collectEventSeverityRatingMoved(assessment(SeverityRating.HIGH, SeverityRating.HIGH));

        assertThat(emitted()).isEmpty();
    }

    @Test
    void givenRejectedAnswerWhenCollectingThenSetErrorClassWithoutMessage() {
        collectEventModelAnswerRejected(new IllegalStateException(RAW_ANSWER));

        assertThat(emitted()).containsExactly("event.model.answer.rejected error_class:IllegalStateException");
    }

    /**
     * The rejected code is caller controlled and by definition absent from the catalog, so only the kind is bounded
     * enough to be a dimension.
     */
    @Test
    void givenRejectedContextValueWhenCollectingThenSetKindWithoutCode() {
        collectEventContextValueRejected(COMPENSATING_CONTROL);

        assertThat(emitted()).containsExactly("event.context.value.rejected attribute_kind:COMPENSATING_CONTROL");
    }

    @Test
    void givenFailedEvaluationWhenCollectingThenSetErrorClass() {
        collectEventEvaluationFailed(new IllegalArgumentException(RAW_ANSWER));

        assertThat(emitted()).containsExactly("event.evaluation.failed error_class:IllegalArgumentException");
    }

    @Test
    void givenFailedModelCallWhenCollectingThenSetOperationAndOutcome() {
        collectResourceModelCall(model, PROPOSAL, false);

        assertThat(emitted()).containsExactly("resource.model.call model:stub-deterministic "
                + "prompt_version:stub-v1 operation:proposal outcome:failure");
    }

    /**
     * The matched pattern, never the raw URI: the URI is caller controlled, and its identifier would be one time
     * series per evaluation.
     */
    @Test
    void givenIncomingRequestWhenCollectingThenSetMatchedPatternInsteadOfUri() {
        collectResourceHttpIncomingRequest(request(), response());

        assertThat(emitted()).containsExactly("resource.http.incoming.request "
                + "path:/vulnerability-evaluations/{id} method:GET status:404");
    }

    /**
     * An unmapped request never reached a handler, so it carries no pattern. Answering {@code unknown} is what keeps
     * the raw URI out of the dimension.
     */
    @Test
    void givenUnmatchedRequestWhenCollectingThenSetUnknownPath() {
        collectResourceHttpIncomingRequest(new MockHttpServletRequest("GET", "/../../etc/passwd"),
                response());

        assertThat(emitted()).singleElement().asString().contains("path:unknown").doesNotContain("passwd");
    }

    /**
     * The regression test for the exclusion list. Free text from the caller or from the model, and unbounded
     * identifiers, must not reach a metric line: they are what turns a metric stream into both a cost without a
     * signal and a place where a description can leak.
     */
    @Test
    void givenEveryMetricWhenCollectingThenDoNotThrowAndEmitNoFreeText() {
        VulnerabilityEvaluation evaluation = evaluation();
        collectWorkInputEvaluationRequest(vulnerability(VECTOR), context(), SCHEME);
        collectWorkInputEvaluationLookup(evaluation);
        collectWorkInputContextAttribute(attribute(RUNTIME, "JAVA"));
        collectWorkProcessSeverityAssessed(assessment(SeverityRating.LOW, SeverityRating.HIGH),
                proposal());
        collectWorkOutputEvaluationRecorded(evaluation);
        collectEventModelAnswerRejected(new IllegalStateException(RAW_ANSWER));
        collectResourceModelCall(model, PROPOSAL, true);

        assertThat(String.join("\n", emitted()))
                .doesNotContain(DESCRIPTION, SUMMARY, RAW_ANSWER, APPLICATION, VECTOR,
                        evaluation.id().toString(), evaluation.vulnerabilityIdentifier());
    }

    private List<String> emitted() {
        return appender.list.stream().map(ILoggingEvent::getFormattedMessage).toList();
    }

    private ContextAttribute attribute(ContextAttribute.Kind kind, String code) {
        return attributes.findByKind(kind).stream()
                .filter(candidate -> candidate.code().equals(code))
                .findFirst()
                .orElseThrow();
    }

    private static Vulnerability vulnerability(String baselineVector) {
        return new Vulnerability("CVE-2024-0001", DESCRIPTION, baselineVector);
    }

    private static ApplicationContext context() {
        return createApplicationContext(APPLICATION,
                createRiskProfile(INTERNET_FACING, FINANCIAL, TIER_1),
                Set.of("JAVA"),
                Set.of("WAF", "MFA"));
    }

    /**
     * The score comes from the rating rather than being passed alongside it. A fixture where the two disagree would
     * assert a direction the arithmetic never produces, which is the one thing these cases exist to check.
     */
    private static SeverityAssessment assessment(SeverityRating baseline, SeverityRating contextual) {
        return new SeverityAssessment(SCHEME,
                createSeverityScore(SCORES.get(baseline), baseline, VECTOR),
                createSeverityScore(SCORES.get(contextual), contextual, VECTOR),
                SUMMARY,
                List.of(createMetricChoice("CR", "H", SUMMARY)));
    }

    private static ModelSeverityProposal proposal() {
        return new ModelSeverityProposal(
                List.of(createMetricChoice("CR", "H", SUMMARY), createMetricChoice("IR", "X", SUMMARY)),
                SUMMARY);
    }

    private static VulnerabilityEvaluation evaluation() {
        return createVulnerabilityEvaluation(vulnerability(VECTOR), context(),
                assessment(SeverityRating.HIGH, SeverityRating.CRITICAL),
                createProvenance(createEvaluationConfiguration(
                                "stub-deterministic", "stub-v1", "catalog-v1", "policy-v1"),
                        CALLER_SUPPLIED, LOW, true));
    }

    private static MockHttpServletRequest request() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET",
                "/vulnerability-evaluations/8b1f9c2e-0000-4000-8000-000000000000");
        request.setAttribute(org.springframework.web.servlet.HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE,
                "/vulnerability-evaluations/{id}");
        return request;
    }

    private static MockHttpServletResponse response() {
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(404);
        return response;
    }
}
