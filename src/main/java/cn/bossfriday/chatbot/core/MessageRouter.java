package cn.bossfriday.chatbot.core;

import cn.bossfriday.chatbot.common.ChatbotException;
import cn.bossfriday.chatbot.core.mailbox.MessageInBox;
import cn.bossfriday.chatbot.core.mailbox.MessageSendBox;
import cn.bossfriday.chatbot.core.message.RoutableImMessage;
import cn.bossfriday.chatbot.core.message.RoutableMessage;
import cn.bossfriday.chatbot.entity.ChatbotConfig;
import cn.bossfriday.chatbot.entity.im.ImMessage;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.List;

import static cn.bossfriday.chatbot.common.enums.ChatbotResultCode.NO_AVAILABLE_SERVER;

/**
 * MessageRouter
 *
 * @author chenx
 */
@Slf4j
@Component
public class MessageRouter {

    @Autowired
    private ConfigInitializer configInitializer;

    @Autowired
    private DiscoveryClient serviceDiscoveryClient;

    @Getter
    private ChatbotConfig config;

    private MessageSendBox sendBox;
    private MessageInBox inBox;

    /**
     * init
     */
    @PostConstruct
    public void init() {
        try {
            this.config = this.configInitializer.getChatRobotConfig();
            InetAddress inetAddress = InetAddress.getLocalHost();
            InetSocketAddress selfAddress = new InetSocketAddress(inetAddress.getHostAddress(), this.config.getServerPort());
            MessageDispatcher messageDispatcher = new MessageDispatcher(this.config);
            this.inBox = new MessageInBox(this.config, messageDispatcher);
            this.sendBox = new MessageSendBox(this.config, this.inBox, selfAddress, messageDispatcher);

            log.info("MessageRouter init done, selfAddress:" + selfAddress + ", " + this.config.toString());
        } catch (Exception ex) {
            log.error("MessageRouter.init() error!", ex);

            // force service start failed
            throw new ChatbotException("MessageRouter.init() failed!");
        }
    }

    /**
     * start
     */
    public void start() {
        this.inBox.start();
        this.sendBox.start();
    }

    /**
     * stop
     */
    public void stop() {
        this.sendBox.stop();
        this.inBox.stop();
    }

    /**
     * routeMessage
     *
     * @param imMessage
     */
    public void routeMessage(ImMessage imMessage) {
        RoutableMessage<ImMessage> routableImMessage = new RoutableImMessage(imMessage);
        List<ServiceInstance> serviceInstances = this.serviceDiscovery();
        serviceInstances.sort((item1, item2) -> {
            String sortValue1 = item1.getHost() + ":" + item1.getPort();
            String sortValue2 = item2.getHost() + ":" + item2.getPort();

            return sortValue1.compareTo(sortValue2);
        });

        // As long as the hash algorithm is good, it can still ensure its balance.
        int serviceInstanceIndex = Math.toIntExact(Math.abs(routableImMessage.getHashCode()) % serviceInstances.size());
        routableImMessage.setTargetServiceInstance(serviceInstances.get(serviceInstanceIndex));

        this.sendBox.put(routableImMessage);
    }

    /**
     * receiveMessage
     *
     * @param imMessage
     */
    public void receiveMessage(ImMessage imMessage) {
        this.inBox.put(new RoutableImMessage(imMessage));
    }

    /**
     * serviceDiscovery
     *
     * @return
     */
    private List<ServiceInstance> serviceDiscovery() {
        List<ServiceInstance> serviceInstances = this.serviceDiscoveryClient.getInstances(this.config.getServerName());
        if (CollectionUtils.isEmpty(serviceInstances)) {
            throw new ChatbotException(NO_AVAILABLE_SERVER);
        }

        return serviceInstances;
    }
}
