package cn.bossfriday.chatbot.core;

import cn.bossfriday.chatbot.common.ChatRobotRuntimeException;
import cn.bossfriday.chatbot.common.ConcurrentCircularList;
import cn.bossfriday.chatbot.entity.ChatRobotConfig;
import cn.bossfriday.chatbot.entity.im.ImMessage;
import cn.bossfriday.chatbot.entity.request.OpenAiCompletionRequest;
import cn.bossfriday.chatbot.utils.ChatRobotUtils;
import cn.bossfriday.chatbot.utils.CircularListCodecUtils;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static cn.bossfriday.chatbot.common.ChatRobotConstant.SERVICE_AI_MODEL_TXT;
import static cn.bossfriday.chatbot.common.ChatRobotConstant.SERVICE_CHOICE_COUNT;

/**
 * ChatContextCorrelator
 *
 * @author chenx
 */
@Slf4j
public class ChatContextCorrelator {

    private Cache<String, byte[]> contextCache = null;
    private ChatRobotConfig config;

    public ChatContextCorrelator(ChatRobotConfig config) {
        this.config = config;
        this.contextCache = Caffeine.newBuilder()
                .initialCapacity(config.getContextCacheInitialCapacity())
                .maximumSize(config.getContextCacheMaxCapacity())
                .expireAfterAccess(config.getContextCacheExpireSeconds(), TimeUnit.SECONDS)
                .build();
    }

    /**
     * getOpenAiCompletionRequest
     *
     * @param message
     * @return
     */
    public OpenAiCompletionRequest getOpenAiCompletionRequest(ImMessage message) throws IOException {
        String key = this.getKey(message);
        String value = ChatRobotUtils.getImMessageContent(message);
        ConcurrentCircularList<String> chatContextList = this.setContext(key, value);

        return this.buildOpenAiCompletionRequest(chatContextList);
    }

    /**
     * @param message
     * @return
     */
    public String getKey(ImMessage message) {
        if (Objects.isNull(message)) {
            throw new ChatRobotRuntimeException("The input ImMessage is null!");
        }

        if (StringUtils.isEmpty(message.getBusChannel()) || StringUtils.isEmpty(message.getFromUserId())) {
            throw new ChatRobotRuntimeException("ImMessage.busChannel or fromUserId is empty!");
        }

        return message.getBusChannel() + "-" + message.getFromUserId();
    }

    /**
     * setContext
     *
     * @param key
     * @param context
     * @return
     */
    public ConcurrentCircularList<String> setContext(String key, String context) throws IOException {
        byte[] data = this.contextCache.getIfPresent(key);
        ConcurrentCircularList<String> circularList = ArrayUtils.isEmpty(data)
                ? new ConcurrentCircularList<>(this.config.getContextCacheRingCapacity())
                : CircularListCodecUtils.decodeStringList(data);
        circularList.add(context);
        this.contextCache.put(key, CircularListCodecUtils.encodeStringList(circularList, this.config.getContextCacheRingCapacity()));

        return circularList;
    }

    /**
     * buildOpenAiCompletionRequest
     *
     * @param chatContextList
     * @return
     */
    private OpenAiCompletionRequest buildOpenAiCompletionRequest(ConcurrentCircularList<String> chatContextList) {
        if (chatContextList == null || chatContextList.isEmpty()) {
            throw new ChatRobotRuntimeException("chatContextList is null or empty!");
        }

        StringBuilder sb = new StringBuilder();
        for (String content : chatContextList) {
            sb.append(content + " ");
        }

        return OpenAiCompletionRequest.builder()
                .prompt(sb.toString())
                .model(SERVICE_AI_MODEL_TXT)
                .temperature(this.config.getOpenAiTemperature())
                .maxTokens(this.config.getOpenAiMaxTokens())
                .n(SERVICE_CHOICE_COUNT)
                .build();
    }
}
