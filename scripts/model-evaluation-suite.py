#!/usr/bin/env python3
"""Run the documented model/provider evaluation suite against a live API."""

from __future__ import annotations

import argparse
import json
import sys
import time
from datetime import datetime, timezone
from decimal import Decimal, InvalidOperation
from pathlib import Path
from typing import Any
from urllib.error import HTTPError, URLError
from urllib.parse import urljoin
from urllib.request import Request, urlopen


ADMITTED_METRIC_VALUES = {
    "AV": {"N", "A", "L", "P", "X"},
    "AC": {"L", "H", "X"},
    "PR": {"N", "L", "H", "X"},
    "UI": {"N", "R", "X"},
    "S": {"U", "C", "X"},
    "C": {"H", "L", "N", "X"},
    "I": {"H", "L", "N", "X"},
    "A": {"H", "L", "N", "X"},
    "CR": {"H", "M", "L", "X"},
    "IR": {"H", "M", "L", "X"},
    "AR": {"H", "M", "L", "X"},
}


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Evaluate one configured model/provider through vuln-severity-evaluator."
    )
    parser.add_argument("--base-url", default="http://localhost:8080")
    parser.add_argument(
        "--cases",
        type=Path,
        default=Path(__file__).with_name("model-evaluation-cases.json"),
    )
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--provider-label", required=True)
    parser.add_argument("--model-label", required=True)
    parser.add_argument(
        "--expected-provenance-model",
        help="Fail a successful case when provenance.model differs from this value.",
    )
    parser.add_argument("--timeout-seconds", type=float, default=300.0)
    parser.add_argument(
        "--verify-idempotency",
        action="store_true",
        help="Repeat the first case and require the same evaluation_id.",
    )
    return parser.parse_args()


def require(mapping: dict[str, Any], *names: str) -> Any:
    for name in names:
        if name in mapping:
            return mapping[name]
    raise KeyError(" or ".join(names))


def expected_contract(fixture: dict[str, Any]) -> tuple[str, dict[str, str]]:
    """Return the score relation and the evidence-bearing metric subset to verify."""
    expected = require(fixture, "expected")
    legacy_relation = isinstance(expected, str)
    if legacy_relation:  # Compatibility with fixtures created by the first suite revision.
        relation, metrics = expected, {}
    elif isinstance(expected, dict):
        relation = require(expected, "score_relation")
        metrics = expected.get("metrics", {})
    else:
        raise ValueError("expected must be a relation or an object")
    if relation not in {"MATCH", "DIFFERENT"}:
        raise ValueError("expected.score_relation must be MATCH or DIFFERENT")
    if not isinstance(metrics, dict) or (not metrics and not legacy_relation):
        raise ValueError("expected.metrics must be a non-empty object")
    if not all(isinstance(metric, str) and isinstance(value, str) for metric, value in metrics.items()):
        raise ValueError("expected.metrics must map metric names to string values")
    invalid = {
        metric: value
        for metric, value in metrics.items()
        if metric not in ADMITTED_METRIC_VALUES or value not in ADMITTED_METRIC_VALUES[metric]
    }
    if invalid:
        raise ValueError(f"expected.metrics contains invalid CVSS v3.1 choices: {invalid}")
    return relation, metrics


def metric_values(justification: Any) -> dict[str, str]:
    if not isinstance(justification, list):
        raise ValueError("justification must be an array")
    values: dict[str, str] = {}
    for choice in justification:
        if not isinstance(choice, dict):
            raise ValueError("each justification entry must be an object")
        metric = require(choice, "metric")
        value = require(choice, "value")
        if metric in values:
            raise ValueError(f"duplicate metric in justification: {metric}")
        values[str(metric)] = str(value)
    return values


def request_json(
    method: str, url: str, timeout: float, payload: dict[str, Any] | None = None
) -> tuple[int, dict[str, str], dict[str, Any]]:
    body = None if payload is None else json.dumps(payload).encode("utf-8")
    request = Request(url, data=body, method=method)
    request.add_header("Accept", "application/json")
    if body is not None:
        request.add_header("Content-Type", "application/json")
    with urlopen(request, timeout=timeout) as response:
        raw = response.read().decode("utf-8")
        parsed = json.loads(raw)
        if not isinstance(parsed, dict):
            raise ValueError("API response must be a JSON object")
        return response.status, dict(response.headers.items()), parsed


def decimal_value(value: Any) -> Decimal:
    try:
        return Decimal(str(value))
    except (InvalidOperation, ValueError) as error:
        raise ValueError(f"invalid numeric score: {value!r}") from error


