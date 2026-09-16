package org.challenge.vulnseverityevaluator.domain.service.impl;

import org.challenge.vulnseverityevaluator.domain.model.ModelSeverityProposal;
import org.challenge.vulnseverityevaluator.domain.model.SchemeMetric;
import org.challenge.vulnseverityevaluator.domain.model.SeverityAssessment;
import org.challenge.vulnseverityevaluator.domain.model.SeverityRating;
import org.challenge.vulnseverityevaluator.domain.model.SeverityScore;
import org.challenge.vulnseverityevaluator.domain.service.SeverityScheme;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

import static java.math.BigDecimal.ONE;
import static java.math.BigDecimal.TEN;
import static java.math.BigDecimal.ZERO;
import static java.math.RoundingMode.DOWN;
import static java.math.RoundingMode.HALF_UP;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import static org.challenge.vulnseverityevaluator.domain.model.SchemeMetric.NOT_DEFINED;

/**
 * CVSS v3.1: Base metrics anchor the score, Environmental metrics re-score it for the concrete application.
 * Implements the arithmetic of the specification, section 7.
 * <p>
 * The metric table — which metrics exist, which values they admit, what each value weighs — is not here. It arrives
 * as {@link SchemeMetric} rows, so this class holds only the formulas. What used to be eight enums of coefficients is
 * now data, and correcting a weight no longer means editing a calculator.
 * <p>
 * The same computation serves both scores: with every Security Requirement at 1.0 the environmental formula collapses
 * into the base one, so there is one implementation parameterised twice instead of two that must agree.
 * <p>
 * The language model contributes only contextual metric values. Every number here is computed, so a score the model
 * volunteers is never used.
 */
@Service
public class Cvss31 implements SeverityScheme {

    public static final String SCHEME_ID = "CVSS:3.1";

    private static final String SEPARATOR = "/";
    private static final String ASSIGNMENT = ":";
    private static final String MODIFIED_PREFIX = "M";
    private static final String SCOPE = "S";
    private static final String SCOPE_CHANGED = "C";
    private static final List<String> BASE_CODES = List.of("AV", "AC", "PR", "UI", "S", "C", "I", "A");
    private static final List<String> EXPLOITABILITY_CODES = List.of("AV", "AC", "PR", "UI");
    private static final Map<String, String> REQUIREMENT_OF_IMPACT = Map.of("C", "CR", "I", "IR", "A", "AR");

    private static final MathContext PRECISION = new MathContext(20, HALF_UP);
    private static final BigDecimal EXPLOITABILITY_COEFFICIENT = new BigDecimal("8.22");
    private static final BigDecimal UNCHANGED_IMPACT_COEFFICIENT = new BigDecimal("6.42");
    private static final BigDecimal CHANGED_IMPACT_COEFFICIENT = new BigDecimal("7.52");
    private static final BigDecimal CHANGED_IMPACT_OFFSET = new BigDecimal("0.029");
    private static final BigDecimal CHANGED_IMPACT_PENALTY = new BigDecimal("3.25");
    private static final BigDecimal CHANGED_IMPACT_SHIFT = new BigDecimal("0.02");
    private static final BigDecimal SCOPE_COEFFICIENT = new BigDecimal("1.08");
    private static final BigDecimal SUB_SCORE_CEILING = new BigDecimal("0.915");
    private static final BigDecimal ENVIRONMENTAL_FACTOR = new BigDecimal("0.9731");
    private static final BigDecimal SCALING = new BigDecimal("100000");
    private static final BigDecimal TENTHS = new BigDecimal("10000");
    private static final int BASE_EXPONENT = 15;
    private static final int ENVIRONMENTAL_EXPONENT = 13;

    private static final NavigableMap<BigDecimal, SeverityRating> RATINGS = new TreeMap<>(Map.of(
            new BigDecimal("0.0"), SeverityRating.NONE,
            new BigDecimal("0.1"), SeverityRating.LOW,
            new BigDecimal("4.0"), SeverityRating.MEDIUM,
            new BigDecimal("7.0"), SeverityRating.HIGH,
            new BigDecimal("9.0"), SeverityRating.CRITICAL));

    @Override
    public String id() {
        return SCHEME_ID;
    }

