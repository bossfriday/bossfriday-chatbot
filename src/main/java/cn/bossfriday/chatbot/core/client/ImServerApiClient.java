package cn.bossfriday.chatbot.core.client;

import cn.bossfriday.chatbot.common.ChatRobotRuntimeException;
import cn.bossfriday.chatbot.entity.ChatRobotConfig;
import cn.bossfriday.chatbot.entity.im.ImPublishMessage;
import cn.bossfriday.chatbot.entity.im.ImServerApiResult;
import cn.bossfriday.chatbot.utils.ChatRobotUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;
import org.springframework.http.*;

import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static cn.bossfriday.chatbot.common.ChatRobotConstant.IM_SERVER_API_RESULT_CODE_OK;

/**
 * ImServerApiClient
 *
 * @author chenx
 */
@Slf4j
public class ImServerApiClient extends PoolingRestClient<Object> {

    private ChatRobotConfig config;

    public ImServerApiClient(String clientName, ChatRobotConfig config) {
        super(clientName, config.getRestClientMaxTotal(), config.getRestClientMaxPerRoute());

        this.config = config;
    }

    @Override
    public void send(Object message) {
        try {
            if (message instanceof ImPublishMessage) {
                this.publishMessage((ImPublishMessage) message);
            }
        } catch (Exception ex) {
            log.info("ImServerApiClient.send() error!", ex);
        }
    }

    /**
     * publishMessage
     *
     * @param message
     */
    private void publishMessage(ImPublishMessage message) throws NoSuchAlgorithmException {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        ChatRobotUtils.setImServerApiAuthHeaders(headers, this.config.getImAppKey(), this.config.getImAppSecret());
        HttpEntity<String> requestEntity = new HttpEntity<>(getBody(message), headers);
        ResponseEntity<ImServerApiResult> responseEntity = this.restTemplate.exchange(
                this.config.getImPublishMessageApiUrl(),
                HttpMethod.POST,
                requestEntity,
                ImServerApiResult.class
        );

        if (responseEntity.getStatusCode().isError()) {
            log.error("ImServerApiClient.publishMessage() failed, httpStatusCode:" + responseEntity.getStatusCode() + ", " + message.toString());
            return;
        }

        ImServerApiResult serverApiResult = responseEntity.getBody();
        if (Objects.isNull(serverApiResult)) {
            log.error("ImServerApiClient.publishMessage() failed, serverApiResult is null! " + message.toString());
            return;
        }

        if (serverApiResult.getCode() != IM_SERVER_API_RESULT_CODE_OK) {
            log.error("ImServerApiClient.publishMessage() failed, resultCode:" + serverApiResult.getCode() + ", " + message.toString());
        }
    }

    /**
     * getBody
     *
     * @param message
     * @return
     */
    private static String getBody(ImPublishMessage message) {
        if (Objects.isNull(message)) {
            throw new ChatRobotRuntimeException("message is null!");
        }

        if (ArrayUtils.isEmpty(message.getToUserId())) {
            throw new ChatRobotRuntimeException("toUserId is empty!");
        }

        List<BasicNameValuePair> parameters = new ArrayList<>();
        parameters.add(new BasicNameValuePair("fromUserId", message.getFromUserId()));
        parameters.add(new BasicNameValuePair("objectName", message.getObjectName()));
        parameters.add(new BasicNameValuePair("content", message.getContent()));
        for (String item : message.getToUserId()) {
            parameters.add(new BasicNameValuePair("toUserId", item));
        }

        return URLEncodedUtils.format(parameters, StandardCharsets.UTF_8);
    }
}