def normalized_response(response: dict[str, Any]) -> dict[str, Any]:
    """Compare durable semantics despite collection order and database timestamp precision."""
    normalized = dict(response)
    justification = normalized.get("justification")
    if isinstance(justification, list):
        normalized["justification"] = sorted(
            justification,
            key=lambda choice: str(choice.get("metric", "")) if isinstance(choice, dict) else "",
        )
    provenance = normalized.get("provenance")
    if isinstance(provenance, dict):
        normalized_provenance = dict(provenance)
        if normalized_provenance.get("evaluated_at") or normalized_provenance.get("evaluatedAt"):
            normalized_provenance.pop("evaluated_at", None)
            normalized_provenance.pop("evaluatedAt", None)
            normalized_provenance["evaluated_at_present"] = True
        normalized["provenance"] = normalized_provenance
    return normalized


def compact_error(error: Exception) -> tuple[str, int | None, str]:
    if isinstance(error, HTTPError):
        return "HTTP_ERROR", error.code, error.read().decode("utf-8", errors="replace")[:2000]
    if isinstance(error, URLError):
        return "CONNECTION_ERROR", None, str(error.reason)
    return type(error).__name__, None, str(error)[:2000]


def evaluate_case(
    base_url: str,
    fixture: dict[str, Any],
    timeout: float,
    expected_provenance_model: str | None,
) -> dict[str, Any]:
    started = time.perf_counter()
    case_name = require(fixture, "case")
    expected_relation, expected_metrics = expected_contract(fixture)
    result: dict[str, Any] = {
        "case": case_name,
        "expected_relation": expected_relation,
        "expected_metrics": expected_metrics,
    }

    try:
        endpoint = base_url.rstrip("/") + "/vulnerability-evaluations"
        status, headers, response = request_json(
            "POST", endpoint, timeout, require(fixture, "body")
        )
        if status != 201:
            raise ValueError(f"expected HTTP 201, received {status}")

        evaluation_id = require(response, "evaluation_id", "evaluationId")
        location = headers.get("Location") or headers.get("location")
        if not location or not location.rstrip("/").endswith("/" + str(evaluation_id)):
            raise ValueError("Location header does not identify evaluation_id")

        baseline = require(response, "baseline")
        contextual = require(response, "contextual")
        provenance = require(response, "provenance")
        baseline_score = decimal_value(require(baseline, "score"))
        contextual_score = decimal_value(require(contextual, "score"))
        actual = "MATCH" if baseline_score == contextual_score else "DIFFERENT"
        provenance_model = require(provenance, "model", "model_identifier", "modelIdentifier")
        justification = require(response, "justification")
        actual_metrics = metric_values(justification)
        metric_mismatches = {
            metric: {"expected": expected, "actual": actual_metrics.get(metric)}
            for metric, expected in expected_metrics.items()
            if actual_metrics.get(metric) != expected
        }
        relation_verification = "PASS" if actual == expected_relation else "FAIL"
        metrics_verification = "PASS" if not metric_mismatches else "FAIL"

        read_status, _, readback = request_json(
            "GET", urljoin(base_url.rstrip("/") + "/", location.lstrip("/")), timeout
        )
        readback_verification = (
            "PASS"
            if read_status == 200
            and normalized_response(readback) == normalized_response(response)
            else "FAIL"
        )
        model_verification = (
            "PASS"
            if expected_provenance_model is None or provenance_model == expected_provenance_model
            else "FAIL"
        )

        result.update(
            {
                "status": "SUCCESS",
                "http_status": status,
                "location": location,
                "evaluation_id": evaluation_id,
                "provenance_model": provenance_model,
                "prompt_version": require(provenance, "prompt_version", "promptVersion"),
                "catalog_version": require(provenance, "catalog_version", "catalogVersion"),
                "policy_version": require(provenance, "policy_version", "policyVersion"),
                "baseline_source": require(
                    provenance, "baseline_vector_source", "baselineVectorSource"
                ),
                "confidence": require(provenance, "confidence"),
                "review_required": require(
                    provenance, "review_required", "reviewRequired"
                ),
                "evaluated_at": require(provenance, "evaluated_at", "evaluatedAt"),
                "baseline": baseline,
                "contextual": contextual,
                "delta": require(response, "delta"),
                "actual_relation": actual,
                "actual_metrics": actual_metrics,
                "relation_verification": relation_verification,
                "metrics_verification": metrics_verification,
                "metric_mismatches": metric_mismatches,
                "expectation_verification": (
                    "PASS"
                    if relation_verification == "PASS" and metrics_verification == "PASS"
                    else "FAIL"
                ),
                "model_verification": model_verification,
                "readback_verification": readback_verification,
                "summary": require(response, "summary"),
                "justification": justification,
            }
        )
    except Exception as error:  # Continue the matrix and preserve per-case evidence.
        error_type, http_status, detail = compact_error(error)
        result.update(
            {
                "status": "ERROR",
                "http_status": http_status,
                "error_type": error_type,
                "error": detail,
                "expectation_verification": "ERROR",
                "relation_verification": "ERROR",
                "metrics_verification": "ERROR",
                "model_verification": "ERROR",
                "readback_verification": "ERROR",
            }
        )

    result["elapsed_ms"] = round((time.perf_counter() - started) * 1000)
    return result


