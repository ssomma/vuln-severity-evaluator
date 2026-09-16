package org.challenge.vulnseverityevaluator.presentation.controller;

/**
 * The request bodies both controller tests work from: one application where the context argues for a higher
 * severity, one where it argues for a lower one. Sharing them keeps the two tests describing the same scenario.
 */
final class TestRequests {

    static final String LOG4SHELL_VECTOR = "CVSS:3.1/AV:N/AC:L/PR:N/UI:N/S:C/C:H/I:H/A:H";
    static final String DESCRIPTION = "Remote code execution through JNDI lookup in a logging library.";

    static final String CRITICAL_APPLICATION = """
            {
              "vulnerability": {
                "identifier": "CVE-2021-44228",
                "description": "%s",
                "baseline_vector": "%s"
              },
              "application": {
                "name": "checkout-api",
                "risk_profile": {
                  "exposure": "INTERNET_FACING",
                  "data_classification": "PII",
                  "business_criticality": "TIER_1"
                },
                "runtime": ["JAVA", "SPRING_BOOT"],
                "compensating_controls": []
              }
            }""".formatted(DESCRIPTION, LOG4SHELL_VECTOR);

    static final String ISOLATED_APPLICATION = """
            {
              "vulnerability": {
                "identifier": "CVE-2021-44228",
                "description": "%s",
                "baseline_vector": "%s"
              },
              "application": {
                "name": "batch-reporter",
                "risk_profile": {
                  "exposure": "ISOLATED",
                  "data_classification": "PUBLIC",
                  "business_criticality": "TIER_3"
                },
                "runtime": ["JAVA"],
                "compensating_controls": ["NETWORK_SEGMENTATION"]
              }
            }""".formatted(DESCRIPTION, LOG4SHELL_VECTOR);

    private TestRequests() {
    }
}
