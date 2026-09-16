package org.challenge.vulnseverityevaluator.presentation.controller.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.challenge.vulnseverityevaluator.domain.model.ApplicationContext;
import org.challenge.vulnseverityevaluator.domain.model.Vulnerability;

/**
 * The request body: the two blocks the evaluation needs.
 * <p>
 * It carries the domain types rather than mirroring them. Both already describe their own shape and constraints, so a
 * parallel set of request records would have repeated nine fields and three mappers to say exactly the same thing.
 * What this type adds is the only thing that is genuinely presentation: the grouping.
 * <p>
 * The two blocks stay separate because they have different provenance and different futures — the vulnerability could
 * come from an advisory feed later, the context from an inventory — so one can evolve without the other.
 */
public record EvaluateSeverityRequest(@NotNull @Valid Vulnerability vulnerability,
                                      @NotNull @Valid ApplicationContext application) {
}
