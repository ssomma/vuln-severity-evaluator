package org.challenge.vulnseverityevaluator.datasource.llm;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import okhttp3.mockwebserver.SocketPolicy;
import org.challenge.vulnseverityevaluator.domain.model.ContextAttribute;
import org.challenge.vulnseverityevaluator.domain.model.ModelSeverityProposal;
import org.challenge.vulnseverityevaluator.domain.model.SchemeMetric;
import org.challenge.vulnseverityevaluator.domain.model.Vulnerability;
import org.challenge.vulnseverityevaluator.infrastructure.ModelAnswerUnusableException;
import org.challenge.vulnseverityevaluator.infrastructure.ModelProviderException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.core.io.ByteArrayResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LLMSeverityReasoningModelTest {

    private static final String VECTOR = "CVSS:3.1/AV:N/AC:L/PR:N/UI:N/S:U/C:H/I:H/A:H";

    private MockWebServer provider;

    @BeforeEach
    void startProvider() throws IOException {
        provider = new MockWebServer();
        provider.start();
    }

    @AfterEach
    void stopProvider() throws IOException {
        provider.shutdown();
    }

    @Test
    void givenValidStructuredAnswerWhenProposingThenReturnMappedResponseAndSerializedPrompt()
            throws InterruptedException {
        provider.enqueue(completion("{\"choices\":[{\"metric\":\"CR\",\"value\":\"H\","
                + "\"rationale\":\"The application processes regulated data.\"}],"
                + "\"summary\":\"Confidentiality is material.\"}"));
        LLMSeverityReasoningModel model = model(Duration.ofSeconds(2));
        SchemeMetric metric = metric();
        ContextAttribute context = context();
        Vulnerability vulnerability = new Vulnerability("CVE-2026-0042", "Description with {template} syntax", VECTOR);

        ModelSeverityProposal proposal = model.propose(vulnerability, List.of(context), List.of(metric), "CVSS:3.1");

        assertThat(proposal.summary()).isEqualTo("Confidentiality is material.");
        assertThat(proposal.choices()).singleElement().satisfies(choice -> {
            assertThat(choice.metric()).isEqualTo("CR");
            assertThat(choice.value()).isEqualTo("H");
            assertThat(choice.rationale()).isEqualTo("The application processes regulated data.");
        });
        RecordedRequest request = provider.takeRequest();
        assertThat(request.getPath()).isEqualTo("/chat/completions");
        assertThat(request.getHeader("Authorization")).isEqualTo("Bearer test-key");
        assertThat(request.getBody().readUtf8())
                .contains("test-model")
                .contains("Description with {template} syntax")
                .contains("CR (Confidentiality Requirement)")
                .contains("DATA_CLASSIFICATION PII")
                .contains("JSON Schema instance");
    }

    @Test
    void givenMalformedStructuredAnswerWhenProposingThenThrowUnusableAnswer() {
        provider.enqueue(completion("not-json"));

        assertThatThrownBy(() -> model(Duration.ofSeconds(2)).propose(
                new Vulnerability("CVE-2026-0042", "description", VECTOR),
                List.of(context()), List.of(metric()), "CVSS:3.1"))
                .isInstanceOf(ModelAnswerUnusableException.class)
                .hasMessage("the model returned an invalid proposal")
                .hasCauseInstanceOf(RuntimeException.class);
    }

    @Test
    void givenHttpErrorWhenDerivingBaselineThenThrowProviderFailure() {
        provider.enqueue(new MockResponse().setResponseCode(429)
                .setHeader("Content-Type", "application/json")
                .setBody("{\"error\":{\"message\":\"rate limited\",\"type\":\"rate_limit_error\"}}"));

        assertThatThrownBy(() -> model(Duration.ofSeconds(2)).deriveBaselineVector(
                new Vulnerability("CVE-2026-0042", "description", null), "CVSS:3.1"))
                .isInstanceOf(ModelProviderException.class)
                .hasMessage("the model provider call failed")
                .hasCauseInstanceOf(RuntimeException.class);
    }

    @Test
    void givenProviderTimeoutWhenDerivingBaselineThenThrowProviderFailure() {
        provider.enqueue(new MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE));

        assertThatThrownBy(() -> model(Duration.ofMillis(100)).deriveBaselineVector(
                new Vulnerability("CVE-2026-0042", "description", null), "CVSS:3.1"))
                .isInstanceOf(ModelProviderException.class)
                .hasMessage("the model provider call failed")
                .hasCauseInstanceOf(RuntimeException.class);
    }

    private LLMSeverityReasoningModel model(Duration timeout) {
        OpenAiChatOptions options = OpenAiChatOptions.builder()
                .baseUrl(provider.url("/").toString())
                .apiKey("test-key")
                .model("test-model")
                .timeout(timeout)
                .maxRetries(0)
                .build();
        OpenAiChatModel chatModel = OpenAiChatModel.builder().options(options).build();
        ChatClient client = ChatClient.builder(chatModel).build();
        LLMSeverityReasoningModel.Prompts prompts = new LLMSeverityReasoningModel.Prompts(
                prompt("{schemeId} {vocabulary}"),
                prompt("{identifier} {description} {context}"),
                prompt("{schemeId}"),
                prompt("{identifier} {description}"));
        return new LLMSeverityReasoningModel(client, "test-provider/test-model", prompts);
    }

    private static ByteArrayResource prompt(String template) {
        return new ByteArrayResource(template.getBytes(StandardCharsets.UTF_8));
    }

    private static MockResponse completion(String content) {
        String escaped = content.replace("\\", "\\\\").replace("\"", "\\\"");
        return new MockResponse().setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("""
                        {"id":"chatcmpl-test","object":"chat.completion","created":1,"model":"test-model",
                         "choices":[{"index":0,"message":{"role":"assistant","content":"%s"},"finish_reason":"stop"}],
                         "usage":{"prompt_tokens":1,"completion_tokens":1,"total_tokens":2}}
                        """.formatted(escaped));
    }

    private static SchemeMetric metric() {
        SchemeMetric metric = mock(SchemeMetric.class);
        when(metric.code()).thenReturn("CR");
        when(metric.label()).thenReturn("Confidentiality Requirement");
        when(metric.guidance()).thenReturn("Importance of confidentiality for the application.");
        when(metric.admitted()).thenReturn(List.of("H", "M", "L", "X"));
        return metric;
    }

    private static ContextAttribute context() {
        ContextAttribute context = mock(ContextAttribute.class);
        when(context.kind()).thenReturn(ContextAttribute.Kind.DATA_CLASSIFICATION);
        when(context.code()).thenReturn("PII");
        when(context.meaning()).thenReturn("The application processes personally identifiable information.");
        return context;
    }
}
