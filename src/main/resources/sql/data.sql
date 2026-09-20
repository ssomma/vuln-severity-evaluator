-- CVSS v3.1 scoring specification and context catalog.
-- The specification is data: metrics, admitted values and coefficients live here instead of in the
-- calculator, so correcting a weight or adding a scheme is a row rather than a code change.

-- One row per metric, carrying its weights and the guidance the model reads. A metric is scored twice
-- (from the baseline vector and from the declared context) but that is how the scheme computes, not two
-- metrics: the X (Not Defined) abstention is protocol, not an admitted value, so it is not seeded.
INSERT INTO scheme_metric (id, scheme_id, code, label, guidance, ordinal, catalog_version) VALUES ('CVSS31-AV', 'CVSS:3.1', 'AV', 'Attack Vector', 'Reachability of the vulnerable component in this deployment: N network, A adjacent, L local, P physical. Answer X (Not Defined) when the declared context gives no evidence: the baseline value is kept.', 1, 'cvss31-context-v1');
INSERT INTO scheme_metric_value (metric_id, code, weight, weight_when_scope_changed) VALUES ('CVSS31-AV', 'N', 0.85, 0.85);
INSERT INTO scheme_metric_value (metric_id, code, weight, weight_when_scope_changed) VALUES ('CVSS31-AV', 'A', 0.62, 0.62);
INSERT INTO scheme_metric_value (metric_id, code, weight, weight_when_scope_changed) VALUES ('CVSS31-AV', 'L', 0.55, 0.55);
INSERT INTO scheme_metric_value (metric_id, code, weight, weight_when_scope_changed) VALUES ('CVSS31-AV', 'P', 0.2, 0.2);
INSERT INTO scheme_metric (id, scheme_id, code, label, guidance, ordinal, catalog_version) VALUES ('CVSS31-AC', 'CVSS:3.1', 'AC', 'Attack Complexity', 'Whether controls in this deployment make the attack harder: L low, H high. Answer X (Not Defined) when the declared context gives no evidence: the baseline value is kept.', 2, 'cvss31-context-v1');
INSERT INTO scheme_metric_value (metric_id, code, weight, weight_when_scope_changed) VALUES ('CVSS31-AC', 'L', 0.77, 0.77);
INSERT INTO scheme_metric_value (metric_id, code, weight, weight_when_scope_changed) VALUES ('CVSS31-AC', 'H', 0.44, 0.44);
INSERT INTO scheme_metric (id, scheme_id, code, label, guidance, ordinal, catalog_version) VALUES ('CVSS31-PR', 'CVSS:3.1', 'PR', 'Privileges Required', 'Privileges an attacker needs in this deployment: N none, L low, H high. Answer X (Not Defined) when the declared context gives no evidence: the baseline value is kept.', 3, 'cvss31-context-v1');
INSERT INTO scheme_metric_value (metric_id, code, weight, weight_when_scope_changed) VALUES ('CVSS31-PR', 'N', 0.85, 0.85);
INSERT INTO scheme_metric_value (metric_id, code, weight, weight_when_scope_changed) VALUES ('CVSS31-PR', 'L', 0.62, 0.68);
INSERT INTO scheme_metric_value (metric_id, code, weight, weight_when_scope_changed) VALUES ('CVSS31-PR', 'H', 0.27, 0.5);
INSERT INTO scheme_metric (id, scheme_id, code, label, guidance, ordinal, catalog_version) VALUES ('CVSS31-UI', 'CVSS:3.1', 'UI', 'User Interaction', 'Whether a user must act for the attack to succeed here: N none, R required. Answer X (Not Defined) when the declared context gives no evidence: the baseline value is kept.', 4, 'cvss31-context-v1');
INSERT INTO scheme_metric_value (metric_id, code, weight, weight_when_scope_changed) VALUES ('CVSS31-UI', 'N', 0.85, 0.85);
INSERT INTO scheme_metric_value (metric_id, code, weight, weight_when_scope_changed) VALUES ('CVSS31-UI', 'R', 0.62, 0.62);
INSERT INTO scheme_metric (id, scheme_id, code, label, guidance, ordinal, catalog_version) VALUES ('CVSS31-S', 'CVSS:3.1', 'S', 'Scope', 'Whether exploitation can affect components beyond the vulnerable one here: U unchanged, C changed. Answer X (Not Defined) when the declared context gives no evidence: the baseline value is kept.', 5, 'cvss31-context-v1');
INSERT INTO scheme_metric_value (metric_id, code, weight, weight_when_scope_changed) VALUES ('CVSS31-S', 'U', 0, 0);
INSERT INTO scheme_metric_value (metric_id, code, weight, weight_when_scope_changed) VALUES ('CVSS31-S', 'C', 0, 0);
INSERT INTO scheme_metric (id, scheme_id, code, label, guidance, ordinal, catalog_version) VALUES ('CVSS31-C', 'CVSS:3.1', 'C', 'Confidentiality Impact', 'Confidentiality loss in this deployment: H high, L low, N none. Answer X (Not Defined) when the declared context gives no evidence: the baseline value is kept.', 6, 'cvss31-context-v1');
INSERT INTO scheme_metric_value (metric_id, code, weight, weight_when_scope_changed) VALUES ('CVSS31-C', 'H', 0.56, 0.56);
INSERT INTO scheme_metric_value (metric_id, code, weight, weight_when_scope_changed) VALUES ('CVSS31-C', 'L', 0.22, 0.22);
INSERT INTO scheme_metric_value (metric_id, code, weight, weight_when_scope_changed) VALUES ('CVSS31-C', 'N', 0, 0);
INSERT INTO scheme_metric (id, scheme_id, code, label, guidance, ordinal, catalog_version) VALUES ('CVSS31-I', 'CVSS:3.1', 'I', 'Integrity Impact', 'Integrity loss in this deployment: H high, L low, N none. Answer X (Not Defined) when the declared context gives no evidence: the baseline value is kept.', 7, 'cvss31-context-v1');
INSERT INTO scheme_metric_value (metric_id, code, weight, weight_when_scope_changed) VALUES ('CVSS31-I', 'H', 0.56, 0.56);
INSERT INTO scheme_metric_value (metric_id, code, weight, weight_when_scope_changed) VALUES ('CVSS31-I', 'L', 0.22, 0.22);
INSERT INTO scheme_metric_value (metric_id, code, weight, weight_when_scope_changed) VALUES ('CVSS31-I', 'N', 0, 0);
INSERT INTO scheme_metric (id, scheme_id, code, label, guidance, ordinal, catalog_version) VALUES ('CVSS31-A', 'CVSS:3.1', 'A', 'Availability Impact', 'Availability loss in this deployment: H high, L low, N none. Answer X (Not Defined) when the declared context gives no evidence: the baseline value is kept.', 8, 'cvss31-context-v1');
INSERT INTO scheme_metric_value (metric_id, code, weight, weight_when_scope_changed) VALUES ('CVSS31-A', 'H', 0.56, 0.56);
INSERT INTO scheme_metric_value (metric_id, code, weight, weight_when_scope_changed) VALUES ('CVSS31-A', 'L', 0.22, 0.22);
INSERT INTO scheme_metric_value (metric_id, code, weight, weight_when_scope_changed) VALUES ('CVSS31-A', 'N', 0, 0);

