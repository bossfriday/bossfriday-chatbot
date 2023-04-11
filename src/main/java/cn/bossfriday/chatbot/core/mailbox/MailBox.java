package cn.bossfriday.chatbot.core.mailbox;

import cn.bossfriday.chatbot.common.ChatbotException;
import cn.bossfriday.chatbot.core.message.RoutableMessage;
import cn.bossfriday.chatbot.utils.ThreadPoolUtils;
import com.lmax.disruptor.*;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import lombok.extern.slf4j.Slf4j;

/**
 * MailBox
 *
 * @author chenx
 */
@Slf4j
public abstract class MailBox {

    protected Disruptor<MessageEvent> queue;
    protected EventTranslatorOneArg<MessageEvent, RoutableMessage<Object>> eventTranslator;
    protected RingBuffer<MessageEvent> ringBuffer;

    protected MailBox(int capacity) {
        this.queue = new Disruptor<>(
                new MessageEventFactory(),
                getMailBoxBufferSize(capacity),
                ThreadPoolUtils.getThreadFactory("mailBoxQueueThread", null),
                ProducerType.MULTI,
                new YieldingWaitStrategy());
        this.queue.handleEventsWithWorkerPool(new MessageEventHandler());
        this.eventTranslator = new MessageEventTranslator();
    }

    /**
     * start
     */
    public void start() {
        this.ringBuffer = this.queue.start();
        if (this.ringBuffer == null) {
            throw new ChatbotException("MailBox.start() error!");
        }
    }

    /**
     * stop
     */
    public void stop() {
        try {
            this.queue.shutdown();
            this.onStop();
        } catch (Exception e) {
            log.error("MailBox.stop() error!", e);
        }
    }

    /**
     * onMessageReceived
     *
     * @param routableMsg
     */
    public abstract void onMessageReceived(RoutableMessage<?> routableMsg);

    /**
     * onStop
     */
    public abstract void onStop();

    /**
     * put
     *
     * @param routableMsg
     */
    public void put(RoutableMessage<?> routableMsg) {
        try {
            this.ringBuffer.publishEvent(this.eventTranslator, (RoutableMessage<Object>) routableMsg);
        } catch (Exception ex) {
            log.error("MailBox.put() error!", ex);
        }
    }

    /**
     * getMailBoxBufferSize: Ensure that ringBufferSize must be a power of 2
     */
    private static int getMailBoxBufferSize(int num) {
        int size = 2;
        while (size < num) {
            size <<= 1;
        }

        return size < 1024 ? 1024 : size;
    }

    /**
     * MessageEvent
     */
    public class MessageEvent {

        private RoutableMessage<Object> message;

        public RoutableMessage<Object> getMessage() {
            return this.message;
        }

        public void setMessage(RoutableMessage<Object> message) {
            this.message = message;
        }
    }

    /**
     * MessageEventFactory
     */
    public class MessageEventFactory implements EventFactory<MessageEvent> {

        @Override
        public MessageEvent newInstance() {
            return new MessageEvent();
        }
    }

    /**
     * MessageEventTranslator
     */
    public class MessageEventTranslator implements EventTranslatorOneArg<MessageEvent, RoutableMessage<Object>> {

        @Override
        public void translateTo(MessageEvent messageEvent, long l, RoutableMessage<Object> routableMessage) {
            messageEvent.setMessage(routableMessage);
        }
    }

    /**
     * MessageEventHandler
     */
    public class MessageEventHandler implements WorkHandler<MessageEvent> {

        @Override
        public void onEvent(MessageEvent messageEvent) {
            MailBox.this.onMessageReceived(messageEvent.getMessage());
        }
    }
}
