package org.challenge.vulnseverityevaluator.domain.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.Set;

import static jakarta.persistence.EnumType.STRING;
import static jakarta.persistence.FetchType.EAGER;

/**
 * The technological context of the application a vulnerability may affect, as declared by the caller.
 * <p>
 * One type for the request body, the domain value and the persisted snapshot. It already carried persistence
 * mappings; adding its own constraints and creator keeps the alternative — a request shape repeating the same six
 * fields plus a mapper — from existing at all. A separate shape is worth writing when it has to differ, and this one
 * does not.
 * <p>
 * Two kinds of field, on purpose. The risk profile is a closed, ordered taxonomy, so it stays typed. Runtimes and
 * compensating controls are open lists that keep growing, so they are codes checked against the catalog in
 * {@link ContextAttribute} — adding one is a row, not a deploy. What the edge still enforces is shape: presence,
 * sizes and format, so no oversized or malformed payload reaches the catalog or the model.
 * <p>
 * The caller is the source of truth for this context, so a caller can influence the severity of its own application —
 * but never invisibly: the evaluation persists it as a snapshot, records that it was self declared, and the resulting
 * score is advisory.
 */
@Embeddable
public class ApplicationContext {

    private static final String CODE = "[A-Z][A-Z0-9_]{0,63}";

    @NotBlank
    @Size(max = 128)
    @Pattern(regexp = "[A-Za-z0-9][A-Za-z0-9._-]{0,127}", message = "must be a plain application name")
    private String name;

    @NotNull
    @Valid
    @Embedded
    private RiskProfile riskProfile;

    @NotNull
    @Size(max = 16)
    @ElementCollection(fetch = EAGER)
    @CollectionTable(name = "evaluation_runtime", joinColumns = @JoinColumn(name = "evaluation_id"))
    @Column(name = "runtime")
    private Set<@Pattern(regexp = CODE) String> runtime;

    @NotNull
    @Size(max = 16)
    @ElementCollection(fetch = EAGER)
    @CollectionTable(name = "evaluation_control", joinColumns = @JoinColumn(name = "evaluation_id"))
    @Column(name = "compensating_control")
    private Set<@Pattern(regexp = CODE) String> compensatingControls;

    protected ApplicationContext() {
    }

    @JsonCreator
    public static ApplicationContext createApplicationContext(
            @JsonProperty("name") String name,
            @JsonProperty("risk_profile") RiskProfile riskProfile,
            @JsonProperty("runtime") Set<String> runtime,
            @JsonProperty("compensating_controls") Set<String> compensatingControls) {
        ApplicationContext context = new ApplicationContext();
        context.name = name;
        context.riskProfile = riskProfile;
        context.runtime = runtime == null ? Set.of() : Set.copyOf(runtime);
        context.compensatingControls = compensatingControls == null ? Set.of() : Set.copyOf(compensatingControls);
        return context;
    }

    public String name() {
        return name;
    }

    public RiskProfile riskProfile() {
        return riskProfile;
    }

    public Set<String> runtime() {
        return Set.copyOf(runtime);
    }

    public Set<String> compensatingControls() {
        return Set.copyOf(compensatingControls);
    }


    /**
     * Why the application matters: where it is reachable from, what data it handles, and how critical it is to the
     * business. These three are only meaningful together, and together they are what moves a severity away from its
     * baseline — which is why they are one type and not three loose fields.
     */
    @Embeddable
    public static class RiskProfile {

        @NotNull
        @Enumerated(STRING)
        private Exposure exposure;

        @NotNull
        @Enumerated(STRING)
        private DataClassification dataClassification;

        @NotNull
        @Enumerated(STRING)
        private BusinessCriticality businessCriticality;

        protected RiskProfile() {
        }

        @JsonCreator
        public static RiskProfile createRiskProfile(
                @JsonProperty("exposure") Exposure exposure,
                @JsonProperty("data_classification") DataClassification dataClassification,
                @JsonProperty("business_criticality") BusinessCriticality businessCriticality) {
            RiskProfile profile = new RiskProfile();
            profile.exposure = exposure;
            profile.dataClassification = dataClassification;
            profile.businessCriticality = businessCriticality;
            return profile;
        }

        public Exposure exposure() {
            return exposure;
        }

        public DataClassification dataClassification() {
            return dataClassification;
        }

        public BusinessCriticality businessCriticality() {
            return businessCriticality;
        }

    }
}
