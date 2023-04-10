package cn.bossfriday.chatbot.entity.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

/**
 * Completions
 *
 * @author chenx
 */
@ToString
@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class OpenAiCompletionRequest {

    /**
     * prompt
     */
    @JsonProperty("prompt")
    private String prompt;

    /**
     * davinci: This is currently the most powerful language model, suitable for generating various types of text, including long articles, text messages, emails, chat logs, code, and more.
     * curie: This is a smaller but faster model, suitable for processing medium-length text, such as text messages, emails, and chat logs.
     * babbage: This is a smaller model suitable for generating short answers and phrases.
     * ada: This is a very small model suitable for generating phrases and short sentences.
     */
    @JsonProperty("model")
    private String model;

    /**
     * suggest to 0.5 - 1.0
     */
    @JsonProperty("temperature")
    private Double temperature;

    /**
     * OpenAI default value is 2048
     */
    @JsonProperty("max_tokens")
    private Integer maxTokens;

    /**
     * the count of reply
     */
    @JsonProperty("n")
    private Integer n;
}
