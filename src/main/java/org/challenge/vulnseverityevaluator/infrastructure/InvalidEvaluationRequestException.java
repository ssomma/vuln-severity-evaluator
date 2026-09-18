package org.challenge.vulnseverityevaluator.infrastructure;

/** A caller supplied a value that the evaluation cannot accept. */
public class InvalidEvaluationRequestException extends RuntimeException {

    public InvalidEvaluationRequestException(String message) {
        super(message);
    }

    public InvalidEvaluationRequestException(String message, Throwable cause) {
        super(message, cause);
    }
}
