package cn.bossfriday.chatbot.core.client;

import cn.bossfriday.chatbot.core.ChatContextCorrelator;
import cn.bossfriday.chatbot.core.MessageDispatcher;
import cn.bossfriday.chatbot.core.message.RoutableImMessage;
import cn.bossfriday.chatbot.entity.ChatRobotConfig;
import cn.bossfriday.chatbot.entity.im.ImMessage;
import cn.bossfriday.chatbot.entity.im.ImPublishMessage;
import cn.bossfriday.chatbot.entity.im.rcmsg.RcTxtMsg;
import cn.bossfriday.chatbot.entity.response.OpenAiCompletionResponse;
import cn.bossfriday.chatbot.utils.ChatRobotUtils;
import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.http.*;

import java.util.Objects;

import static cn.bossfriday.chatbot.common.ChatRobotConstant.IM_MESSAGE_TYPE_TXT;
import static cn.bossfriday.chatbot.common.enums.ChatbotResultCode.*;

/**
 * ChatGptClient
 *
 * @author chenx
 */
@Slf4j
public class ChatGptClient extends PoolingRestClient<RoutableImMessage> {

    private ChatRobotConfig config;
    private ChatContextCorrelator correlator;
    private ImServerApiClient imServerApiClient;
    private MessageDispatcher messageDispatcher;

    public ChatGptClient(String clientName, ChatRobotConfig config, MessageDispatcher messageDispatcher) {
        super(clientName, config.getRestClientMaxTotal(), config.getRestClientMaxPerRoute());

        this.config = config;
        this.correlator = new ChatContextCorrelator(config);
        this.imServerApiClient = new ImServerApiClient("imServerApiClient", config);
        this.messageDispatcher = messageDispatcher;
    }

    @Override
    public void send(RoutableImMessage routableImMessage) {
        String callbackContent = "";
        try {
            ImMessage imMessage = routableImMessage.getPayload();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", ChatRobotUtils.getOpenAiApiAuth(this.config.getOpenAiApiAuth()));
            String requestBody = JSON.toJSONString(this.correlator.getOpenAiCompletionRequest(imMessage));
            HttpEntity<String> requestEntity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<OpenAiCompletionResponse> responseEntity = this.restTemplate.exchange(
                    this.config.getOpenAiAskApiUrl(),
                    HttpMethod.POST,
                    requestEntity,
                    OpenAiCompletionResponse.class
            );

            if (responseEntity.getStatusCode().isError()) {
                log.error("ChatGptClient.send() failed, httpStatusCode:" + responseEntity.getStatusCode() + ", " + imMessage.toString());
                callbackContent = getErrorResult(CHAT_GPT_API_HTTP_STATUS_CODE_ERROR.getCode());
                return;
            }

            OpenAiCompletionResponse responseBody = responseEntity.getBody();
            if (Objects.isNull(responseBody)) {
                log.error("ChatGptClient.send() failed, responseBody is null!");
                callbackContent = getErrorResult(CHAT_GPT_API_RESPONSE_BODY_IS_NULL.getCode());
                return;
            }

            if (CollectionUtils.isEmpty(responseBody.getChoices())) {
                log.error("ChatGptClient.send() failed, response choices is empty!");
                callbackContent = getErrorResult(CHAT_GPT_API_RESPONSE_CHOICES_IS_EMPTY.getCode());
                return;
            }

            callbackContent = responseBody.getChoices().get(0).getText();
            this.correlator.setContext(this.correlator.getKey(imMessage), callbackContent);
        } catch (Exception ex) {
            log.error("ChatGptClient.send() error!", ex);
        } finally {
            this.callback(routableImMessage, callbackContent);
        }
    }

    /**
     * callback
     *
     * @param routableImMessage
     * @param callbackContent
     */
    private void callback(RoutableImMessage routableImMessage, String callbackContent) {
        try {
            this.messageDispatcher.getChatGptInvokerExecutor(routableImMessage.getHashCode()).execute(() -> {
                ImMessage imMessage = routableImMessage.getPayload();
                ImPublishMessage imPublishMessage = ImPublishMessage.builder()
                        .fromUserId(imMessage.getToUserId())
                        .toUserId(new String[]{imMessage.getFromUserId()})
                        .objectName(IM_MESSAGE_TYPE_TXT)
                        .content(JSON.toJSONString(RcTxtMsg.builder().content(callbackContent).build()))
                        .build();

                this.imServerApiClient.send(imPublishMessage);
            });
        } catch (Exception ex) {
            log.error("ChatGptClient.callback() error!", ex);
        }
    }

    /**
     * getErrorResult
     *
     * @param code
     * @return
     */
    private static String getErrorResult(int code) {
        return String.format("System Internal Error (%s)!", code);
    }
}
