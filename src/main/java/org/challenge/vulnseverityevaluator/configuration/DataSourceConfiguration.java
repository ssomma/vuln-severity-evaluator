package org.challenge.vulnseverityevaluator.configuration;

import jakarta.annotation.PostConstruct;
import org.challenge.vulnseverityevaluator.datasource.llm.SeverityReasoningModel;
import org.challenge.vulnseverityevaluator.datasource.llm.SpringAiSeverityReasoningModel;
import org.challenge.vulnseverityevaluator.datasource.llm.SpringAiSeverityReasoningModel.Prompts;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Wires the language model datasource for the profiles that talk to a real provider.
 * <p>
 * The API key is read from the environment and validated at startup: a missing credential fails the boot instead of
 * failing the first request, and it is never held in configuration committed to the repository.
 */
@Configuration
@Profile({"production", "test"})
public class DataSourceConfiguration {

    private static final String MISSING_KEY = "OPENAI_API_KEY must be set for this profile";

    @Value("${spring.ai.openai.api-key:}")
    private String apiKey;

    @Value("${spring.ai.openai.chat.model}")
    private String model;

    @Value("classpath:templates/prompts/contextual-severity/system-v1.st")
    private Resource contextualSystem;

    @Value("classpath:templates/prompts/contextual-severity/user-v1.st")
    private Resource contextualUser;

    @Value("classpath:templates/prompts/baseline-vector/system-v1.st")
    private Resource baselineSystem;

    @Value("classpath:templates/prompts/baseline-vector/user-v1.st")
    private Resource baselineUser;

    @PostConstruct
    public void validateCredentials() {
        Assert.state(StringUtils.hasText(apiKey), MISSING_KEY);
    }

    @Bean
    public SeverityReasoningModel severityReasoningModel(ChatClient.Builder builder) {
        Prompts prompts = new Prompts(contextualSystem, contextualUser, baselineSystem, baselineUser);
        return new SpringAiSeverityReasoningModel(builder.build(), model, prompts);
    }
}
