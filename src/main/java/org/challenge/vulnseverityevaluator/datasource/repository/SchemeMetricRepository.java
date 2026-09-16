package org.challenge.vulnseverityevaluator.datasource.repository;

import org.challenge.vulnseverityevaluator.domain.model.SchemeMetric;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * The scoring specification, read from the database.
 * <p>
 * Derived queries only: the scheme identifier is bound as a parameter, so there is nowhere for a concatenated query
 * to appear.
 */
@Repository
public interface SchemeMetricRepository extends JpaRepository<SchemeMetric, String> {

    List<SchemeMetric> findBySchemeIdOrderByOrdinal(String schemeId);
}
