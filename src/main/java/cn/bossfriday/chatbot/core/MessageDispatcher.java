package cn.bossfriday.chatbot.core;

import cn.bossfriday.chatbot.entity.ChatRobotConfig;
import cn.bossfriday.chatbot.utils.ThreadPoolUtils;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ExecutorService;

/**
 * MessageDispatcher
 *
 * @author chenx
 */
@Slf4j
public class MessageDispatcher {

    private int chatGptDispatcherThreadSize;
    private int imServerDispatcherThreadSize;

    private ExecutorService[] chatGptInvokerThreads = null;
    private ExecutorService[] imServerDispatcherThreads = null;

    public MessageDispatcher(ChatRobotConfig config) {
        this.chatGptDispatcherThreadSize = config.getChatGptDispatcherThreadSize();
        this.imServerDispatcherThreadSize = config.getImServerDispatcherThreadSize();

        this.chatGptInvokerThreads = new ExecutorService[this.chatGptDispatcherThreadSize];
        this.imServerDispatcherThreads = new ExecutorService[this.imServerDispatcherThreadSize];

        for (int i = 0; i < this.chatGptDispatcherThreadSize; i++) {
            this.chatGptInvokerThreads[i] = ThreadPoolUtils.getSingleThreadExecutor("chatGptInvokerThread-" + i);
        }

        for (int i = 0; i < this.imServerDispatcherThreadSize; i++) {
            this.imServerDispatcherThreads[i] = ThreadPoolUtils.getSingleThreadExecutor("imServerDispatcherThread-" + i);
        }

        log.info("MessageDispatcher init done.");
    }

    /**
     * getChatGptInvokerExecutor
     *
     * @param hashCode
     * @return
     */
    public ExecutorService getChatGptInvokerExecutor(Long hashCode) {
        return this.chatGptInvokerThreads[getHashIndex(hashCode, this.chatGptDispatcherThreadSize)];
    }

    /**
     * getImServerInvokerExecutor
     *
     * @param hashCode
     * @return
     */
    public ExecutorService getImServerInvokerExecutor(Long hashCode) {
        return this.imServerDispatcherThreads[getHashIndex(hashCode, this.chatGptDispatcherThreadSize)];
    }

    /**
     * getHashIndex
     *
     * @param hashCode
     * @param threadSize
     * @return
     */
    private static int getHashIndex(Long hashCode, int threadSize) {
        return Math.toIntExact(Math.abs(hashCode) % threadSize);
    }
}
