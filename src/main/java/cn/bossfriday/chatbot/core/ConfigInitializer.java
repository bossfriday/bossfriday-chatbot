package cn.bossfriday.chatbot.core;

import cn.bossfriday.chatbot.entity.ChatbotConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * ConfigInitializer
 *
 * @author chenx
 */
@Component
public class ConfigInitializer {

    @Value("${server.port}")
    private int serverPort;

    @Value("${spring.application.name}")
    private String serverName;

    @Value("${bossfriday.chatbot.service.mailBoxQueueSize}")
    private int mailBoxQueueSize;

    @Value("${bossfriday.chatbot.service.localRouteNoNetwork}")
    private boolean localRouteNoNetwork;

    @Value("${bossfriday.chatbot.service.chatGPTDispatcherThreadSize}")
    private int chatGptDispatcherThreadSize;

    @Value("${bossfriday.chatbot.service.imServerDispatcherThreadSize}")
    private int imServerDispatcherThreadSize;

    @Value("${bossfriday.chatbot.service.restClientMaxTotal}")
    private int restClientMaxTotal;

    @Value("${bossfriday.chatbot.service.restClientMaxPerRoute}")
    private int restClientMaxPerRoute;

    @Value("${bossfriday.chatbot.openAi.authorization}")
    private String openAiAuth;

    @Value("${bossfriday.chatbot.openAi.askApiUrl}")
    private String openAiAskApiUrl;

    @Value("${bossfriday.chatbot.openAi.maxTokens}")
    private int openAiMaxTokens;

    @Value("${bossfriday.chatbot.openAi.temperature}")
    private Double openAiTemperature;

    @Value("${bossfriday.chatbot.service.contextCacheInitialCapacity}")
    private int contextCacheInitialCapacity;

    @Value("${bossfriday.chatbot.service.contextCacheMaxCapacity}")
    private int contextCacheMaxCapacity;

    @Value("${bossfriday.chatbot.service.contextCacheExpireSeconds}")
    private int contextCacheExpireSeconds;

    @Value("${bossfriday.chatbot.service.contextCacheRingCapacity}")
    private int contextCacheRingCapacity;

    @Value("${bossfriday.chatbot.im.appKey}")
    private String imAppKey;

    @Value("${bossfriday.chatbot.im.appSecret}")
    private String imAppSecret;

    @Value("${bossfriday.chatbot.im.publishMessageApiUrl}")
    private String imPublishMessageApiUrl;

    /**
     * getChatRobotConfig
     *
     * @return
     */
    public ChatbotConfig getChatRobotConfig() {
        return ChatbotConfig.builder()
                .serverPort(this.serverPort)
                .serverName(this.serverName)
                .mailBoxQueueSize(this.mailBoxQueueSize)
                .localRouteNoNetwork(this.localRouteNoNetwork)
                .chatGptDispatcherThreadSize(this.chatGptDispatcherThreadSize)
                .imServerDispatcherThreadSize(this.imServerDispatcherThreadSize)
                .restClientMaxTotal(this.restClientMaxTotal)
                .restClientMaxPerRoute(this.restClientMaxPerRoute)
                .openAiApiAuth(this.openAiAuth)
                .openAiAskApiUrl(this.openAiAskApiUrl)
                .openAiMaxTokens(this.openAiMaxTokens)
                .openAiTemperature(this.openAiTemperature)
                .contextCacheInitialCapacity(this.contextCacheInitialCapacity)
                .contextCacheMaxCapacity(this.contextCacheMaxCapacity)
                .contextCacheExpireSeconds(this.contextCacheExpireSeconds)
                .contextCacheRingCapacity(this.contextCacheRingCapacity)
                .imAppKey(this.imAppKey)
                .imAppSecret(this.imAppSecret)
                .imPublishMessageApiUrl(this.imPublishMessageApiUrl)
                .build();
    }
}
