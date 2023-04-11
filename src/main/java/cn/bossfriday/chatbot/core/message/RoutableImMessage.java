package cn.bossfriday.chatbot.core.message;

import cn.bossfriday.chatbot.common.ChatbotException;
import cn.bossfriday.chatbot.entity.im.ImMessage;
import cn.bossfriday.chatbot.utils.MurmurHashUtils;
import org.apache.commons.lang3.StringUtils;

/**
 * RoutableImRoutingMessage
 *
 * @author chenx
 */
public class RoutableImMessage extends RoutableMessage<ImMessage> {

    public RoutableImMessage(ImMessage payload) {
        super(payload);
    }

    @Override
    public Long calculateHashCode() {

        if (StringUtils.isEmpty(this.getPayload().getBusChannel())) {
            throw new ChatbotException("ImMessage.busChannel is empty!");
        }

        if (StringUtils.isEmpty(this.getPayload().getFromUserId())) {
            throw new ChatbotException("ImMessage.fromUserId is empty!");
        }

        return MurmurHashUtils.hash64(this.getPayload().getBusChannel() + "$$" + this.getPayload().getFromUserId());
    }
}
