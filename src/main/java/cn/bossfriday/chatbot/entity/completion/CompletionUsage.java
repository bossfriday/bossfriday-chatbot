package cn.bossfriday.chatbot.entity.completion;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

/**
 * CompletionUsage
 *
 * @author chenx
 */
@ToString
@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CompletionUsage {

    @JsonProperty("text_characters")
    private int textCharacters;

    @JsonProperty("tokens")
    private int tokens;

    @JsonProperty("model_load_seconds")
    private double modelLoadSeconds;

    @JsonProperty("model_load_timestamp")
    private long modelLoadTimestamp;

    @JsonProperty("inference_seconds")
    private double inferenceSeconds;

    @JsonProperty("output_characters")
    private int outputCharacters;

    @JsonProperty("output_tokens")
    private int outputTokens;

    @JsonProperty("prompt_tokens")
    private int promptTokens;

    @JsonProperty("completion_tokens")
    private int completionTokens;

    @JsonProperty("total_tokens")
    private int totalTokens;
}
