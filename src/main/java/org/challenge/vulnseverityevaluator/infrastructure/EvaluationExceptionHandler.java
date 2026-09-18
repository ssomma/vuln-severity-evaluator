package org.challenge.vulnseverityevaluator.infrastructure;

import org.slf4j.Logger;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.net.URI;

import static org.challenge.vulnseverityevaluator.infrastructure.metric.ApplicationMetricCollector.EventMetrics.collectEventEvaluationFailed;
import static org.challenge.vulnseverityevaluator.infrastructure.metric.ApplicationMetricCollector.EventMetrics.collectEventModelAnswerRejected;
import static org.slf4j.LoggerFactory.getLogger;
import static org.springframework.http.HttpStatus.BAD_GATEWAY;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

/**
 * Turns failures into RFC 7807 problem responses.
 * <p>
 * Explicit failure types preserve the source of a failure through the service boundary:
 * <ul>
 * <li>{@link InvalidEvaluationRequestException} — the caller sent an invalid vector or context value.</li>
 * <li>{@link ModelAnswerUnusableException} — the model produced an unusable answer.</li>
 * <li>{@link ModelProviderException} — the provider call failed.</li>
 * </ul>
 * Extending {@link ResponseEntityExceptionHandler} is not cosmetic: it keeps Spring's own handling of malformed
 * bodies, unknown enum values and type mismatches, which already carry the right status. A blanket handler without it
 * would swallow those and answer 500 to what is plainly a 400.
 */
@RestControllerAdvice
public class EvaluationExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger logger = getLogger(EvaluationExceptionHandler.class);

    private static final URI INVALID_REQUEST = URI.create("urn:problem:invalid-evaluation-request");
    private static final URI MODEL_UNUSABLE = URI.create("urn:problem:model-answer-unusable");
    private static final URI MODEL_PROVIDER_FAILED = URI.create("urn:problem:model-provider-failed");
    private static final URI EVALUATION_FAILED = URI.create("urn:problem:evaluation-failed");

    private static final String INVALID_TITLE = "The evaluation request is not valid";
    private static final String UNUSABLE_TITLE = "The severity assessment could not be completed";
    private static final String UNUSABLE_DETAIL =
            "The reasoning model did not produce a usable answer. No severity was produced; retrying is safe.";
    private static final String PROVIDER_DETAIL = "The reasoning model is unavailable. Retrying is safe.";
    private static final String FAILED_DETAIL = "The severity assessment could not be completed.";

    @ExceptionHandler(InvalidEvaluationRequestException.class)
    public ProblemDetail handleInvalidRequest(InvalidEvaluationRequestException exception) {
        logger.info("evaluation request rejected: {}", exception.getMessage());
        return problem(BAD_REQUEST.value(), INVALID_REQUEST, INVALID_TITLE, exception.getMessage());
    }

    /**
     * An unusable model answer is counted here, not just reported. A rising rejection rate is the earliest visible
     * symptom of model drift or of a prompt that no longer matches the vocabulary, and without the counter the
     * failure is silent. Only the exception class becomes a dimension: the message usually carries the raw model
     * answer.
     */
    @ExceptionHandler(ModelAnswerUnusableException.class)
    public ProblemDetail handleUnusableAnswer(ModelAnswerUnusableException exception) {
        collectEventModelAnswerRejected(exception);
        logger.warn("model answer rejected: {}", exception.getMessage());
        return problem(BAD_GATEWAY.value(), MODEL_UNUSABLE, UNUSABLE_TITLE, UNUSABLE_DETAIL);
    }

    @ExceptionHandler(ModelProviderException.class)
    public ProblemDetail handleProviderFailure(ModelProviderException exception) {
        collectEventEvaluationFailed(exception);
        logger.error("reasoning model provider failed", exception);
        return problem(BAD_GATEWAY.value(), MODEL_PROVIDER_FAILED, UNUSABLE_TITLE, PROVIDER_DETAIL);
    }

    /** An unexpected service or persistence failure is never attributed to the model. */
    @ExceptionHandler(RuntimeException.class)
    public ProblemDetail handleUnexpectedFailure(RuntimeException exception) {
        collectEventEvaluationFailed(exception);
        logger.error("severity evaluation failed", exception);
        return problem(INTERNAL_SERVER_ERROR.value(), EVALUATION_FAILED, UNUSABLE_TITLE, FAILED_DETAIL);
    }

    private static ProblemDetail problem(int status, URI type, String title, String detail) {
        ProblemDetail problem = ProblemDetail.forStatus(status);
        problem.setType(type);
        problem.setTitle(title);
        problem.setDetail(detail);
        return problem;
    }
}
