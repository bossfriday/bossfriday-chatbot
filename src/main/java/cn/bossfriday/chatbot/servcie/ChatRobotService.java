package cn.bossfriday.chatbot.servcie;

import cn.bossfriday.chatbot.common.enums.ImMessageType;
import cn.bossfriday.chatbot.core.MessageRouter;
import cn.bossfriday.chatbot.entity.im.ImMessage;
import cn.bossfriday.chatbot.utils.ChatRobotUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * ChatRobotService
 *
 * @author chenx
 */
@Slf4j
@Service
public class ChatRobotService {

    @Autowired
    private MessageRouter router;

    /**
     * onImMessageReceived
     *
     * @param message
     */
    public void onImMessageReceived(ImMessage message) {
        try {
            if (Objects.isNull(message)) {
                log.warn("The input of ChatRobotService.onImMessageReceived() is null!");
                return;
            }

            log.info("ChatRobotService.onImMessageReceived(): " + ChatRobotUtils.getImMessageRouterAccessLogInfo(message));
            ImMessageType messageType = ImMessageType.parse(message.getObjectName());
            if (messageType.isIgnore()) {
                log.warn("ChatRobotService.onImMessageReceived() ignore a routableMessage: " + message);
                return;
            }

            this.router.routeMessage(message);
        } catch (Exception ex) {
            log.error("ChatRobotService.onImMessageReceived() error!", ex);
        }
    }

    /**
     * onRoutedMessageReceived
     *
     * @param message
     */
    public void onRoutedMessageReceived(ImMessage message) {
        try {
            this.router.receiveMessage(message);
        } catch (Exception ex) {
            log.error("ChatRobotService.onRoutedMessageReceived() error!", ex);
        }
    }
}