def verify_idempotency(
    base_url: str, fixture: dict[str, Any], first_result: dict[str, Any], timeout: float
) -> dict[str, Any]:
    if first_result.get("status") != "SUCCESS":
        return {"status": "SKIPPED", "reason": "first case did not succeed"}
    started = time.perf_counter()
    try:
        status, _, response = request_json(
            "POST",
            base_url.rstrip("/") + "/vulnerability-evaluations",
            timeout,
            require(fixture, "body"),
        )
        repeated_id = require(response, "evaluation_id", "evaluationId")
        original_id = first_result["evaluation_id"]
        return {
            "status": "PASS" if status == 201 and repeated_id == original_id else "FAIL",
            "http_status": status,
            "original_evaluation_id": original_id,
            "repeated_evaluation_id": repeated_id,
            "elapsed_ms": round((time.perf_counter() - started) * 1000),
        }
    except Exception as error:
        error_type, http_status, detail = compact_error(error)
        return {
            "status": "ERROR",
            "http_status": http_status,
            "error_type": error_type,
            "error": detail,
            "elapsed_ms": round((time.perf_counter() - started) * 1000),
        }


def print_summary(results: list[dict[str, Any]]) -> None:
    columns = (
        "case",
        "expected_relation",
        "actual_relation",
        "relation_verification",
        "metrics_verification",
        "status",
        "elapsed_ms",
    )
    widths = {name: len(name) for name in columns}
    for result in results:
        for name in columns:
            widths[name] = max(widths[name], len(str(result.get(name, "-"))))
    print("  ".join(name.ljust(widths[name]) for name in columns))
    print("  ".join("-" * widths[name] for name in columns))
    for result in results:
        print("  ".join(str(result.get(name, "-")).ljust(widths[name]) for name in columns))


def main() -> int:
    args = parse_args()
    if args.timeout_seconds <= 0:
        print("--timeout-seconds must be positive", file=sys.stderr)
        return 2

    try:
        fixtures = json.loads(args.cases.read_text(encoding="utf-8"))
        if not isinstance(fixtures, list) or not fixtures:
            raise ValueError("cases file must contain a non-empty JSON array")
        if len(fixtures) > 10:
            raise ValueError("cases file must not contain more than 10 evaluations")
        case_names = [require(fixture, "case") for fixture in fixtures]
        if len(case_names) != len(set(case_names)):
            raise ValueError("case names must be unique")
        for fixture in fixtures:
            expected_contract(fixture)
    except (OSError, KeyError, ValueError) as error:
        print(f"Cannot load cases: {error}", file=sys.stderr)
        return 2

    started_at = datetime.now(timezone.utc)
    results = [
        evaluate_case(
            args.base_url,
            fixture,
            args.timeout_seconds,
            args.expected_provenance_model,
        )
        for fixture in fixtures
    ]
    idempotency = (
        verify_idempotency(args.base_url, fixtures[0], results[0], args.timeout_seconds)
        if args.verify_idempotency
        else {"status": "NOT_REQUESTED"}
    )
    failures = sum(
        result.get("status") != "SUCCESS"
        or result.get("expectation_verification") != "PASS"
        or result.get("model_verification") != "PASS"
        or result.get("readback_verification") != "PASS"
        for result in results
    )
    if idempotency["status"] in {"FAIL", "ERROR", "SKIPPED"}:
        failures += 1

    report = {
        "run": {
            "started_at": started_at.isoformat(),
            "finished_at": datetime.now(timezone.utc).isoformat(),
            "base_url": args.base_url,
            "provider_label": args.provider_label,
            "model_label": args.model_label,
            "expected_provenance_model": args.expected_provenance_model,
            "cases_file": str(args.cases),
            "case_count": len(fixtures),
        },
        "summary": {
            "successes": sum(result.get("status") == "SUCCESS" for result in results),
            "errors": sum(result.get("status") == "ERROR" for result in results),
            "passed_expectations": sum(
                result.get("expectation_verification") == "PASS" for result in results
            ),
            "failed_checks": failures,
        },
        "idempotency": idempotency,
        "results": results,
    }
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(report, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    print_summary(results)
    print(f"\nReport: {args.output}")
    return 0 if failures == 0 else 1


if __name__ == "__main__":
    raise SystemExit(main())
