package cn.bossfriday.chatbot.core.mailbox;

import cn.bossfriday.chatbot.common.ChatRobotRuntimeException;
import cn.bossfriday.chatbot.core.MessageDispatcher;
import cn.bossfriday.chatbot.core.client.RoutableMessageClient;
import cn.bossfriday.chatbot.core.message.RoutableMessage;
import cn.bossfriday.chatbot.entity.ChatRobotConfig;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * MessageSendBox
 *
 * @author chenx
 */
@Slf4j
public class MessageSendBox extends MailBox {

    private ChatRobotConfig config;
    private MessageInBox inBox;
    private InetSocketAddress selfAddress;
    private ConcurrentHashMap<InetSocketAddress, RoutableMessageClient> clientMap = new ConcurrentHashMap<>();
    private MessageDispatcher messageDispatcher;

    public MessageSendBox(ChatRobotConfig config, MessageInBox inBox, InetSocketAddress selfAddress, MessageDispatcher messageDispatcher) {
        super(config.getMailBoxQueueSize());

        this.config = config;
        this.inBox = inBox;
        this.selfAddress = selfAddress;
        this.messageDispatcher = messageDispatcher;
    }

    @Override
    public void onMessageReceived(RoutableMessage msg) {
        if (Objects.isNull(msg)) {
            throw new ChatRobotRuntimeException("The input routableMessage is null!");
        }

        if (Objects.isNull(msg.getTargetServiceInstance())) {
            throw new ChatRobotRuntimeException("The input routableMessage.targetServiceInstance is null!");
        }

        // local process need not network IO, enqueue directly. the localRouteNoNetwork config only just for testing.
        InetSocketAddress targetAddress = new InetSocketAddress(msg.getTargetServiceInstance().getHost(), msg.getTargetServiceInstance().getPort());
        if (this.selfAddress.equals(targetAddress) && this.config.isLocalRouteNoNetwork()) {
            this.inBox.put(msg);
            return;
        }

        // remote process
        if (!this.clientMap.containsKey(targetAddress)) {
            RoutableMessageClient client = new RoutableMessageClient(this.getPoolingClientName(targetAddress), this.config.getRestClientMaxTotal(), this.config.getRestClientMaxPerRoute());
            this.clientMap.putIfAbsent(targetAddress, client);
        }

        this.messageDispatcher.getChatGptInvokerExecutor(msg.getHashCode()).execute(() -> this.clientMap.get(targetAddress).send(msg));
    }

    @Override
    public void onStop() {
        try {
            this.clientMap.forEach((key, value) -> value.shutdown());
            this.clientMap = new ConcurrentHashMap<>(8);
        } catch (Exception e) {
            log.error("MessageSendBox.stop() error!", e);
        }
    }

    /**
     * getName
     *
     * @param targetAddress
     * @return
     */
    private String getPoolingClientName(InetSocketAddress targetAddress) {
        return "RoutableMessagePoolingClient-" + targetAddress.toString();
    }
}