    @Override
    public SeverityAssessment assess(String baselineVector, ModelSeverityProposal proposal,
                                     List<SchemeMetric> metrics) {
        Map<String, SchemeMetric> catalog = metrics.stream().collect(toMap(SchemeMetric::code, identity()));
        Map<String, String> baseline = parse(baselineVector, catalog);
        Computation base = new Computation(baseline, neutral(), catalog);
        Computation contextual = new Computation(modified(baseline, proposal),
                requirements(proposal, catalog), catalog);
        return new SeverityAssessment(SCHEME_ID,
                score(base, ONE, BASE_EXPONENT, vector(baseline)),
                score(contextual, ENVIRONMENTAL_FACTOR, ENVIRONMENTAL_EXPONENT, contextualVector(baseline, proposal)),
                proposal.summary(), proposal.choices());
    }

    private static SeverityScore score(Computation computation, BigDecimal factor, int exponent, String vector) {
        BigDecimal value = computation.score(factor, exponent);
        return SeverityScore.createSeverityScore(value, RATINGS.floorEntry(value).getValue(), vector);
    }

    /**
     * Resolves the vector the contextual score is computed from: the value the model chose for a metric replaces the
     * one the baseline vector carried, and X (Not Defined) keeps the baseline value, exactly as the specification
     * prescribes.
     */
    private static Map<String, String> modified(Map<String, String> baseline, ModelSeverityProposal proposal) {
        Map<String, String> resolved = new LinkedHashMap<>(baseline);
        BASE_CODES.forEach(code -> resolved.computeIfPresent(code,
                (metric, value) -> chosen(proposal.value(metric), value)));
        return resolved;
    }

    private static String chosen(String contextual, String baseline) {
        return NOT_DEFINED.equals(contextual) ? baseline : contextual;
    }

    /**
     * The Security Requirement weights, which is how the criticality of the application enters the score.
     */
    private static Map<String, BigDecimal> requirements(ModelSeverityProposal proposal,
                                                        Map<String, SchemeMetric> catalog) {
        return REQUIREMENT_OF_IMPACT.values().stream()
                .collect(toMap(identity(), code -> requirement(proposal, catalog, code)));
    }

    /**
     * An abstained requirement weighs 1.0, which is the same neutral factor {@link #neutral()} applies: not declaring
     * how critical the application is cannot change the score.
     */
    private static BigDecimal requirement(ModelSeverityProposal proposal, Map<String, SchemeMetric> catalog,
                                          String code) {
        String declared = proposal.value(code);
        return NOT_DEFINED.equals(declared) ? ONE : catalog.get(code)
                .value(declared)
                .orElseThrow(() -> new IllegalArgumentException("unknown value for metric " + code))
                .weight();
    }

    /**
     * Neutral requirements, which is what turns the environmental formula into the base one.
     */
    private static Map<String, BigDecimal> neutral() {
        return REQUIREMENT_OF_IMPACT.values().stream().collect(toMap(identity(), code -> ONE));
    }

    private static Map<String, String> parse(String vector, Map<String, SchemeMetric> catalog) {
        Assert.isTrue(StringUtils.hasText(vector), "the baseline vector is missing");
        Map<String, String> parsed = Arrays.stream(vector.split(SEPARATOR))
                .map(part -> part.split(ASSIGNMENT))
                .filter(pair -> pair.length == 2)
                .collect(toMap(pair -> pair[0], pair -> pair[1], (first, second) -> first));
        BASE_CODES.forEach(code -> require(parsed, code, catalog));
        return parsed;
    }

    private static void require(Map<String, String> parsed, String code, Map<String, SchemeMetric> catalog) {
        SchemeMetric metric = catalog.get(code);
        boolean admitted = metric != null && metric.value(parsed.getOrDefault(code, NOT_DEFINED)).isPresent();
        Assert.isTrue(admitted, () -> "missing or unknown metric " + code + " in the baseline vector");
    }

    private static String vector(Map<String, String> metrics) {
        return SCHEME_ID + SEPARATOR + BASE_CODES.stream()
                .map(code -> code + ASSIGNMENT + metrics.get(code))
                .reduce((left, right) -> left + SEPARATOR + right)
                .orElseThrow();
    }

