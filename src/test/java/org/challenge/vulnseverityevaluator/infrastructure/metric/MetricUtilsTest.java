package org.challenge.vulnseverityevaluator.infrastructure.metric;

import org.challenge.vulnseverityevaluator.infrastructure.metric.MetricUtils.Tags;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.challenge.vulnseverityevaluator.infrastructure.metric.MetricUtils.Dimensions.HTTP_STATUS;

/**
 * The vocabulary. The two bucketing cases at their edges matter most: a delta of exactly the review threshold has to
 * fall on the significant side, or the dashboard bucket and the rule that forces a human review stop agreeing.
 */
class MetricUtilsTest {

    @Test
    void givenValueWhenMappingDimensionThenReturnNameAndValue() {
        assertThat(HTTP_STATUS.mapDimension(201)).isEqualTo("status:201");
    }

    /**
     * A missing value is spelled out rather than dropped: a dimension that silently disappears makes the series it
     * belongs to incomparable over time.
     */
    @Test
    void givenNullWhenMappingDimensionThenReturnUnknown() {
        assertThat(HTTP_STATUS.mapDimension(null)).isEqualTo("status:unknown");
    }

    @ParameterizedTest
    @CsvSource({"0.0, none", "0.1, minor", "1.9, minor", "2.0, significant", "4.2, significant", "-2.0, significant"})
    void givenDeltaWhenBucketingThenReturnBucket(String delta, String expected) {
        assertThat(MetricUtils.delta(new BigDecimal(delta))).isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource({"2.0, up", "0.1, up", "0.0, flat", "-0.1, down", "-2.0, down"})
    void givenDeltaWhenResolvingDirectionThenReturnDirection(String delta, String expected) {
        assertThat(MetricUtils.direction(new BigDecimal(delta))).isEqualTo(expected);
    }

    /**
     * The taxonomy is the contract a dashboard groups by, so a tag that belongs to no category would be invisible to
     * every query written against it.
     */
    @Test
    void givenEveryTagWhenReadingThenReturnKnownCategoryPrefix() {
        assertThat(Tags.values())
                .allSatisfy(tag -> assertThat(tag.getTag()).matches("(work|event|resource)\\.[a-z.]+"));
    }
}
