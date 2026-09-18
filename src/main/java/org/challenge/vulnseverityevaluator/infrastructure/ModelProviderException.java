package org.challenge.vulnseverityevaluator.infrastructure;

/** The model provider call failed before a usable answer was returned. */
public class ModelProviderException extends RuntimeException {

    public ModelProviderException(String message, Throwable cause) {
        super(message, cause);
    }
}