    /**
     * The published Environmental form: the baseline vector followed by the contextual values, where a metric that
     * also exists in the baseline is prefixed with M so both readings fit one string without colliding. Emitting the
     * standard spelling is what lets any CVSS calculator reproduce both scores from this field alone.
     */
    private static String contextualVector(Map<String, String> baseline, ModelSeverityProposal proposal) {
        String contextual = proposal.choices().stream()
                .map(choice -> environmental(choice.metric()) + ASSIGNMENT + choice.value())
                .reduce((left, right) -> left + SEPARATOR + right)
                .orElseThrow();
        return vector(baseline) + SEPARATOR + contextual;
    }

    private static String environmental(String code) {
        return BASE_CODES.contains(code) ? MODIFIED_PREFIX + code : code;
    }

    /**
     * Rounds up to one decimal place following the CVSS v3.1 specification, which avoids the floating point artefacts
     * of a naive ceiling and is why scores are computed on {@link BigDecimal} rather than {@code double}.
     */
    private static BigDecimal roundUp(BigDecimal input) {
        BigDecimal scaled = input.multiply(SCALING).setScale(0, HALF_UP);
        BigDecimal truncated = scaled.divide(TENTHS, 0, DOWN);
        boolean exact = scaled.remainder(TENTHS).signum() == 0;
        return (exact ? truncated : truncated.add(ONE)).movePointLeft(1);
    }

    /**
     * One scoring pass: a resolved vector, the Security Requirement weights that apply to it, and the catalog to look
     * weights up in. Private to this scheme — nothing outside needs the internals of the specification.
     */
    private record Computation(Map<String, String> vector,
                               Map<String, BigDecimal> requirements,
                               Map<String, SchemeMetric> catalog) {

        BigDecimal score(BigDecimal factor, int exponent) {
            BigDecimal impact = impact(factor, exponent);
            boolean harmless = impact.signum() <= 0;
            return harmless ? ZERO.setScale(1) : roundUp(scoped(impact.add(exploitability())).min(TEN));
        }

        private BigDecimal scoped(BigDecimal sum) {
            return scopeChanged() ? SCOPE_COEFFICIENT.multiply(sum) : sum;
        }

        private boolean scopeChanged() {
            return SCOPE_CHANGED.equals(vector.get(SCOPE));
        }

        private BigDecimal exploitability() {
            return EXPLOITABILITY_CODES.stream()
                    .map(this::weight)
                    .reduce(EXPLOITABILITY_COEFFICIENT, BigDecimal::multiply);
        }

        /**
         * The impact sub score. With every requirement at 1.0 this is the specification's ISS; with real weights it
         * is MISS, whose ceiling the base case can never reach anyway.
         */
        private BigDecimal subScore() {
            BigDecimal remaining = REQUIREMENT_OF_IMPACT.entrySet().stream()
                    .map(entry -> ONE.subtract(weight(entry.getKey()).multiply(requirements.get(entry.getValue()))))
                    .reduce(ONE, BigDecimal::multiply);
            return ONE.subtract(remaining).min(SUB_SCORE_CEILING);
        }

        private BigDecimal impact(BigDecimal factor, int exponent) {
            BigDecimal subScore = subScore();
            return scopeChanged()
                    ? changedImpact(subScore, factor, exponent)
                    : UNCHANGED_IMPACT_COEFFICIENT.multiply(subScore);
        }

        private static BigDecimal changedImpact(BigDecimal subScore, BigDecimal factor, int exponent) {
            BigDecimal growth = CHANGED_IMPACT_COEFFICIENT.multiply(subScore.subtract(CHANGED_IMPACT_OFFSET));
            BigDecimal shifted = subScore.multiply(factor).subtract(CHANGED_IMPACT_SHIFT);
            return growth.subtract(CHANGED_IMPACT_PENALTY.multiply(shifted.pow(exponent, PRECISION)));
        }

        private BigDecimal weight(String code) {
            return catalog.get(code)
                    .value(vector.get(code))
                    .orElseThrow(() -> new IllegalArgumentException("unknown value for metric " + code))
                    .weight(scopeChanged());
        }
    }
}
