package cn.bossfriday.chatbot.entity;

import lombok.*;

/**
 * ChatRobotServiceConfig
 *
 * @author chenx
 */
@ToString
@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChatRobotConfig {

    private int serverPort;

    private String serverName;

    private int mailBoxQueueSize;

    private boolean localRouteNoNetwork;

    private int chatGptDispatcherThreadSize;

    private int imServerDispatcherThreadSize;

    private int restClientMaxTotal;

    private int restClientMaxPerRoute;

    private String openAiApiAuth;

    private String openAiAskApiUrl;

    private int openAiMaxTokens;

    private Double openAiTemperature;

    private int contextCacheInitialCapacity;

    private int contextCacheMaxCapacity;

    private int contextCacheExpireSeconds;

    private int contextCacheRingCapacity;

    private String imAppKey;

    private String imAppSecret;

    private String imPublishMessageApiUrl;
}