-- The Security Requirements: not part of the baseline vector, so the model is their only source. An
-- abstention weighs 1.0, which the calculator applies as the neutral factor rather than storing it.
INSERT INTO scheme_metric (id, scheme_id, code, label, guidance, ordinal, catalog_version) VALUES ('CVSS31-CR', 'CVSS:3.1', 'CR', 'Confidentiality Requirement', 'How much the confidentiality of the data this application handles matters. Answer X (Not Defined) when the declared context gives no evidence: the baseline value is kept.', 9, 'cvss31-context-v1');
INSERT INTO scheme_metric_value (metric_id, code, weight, weight_when_scope_changed) VALUES ('CVSS31-CR', 'H', 1.5, 1.5);
INSERT INTO scheme_metric_value (metric_id, code, weight, weight_when_scope_changed) VALUES ('CVSS31-CR', 'M', 1.0, 1.0);
INSERT INTO scheme_metric_value (metric_id, code, weight, weight_when_scope_changed) VALUES ('CVSS31-CR', 'L', 0.5, 0.5);
INSERT INTO scheme_metric (id, scheme_id, code, label, guidance, ordinal, catalog_version) VALUES ('CVSS31-IR', 'CVSS:3.1', 'IR', 'Integrity Requirement', 'How much the integrity of the data this application handles matters. Answer X (Not Defined) when the declared context gives no evidence: the baseline value is kept.', 10, 'cvss31-context-v1');
INSERT INTO scheme_metric_value (metric_id, code, weight, weight_when_scope_changed) VALUES ('CVSS31-IR', 'H', 1.5, 1.5);
INSERT INTO scheme_metric_value (metric_id, code, weight, weight_when_scope_changed) VALUES ('CVSS31-IR', 'M', 1.0, 1.0);
INSERT INTO scheme_metric_value (metric_id, code, weight, weight_when_scope_changed) VALUES ('CVSS31-IR', 'L', 0.5, 0.5);
INSERT INTO scheme_metric (id, scheme_id, code, label, guidance, ordinal, catalog_version) VALUES ('CVSS31-AR', 'CVSS:3.1', 'AR', 'Availability Requirement', 'How much the availability of this application matters. Answer X (Not Defined) when the declared context gives no evidence: the baseline value is kept.', 11, 'cvss31-context-v1');
INSERT INTO scheme_metric_value (metric_id, code, weight, weight_when_scope_changed) VALUES ('CVSS31-AR', 'H', 1.5, 1.5);
INSERT INTO scheme_metric_value (metric_id, code, weight, weight_when_scope_changed) VALUES ('CVSS31-AR', 'M', 1.0, 1.0);
INSERT INTO scheme_metric_value (metric_id, code, weight, weight_when_scope_changed) VALUES ('CVSS31-AR', 'L', 0.5, 0.5);

