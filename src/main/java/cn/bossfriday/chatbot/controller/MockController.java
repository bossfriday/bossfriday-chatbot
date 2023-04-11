package cn.bossfriday.chatbot.controller;

import cn.bossfriday.chatbot.entity.completion.CompletionChoice;
import cn.bossfriday.chatbot.entity.completion.CompletionUsage;
import cn.bossfriday.chatbot.entity.im.ImPublishMessage;
import cn.bossfriday.chatbot.entity.im.ImServerApiResult;
import cn.bossfriday.chatbot.entity.request.OpenAiCompletionRequest;
import cn.bossfriday.chatbot.entity.response.OpenAiCompletionResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;

import static cn.bossfriday.chatbot.common.ChatRobotConstant.*;

/**
 * MockController
 *
 * @author chenx
 */
@RestController
@Slf4j
public class MockController {
    @PostMapping(value = URL_MOCK_OPENAI_COMPLETIONS, consumes = MediaType.APPLICATION_JSON_VALUE)
    public OpenAiCompletionResponse completions(@RequestHeader String authorization, @RequestBody @Validated OpenAiCompletionRequest request) {
        Long ts = System.currentTimeMillis();
        String answerTxt = "[Mocked ChatGPT Reply-" + ts + "]";
        log.info("===============ChatGPT===============");
        log.info("Q: " + request.getPrompt() + ", A: " + answerTxt);
        log.info("=====================================");
        return OpenAiCompletionResponse.builder()
                .id(String.valueOf(ts))
                .object("text_completion")
                .created(ts)
                .model("text-davinci-003")
                .choices(Arrays.asList(CompletionChoice.builder().text(answerTxt).index(0).build()))
                .usage(CompletionUsage.builder().promptTokens(100).completionTokens(100).totalTokens(200).build())
                .build();
    }

    @PostMapping(value = URL_MOCK_IM_PUBLISH, consumes = {MediaType.APPLICATION_FORM_URLENCODED_VALUE})
    public ImServerApiResult publishMessage(@Validated ImPublishMessage request) {
        log.info("==============ImServer===============");
        log.info(request.toString());
        log.info("=====================================");
        return ImServerApiResult.builder().code(IM_SERVER_API_RESULT_CODE_OK).build();
    }
}
