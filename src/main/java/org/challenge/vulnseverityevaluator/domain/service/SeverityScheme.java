package org.challenge.vulnseverityevaluator.domain.service;

import org.challenge.vulnseverityevaluator.domain.model.ModelSeverityProposal;
import org.challenge.vulnseverityevaluator.domain.model.SchemeMetric;
import org.challenge.vulnseverityevaluator.domain.model.SeverityAssessment;

import java.util.List;

/**
 * Contract of a severity scoring scheme: behaviour, not data.
 * <p>
 * It lives with the services because it applies logic — it parses a vector and computes scores. The metric catalog it
 * works from arrives as an argument rather than being fetched here, so a scheme is a pure function of its inputs:
 * testable without a database and unable to reach a datasource by accident.
 * <p>
 * Replacing CVSS v3.1 with another version means adding an implementation and its catalog rows. No service,
 * controller, entity or datasource has to change.
 */
public interface SeverityScheme {

    String id();

    void validateBaselineVector(String vector, List<SchemeMetric> metrics);

    SeverityAssessment assess(String baselineVector, ModelSeverityProposal proposal, List<SchemeMetric> metrics);
}
