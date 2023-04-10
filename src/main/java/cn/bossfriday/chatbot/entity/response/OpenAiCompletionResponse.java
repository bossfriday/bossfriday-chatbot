package cn.bossfriday.chatbot.entity.response;

import cn.bossfriday.chatbot.entity.completion.CompletionChoice;
import cn.bossfriday.chatbot.entity.completion.CompletionUsage;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.List;

/**
 * CompletionsOkResponse
 *
 * @author chenx
 */
@ToString
@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class OpenAiCompletionResponse {

    @JsonProperty("id")
    private String id;

    @JsonProperty("object")
    private String object;

    @JsonProperty("created")
    private long created;

    @JsonProperty("model")
    private String model;

    @JsonProperty("choices")
    private List<CompletionChoice> choices;

    @JsonProperty("usage")
    private CompletionUsage usage;

}