-- Context catalog: the allowlist for the declared context, the meanings the model reads, and the metric
-- values each attribute argues for, which is what lets the deterministic model work without hardcoded
-- mapping tables.
INSERT INTO context_attribute (id, kind, code, meaning) VALUES ('EXPOSURE-INTERNET_FACING', 'EXPOSURE', 'INTERNET_FACING', 'the application is reachable from the public internet');
INSERT INTO context_attribute (id, kind, code, meaning) VALUES ('EXPOSURE-INTERNAL_NETWORK', 'EXPOSURE', 'INTERNAL_NETWORK', 'the vulnerable component accepts traffic only from hosts on the same trusted local network segment and is not routed from other internal networks or the public internet');
INSERT INTO context_suggestion (attribute_id, metric, suggested) VALUES ('EXPOSURE-INTERNAL_NETWORK', 'AV', 'A');
INSERT INTO context_attribute (id, kind, code, meaning) VALUES ('EXPOSURE-ISOLATED', 'EXPOSURE', 'ISOLATED', 'the application has no inbound network reachability');
INSERT INTO context_suggestion (attribute_id, metric, suggested) VALUES ('EXPOSURE-ISOLATED', 'AV', 'L');
INSERT INTO context_attribute (id, kind, code, meaning) VALUES ('DATA_CLASSIFICATION-CREDENTIALS', 'DATA_CLASSIFICATION', 'CREDENTIALS', 'unauthorized disclosure of the credentials, tokens or keys handled by the application would have severe confidentiality consequences');
INSERT INTO context_suggestion (attribute_id, metric, suggested) VALUES ('DATA_CLASSIFICATION-CREDENTIALS', 'CR', 'H');
INSERT INTO context_attribute (id, kind, code, meaning) VALUES ('DATA_CLASSIFICATION-FINANCIAL', 'DATA_CLASSIFICATION', 'FINANCIAL', 'unauthorized disclosure of the financial or payment data handled by the application would have severe confidentiality consequences');
INSERT INTO context_suggestion (attribute_id, metric, suggested) VALUES ('DATA_CLASSIFICATION-FINANCIAL', 'CR', 'H');
INSERT INTO context_attribute (id, kind, code, meaning) VALUES ('DATA_CLASSIFICATION-PII', 'DATA_CLASSIFICATION', 'PII', 'unauthorized disclosure of the personally identifiable information handled by the application would have severe confidentiality consequences');
INSERT INTO context_suggestion (attribute_id, metric, suggested) VALUES ('DATA_CLASSIFICATION-PII', 'CR', 'H');
INSERT INTO context_attribute (id, kind, code, meaning) VALUES ('DATA_CLASSIFICATION-INTERNAL', 'DATA_CLASSIFICATION', 'INTERNAL', 'unauthorized disclosure of the internal, non-personal and non-financial data handled by the application would have material but not severe confidentiality consequences');
INSERT INTO context_suggestion (attribute_id, metric, suggested) VALUES ('DATA_CLASSIFICATION-INTERNAL', 'CR', 'M');
INSERT INTO context_attribute (id, kind, code, meaning) VALUES ('DATA_CLASSIFICATION-PUBLIC', 'DATA_CLASSIFICATION', 'PUBLIC', 'the application handles only public data, so loss of confidentiality would have limited consequences');
INSERT INTO context_suggestion (attribute_id, metric, suggested) VALUES ('DATA_CLASSIFICATION-PUBLIC', 'CR', 'L');
INSERT INTO context_attribute (id, kind, code, meaning) VALUES ('BUSINESS_CRITICALITY-TIER_1', 'BUSINESS_CRITICALITY', 'TIER_1', 'unauthorized modification of application data or an outage would stop a core business flow and have severe consequences');
INSERT INTO context_suggestion (attribute_id, metric, suggested) VALUES ('BUSINESS_CRITICALITY-TIER_1', 'IR', 'H');
INSERT INTO context_suggestion (attribute_id, metric, suggested) VALUES ('BUSINESS_CRITICALITY-TIER_1', 'AR', 'H');
INSERT INTO context_attribute (id, kind, code, meaning) VALUES ('BUSINESS_CRITICALITY-TIER_2', 'BUSINESS_CRITICALITY', 'TIER_2', 'unauthorized modification of application data or an outage would degrade a business flow and have material but not severe consequences');
INSERT INTO context_suggestion (attribute_id, metric, suggested) VALUES ('BUSINESS_CRITICALITY-TIER_2', 'IR', 'M');
INSERT INTO context_suggestion (attribute_id, metric, suggested) VALUES ('BUSINESS_CRITICALITY-TIER_2', 'AR', 'M');
INSERT INTO context_attribute (id, kind, code, meaning) VALUES ('BUSINESS_CRITICALITY-TIER_3', 'BUSINESS_CRITICALITY', 'TIER_3', 'unauthorized modification of application data or an outage would have limited business consequences');
INSERT INTO context_suggestion (attribute_id, metric, suggested) VALUES ('BUSINESS_CRITICALITY-TIER_3', 'IR', 'L');
INSERT INTO context_suggestion (attribute_id, metric, suggested) VALUES ('BUSINESS_CRITICALITY-TIER_3', 'AR', 'L');
INSERT INTO context_attribute (id, kind, code, meaning) VALUES ('COMPENSATING_CONTROL-WAF', 'COMPENSATING_CONTROL', 'WAF', 'a web application firewall filters inbound traffic');
INSERT INTO context_attribute (id, kind, code, meaning) VALUES ('COMPENSATING_CONTROL-EXPLOIT_SPECIFIC_WAF_RULE', 'COMPENSATING_CONTROL', 'EXPLOIT_SPECIFIC_WAF_RULE', 'a tested rule blocks the direct exploit payload and successful exploitation requires constructing a variant that bypasses the rule');
INSERT INTO context_suggestion (attribute_id, metric, suggested) VALUES ('COMPENSATING_CONTROL-EXPLOIT_SPECIFIC_WAF_RULE', 'AC', 'H');
INSERT INTO context_attribute (id, kind, code, meaning) VALUES ('COMPENSATING_CONTROL-STRICT_INPUT_VALIDATION', 'COMPENSATING_CONTROL', 'STRICT_INPUT_VALIDATION', 'inputs reaching the component are validated against an allowlist');
INSERT INTO context_suggestion (attribute_id, metric, suggested) VALUES ('COMPENSATING_CONTROL-STRICT_INPUT_VALIDATION', 'AC', 'H');
INSERT INTO context_attribute (id, kind, code, meaning) VALUES ('COMPENSATING_CONTROL-RUNTIME_PROTECTION', 'COMPENSATING_CONTROL', 'RUNTIME_PROTECTION', 'a runtime agent blocks exploitation attempts');
INSERT INTO context_suggestion (attribute_id, metric, suggested) VALUES ('COMPENSATING_CONTROL-RUNTIME_PROTECTION', 'AC', 'H');
INSERT INTO context_attribute (id, kind, code, meaning) VALUES ('COMPENSATING_CONTROL-NETWORK_SEGMENTATION', 'COMPENSATING_CONTROL', 'NETWORK_SEGMENTATION', 'the component is segmented from untrusted networks');
INSERT INTO context_attribute (id, kind, code, meaning) VALUES ('COMPENSATING_CONTROL-MUTUAL_TLS', 'COMPENSATING_CONTROL', 'MUTUAL_TLS', 'callers are authenticated with mutual TLS');
INSERT INTO context_attribute (id, kind, code, meaning) VALUES ('COMPENSATING_CONTROL-READ_ONLY_FILESYSTEM', 'COMPENSATING_CONTROL', 'READ_ONLY_FILESYSTEM', 'the component runs with a read only filesystem');
INSERT INTO context_attribute (id, kind, code, meaning) VALUES ('COMPENSATING_CONTROL-LEAST_PRIVILEGE_RUNTIME', 'COMPENSATING_CONTROL', 'LEAST_PRIVILEGE_RUNTIME', 'the component runs without privileged permissions');
INSERT INTO context_attribute (id, kind, code, meaning) VALUES ('RUNTIME-JAVA', 'RUNTIME', 'JAVA', 'the application runs on the Java virtual machine');
INSERT INTO context_attribute (id, kind, code, meaning) VALUES ('RUNTIME-KOTLIN', 'RUNTIME', 'KOTLIN', 'the application runs on the Java virtual machine');
INSERT INTO context_attribute (id, kind, code, meaning) VALUES ('RUNTIME-GO', 'RUNTIME', 'GO', 'the application compiled Go binary');
INSERT INTO context_attribute (id, kind, code, meaning) VALUES ('RUNTIME-NODE', 'RUNTIME', 'NODE', 'the application runs on Node.js');
INSERT INTO context_attribute (id, kind, code, meaning) VALUES ('RUNTIME-PYTHON', 'RUNTIME', 'PYTHON', 'the application runs on CPython');
INSERT INTO context_attribute (id, kind, code, meaning) VALUES ('RUNTIME-RUBY', 'RUNTIME', 'RUBY', 'the application runs on Ruby');
INSERT INTO context_attribute (id, kind, code, meaning) VALUES ('RUNTIME-PHP', 'RUNTIME', 'PHP', 'the application runs on PHP');
INSERT INTO context_attribute (id, kind, code, meaning) VALUES ('RUNTIME-DOTNET', 'RUNTIME', 'DOTNET', 'the application runs on .NET');
INSERT INTO context_attribute (id, kind, code, meaning) VALUES ('RUNTIME-RUST', 'RUNTIME', 'RUST', 'the application compiled Rust binary');
INSERT INTO context_attribute (id, kind, code, meaning) VALUES ('RUNTIME-SPRING_BOOT', 'RUNTIME', 'SPRING_BOOT', 'the application spring Boot application');
INSERT INTO context_attribute (id, kind, code, meaning) VALUES ('RUNTIME-QUARKUS', 'RUNTIME', 'QUARKUS', 'the application quarkus application');
INSERT INTO context_attribute (id, kind, code, meaning) VALUES ('RUNTIME-EXPRESS', 'RUNTIME', 'EXPRESS', 'the application express application');
INSERT INTO context_attribute (id, kind, code, meaning) VALUES ('RUNTIME-DJANGO', 'RUNTIME', 'DJANGO', 'the application django application');
INSERT INTO context_attribute (id, kind, code, meaning) VALUES ('RUNTIME-FLASK', 'RUNTIME', 'FLASK', 'the application flask application');
INSERT INTO context_attribute (id, kind, code, meaning) VALUES ('RUNTIME-RAILS', 'RUNTIME', 'RAILS', 'the application rails application');
INSERT INTO context_attribute (id, kind, code, meaning) VALUES ('RUNTIME-CONTAINER', 'RUNTIME', 'CONTAINER', 'the application deployed as a container');
INSERT INTO context_attribute (id, kind, code, meaning) VALUES ('RUNTIME-SERVERLESS', 'RUNTIME', 'SERVERLESS', 'the application deployed as a serverless function');
INSERT INTO context_attribute (id, kind, code, meaning) VALUES ('RUNTIME-VIRTUAL_MACHINE', 'RUNTIME', 'VIRTUAL_MACHINE', 'the application deployed on a virtual machine');
