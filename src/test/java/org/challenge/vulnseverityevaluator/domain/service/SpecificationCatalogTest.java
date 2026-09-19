package org.challenge.vulnseverityevaluator.domain.service;

import org.challenge.vulnseverityevaluator.datasource.repository.ContextAttributeRepository;
import org.challenge.vulnseverityevaluator.datasource.repository.SchemeMetricRepository;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SpecificationCatalogTest {

    private final SchemeMetricRepository metrics = mock(SchemeMetricRepository.class);
    private final SpecificationCatalog catalog = new SpecificationCatalog(
            metrics, mock(ContextAttributeRepository.class));

    @Test
    void givenOneCatalogVersionWhenReadingThenReturnVersion() {
        when(metrics.findCatalogVersionsBySchemeId("CVSS:3.1")).thenReturn(List.of("catalog-v1"));

        assertThat(catalog.version("CVSS:3.1")).isEqualTo("catalog-v1");
    }

    @Test
    void givenInconsistentCatalogVersionsWhenReadingThenThrowIllegalStateException() {
        when(metrics.findCatalogVersionsBySchemeId("CVSS:3.1"))
                .thenReturn(List.of("catalog-v1", "catalog-v2"));

        assertThatThrownBy(() -> catalog.version("CVSS:3.1"))
                .isInstanceOf(IllegalStateException.class);
    }
}
