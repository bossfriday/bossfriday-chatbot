package cn.bossfriday.chatbot.core.client;

import cn.bossfriday.chatbot.common.ChatbotException;
import cn.bossfriday.chatbot.core.message.RoutableMessage;
import cn.bossfriday.chatbot.entity.result.Result;
import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;

import java.util.Objects;

import static cn.bossfriday.chatbot.common.ChatbotConstant.URL_RECEIVE_ROUTED_MSG;

/**
 * RoutableMessageClient
 *
 * @author chenx
 */
@Slf4j
public class RoutableMessageClient extends PoolingRestClient<RoutableMessage<?>> {

    public RoutableMessageClient(String name, int restClientMaxTotal, int restClientMaxPerRoute) {
        super(name, restClientMaxTotal, restClientMaxPerRoute);
    }

    @Override
    public void send(RoutableMessage<?> message) {
        try {
            String url = message.getTargetServiceInstance().getUri().toString() + URL_RECEIVE_ROUTED_MSG;
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> requestEntity = new HttpEntity<>(JSON.toJSONString(message.getPayload()), headers);
            ResponseEntity<Result<Void>> responseEntity = this.restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    requestEntity,
                    new ParameterizedTypeReference<Result<Void>>() {
                        // Result<Void> not suggest to replace with <>, otherwise it will maybe build error!
                    }
            );

            if (responseEntity.getStatusCode().isError()) {
                throw new ChatbotException("RoutableMessageTransport.send() failed, " + responseEntity.getStatusCode());
            }

            Result<Void> result = responseEntity.getBody();
            if (Objects.isNull(result)) {
                throw new ChatbotException("RoutableMessageTransport.send() failed, responseEntity.getBody() is null!");
            }

            if (!result.isSuccess()) {
                log.error("RoutableMessageTransport.send() error! " + result.toString());
            }
        } catch (Exception ex) {
            log.info("RoutableMessageTransport.send() error!", ex);
        }
    }
}
