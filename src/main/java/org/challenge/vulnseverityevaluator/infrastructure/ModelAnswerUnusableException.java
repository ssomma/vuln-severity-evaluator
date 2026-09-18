package org.challenge.vulnseverityevaluator.infrastructure;

/** The model returned an answer that cannot safely be scored or stored. */
public class ModelAnswerUnusableException extends RuntimeException {

    public ModelAnswerUnusableException(String message) {
        super(message);
    }

    public ModelAnswerUnusableException(String message, Throwable cause) {
        super(message, cause);
    }
}
