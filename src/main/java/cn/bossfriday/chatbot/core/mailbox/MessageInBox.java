package cn.bossfriday.chatbot.core.mailbox;

import cn.bossfriday.chatbot.common.ChatbotException;
import cn.bossfriday.chatbot.core.MessageDispatcher;
import cn.bossfriday.chatbot.core.client.ChatGptClient;
import cn.bossfriday.chatbot.core.message.RoutableImMessage;
import cn.bossfriday.chatbot.core.message.RoutableMessage;
import cn.bossfriday.chatbot.entity.ChatbotConfig;
import lombok.extern.slf4j.Slf4j;

/**
 * MessageInBox
 *
 * @author chenx
 */
@Slf4j
public class MessageInBox extends MailBox {

    private MessageDispatcher messageDispatcher;
    private ChatGptClient chatGptClient;

    public MessageInBox(ChatbotConfig config, MessageDispatcher messageDispatcher) {
        super(config.getMailBoxQueueSize());

        this.chatGptClient = new ChatGptClient("ChatGptPoolingClient", config, messageDispatcher);
        this.messageDispatcher = messageDispatcher;
    }

    @Override
    public void onMessageReceived(RoutableMessage msg) {
        this.messageDispatcher.getChatGptInvokerExecutor(msg.getHashCode()).execute(() -> {
            if (msg instanceof RoutableImMessage) {
                this.chatGptClient.send((RoutableImMessage) msg);
            } else {
                throw new ChatbotException("MessageInBox received an unsupported message!");
            }
        });
    }

    @Override
    public void onStop() {
        try {
            this.chatGptClient.shutdown();
        } catch (Exception e) {
            log.error("MessageInBox stop() error!", e);
        }
    }
}
