package org.challenge.vulnseverityevaluator.datasource.repository;

import org.challenge.vulnseverityevaluator.domain.model.SchemeMetric;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * The scoring specification, read from the database.
 * <p>
 * Every query binds the scheme identifier as a parameter, so there is nowhere for a concatenated query to appear.
 * The version projection deliberately returns a scalar rather than materialising the specification.
 */
@Repository
public interface SchemeMetricRepository extends JpaRepository<SchemeMetric, String> {

    List<SchemeMetric> findBySchemeIdOrderByOrdinal(String schemeId);

    @Query("select distinct metric.catalogVersion from SchemeMetric metric where metric.schemeId = :schemeId")
    List<String> findCatalogVersionsBySchemeId(@Param("schemeId") String schemeId);
}
