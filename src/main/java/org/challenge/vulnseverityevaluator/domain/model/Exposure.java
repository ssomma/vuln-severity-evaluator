package org.challenge.vulnseverityevaluator.domain.model;

/**
 * Where the application can be reached from: the single strongest contextual signal, since the same vulnerability in
 * an isolated component and in an internet facing one are not the same risk.
 * <p>
 * A closed, ordered taxonomy, so it stays a type. What each value <em>means</em> is catalog data in
 * {@link ContextAttribute}, which is what the language model reads.
 */
public enum Exposure {

    INTERNET_FACING, INTERNAL_NETWORK, ISOLATED
}
