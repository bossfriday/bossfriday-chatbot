# 1. 背景
要基于GPT自己去实现一个聊天机器人服务功能实现上其实特别简单：将上游服务过来的请求转换为GPT接口请求发出去然后直接返回或者回调给上游服务即可。但是其中的一些其他问题不知道大家有没有考虑过？
1、搞成一个大同步的实现，当并发真的上来之后，连接和其他处理所持有了大量文件句柄或者源端口后导致不得不重启大法怎么办？
2、GPT-API时常拒绝了你的请求需要统一考虑流控/重试/熔断怎么做？
3、为了支持聊天上下文关联需要存储每个用户的对话内容，这个高写高读的场景如何才能稳定而高效？
4、服务端如何保障每个用户与GPT和下游服务的交互一定是一问一答？

在电影《功夫》中有这样一段对白：这块布料的艺术成分很高。有多高？三四楼那么高啦……那么，这里我给大家介绍一个艺术成分很高的GPT聊天机器人服务实现，那么有多高呢？**七八楼那么高啦**，这七八楼是个啥详见以下，源码地址最后给出。

1、**实现了一个简化版的Akka邮箱机制**（解耦上游服务ImServer->chatBotService->chartGPT交互，同时可以为可能需要的流控/重试/熔断提供统一的处理层）；
2、**基于邮箱队列机制实现同节点RPC不走网络**（减少RPC网络IO）；
3、**使用Disruptor构建有界邮箱队列**（1：可以减少队列对象的GC；2：Disruptor出队监听采用等待序列栅栏信号方式实现，相对传统while true的自旋等待方式可节省1核CPU）；
4、**使用Caffeine缓存用户最近会话用户上下文关联**（W-TinyLFU的缓存淘汰策略可以让有限内存能够支持更大的业务并发，同时相对Redis具备更高的稳定性）；
5、**使用环形List的数据结构表达用户最近会话**（从根源上避免了因多次用户对话导致内存溢出的可能性）；
6、**使用Murmur64哈希算法实现一致性路由及线程一致性保障**（计算效率高且平衡性好）；
7、**采用紧凑自定义方式序列化用户最近会话信息**（节省内存，让有限的内存可以直接更多的用户并发）；
8、通过保障处理线程的一致性**确保与ChatGPT的交互一定是一问一答的串行方式**（原则上来说客户端也要实现问答padding的UI交互限制）；
# 2. 主要设计点
在考虑上述的那七八楼之前，其实我首先考虑的是到底用Springboot去做httpServer还是使用Netty，其实在我的心里Springboot + openFeign这套东西根本不适用于高并发的ToC场景，无奈的是基于各方面的原因，最终我还是妥协的选择了用Springboot的传统套路，不过我摒弃了openFeign，原因是由于想使用本地缓存，因此需要一致性路由，然后就自然的想到了RPC中基本的服务注册、发现、路由，然后索性打算去实现一个猴版的Akka邮箱机制，然后就继续想到了后续那2,3,4,5,6,7,8。
## 2.1 猴版Akka邮箱机制
实现一个简化版的Akka邮箱机制其目的是：
1、解耦ImServer（上游服务）->chatBotService（聊天机器人服务）->chartGPT（GPI-API）交互。
2、为将来可能需要的流控/重试/熔断提供一个统一的处理层。

## 2.2 同节点RPC不走网络
之前用java实现了一个Akka（详见：https://github.com/bossfriday/bossfriday-nubybear/tree/master/cn.bossfriday.common），
在实现的过程中同时实现了一个AbstractServiceBootstrap，这个Bootstrap可以理解为一个容器，启动时将所有PluginElements中的BaseUntypedActor加载到容器中，同时完成服务注册。大家可以把每一个PluginElement认为是一个微服务（每个微服务可以含有N个Actor，一个系统由N个微服务构成），把这个容器类比为tomcat，由于所有的Actor均运行于该容器内，因此可以做到同节点的ActorRPC不走网络。由于设计思路已经往这个方面走了，于是这里也索性实现同节点RPC不走网络，虽然这里有点low，只是一个http方式的RPC（没有使用openFegin，自定义了路由方式，利用RestTemplate实现）。这样的好处显而易见：减少网络IO。需要说明的是，为了自测方便，我增加了一个配置：application.yml.bossfriday.chatbot.service.localRouteNoNetwork去最终决定是否开启。

## 2.3 使用Disruptor构建有界邮箱队列
在最开始的实现中，我还是按照常规套路：LinkedBlockingQueue + while true自旋的方式去不断take，实现如下：

```java
/**
 * MailBox
 *
 * @author chenx
 */
@Slf4j
public abstract class MailBox {

    protected final LinkedBlockingQueue<RoutableMessage<?>> queue;
    protected boolean isStart = true;

    protected MailBox(LinkedBlockingQueue<RoutableMessage<?>> queue) {
        this.queue = queue;
    }

    /**
     * start
     */
    public void start() {
        new Thread(() -> {
            while (this.isStart) {
                try {
                    RoutableMessage<?> routableMsg = this.queue.take();
                    this.onMessageReceived(routableMsg);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    log.error("MailBox.process() error!", ex);
                } catch (Exception e) {
                    log.error("MailBox.process() error!", e);
                }
            }
        }).start();
    }

    /**
     * stop
     */
    public void stop() {
        try {
            this.isStart = false;
            this.queue.clear();
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
            this.queue.put(routableMsg);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            log.error("MailBox.put() error!", ex);
        } catch (Exception ex) {
            log.error("MailBox.put() error!", ex);
        }
    }
}

```

后来我改成了使用Disruptor去构建有界邮箱队列，不过相比之前的方式至少可以获得如下收益：
1、Disruptor出队监听采用等待序列栅栏信号方式实现，相对传统while true的自旋等待方式可节省1核CPU
2、Disruptor采用环形直接内存存储队列对象，这样可以减少队列对象的GC。
关于Disruptor，之前写过一篇介绍文章： [先进先出的高性能的有界内存队列Disruptor简介](https://blog.csdn.net/camelials/article/details/123492015)，有兴趣的同学可以去扒拉一下。

## 2.4 邮箱实现代码
### 2.4.1 MailBox

```java
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
```
### 2.4.2 MessageInBox

```java
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
```

### 2.4.3 MessageSendBox

```java
import cn.bossfriday.chatbot.common.ChatbotException;
import cn.bossfriday.chatbot.core.MessageDispatcher;
import cn.bossfriday.chatbot.core.client.RoutableMessageClient;
import cn.bossfriday.chatbot.core.message.RoutableMessage;
import cn.bossfriday.chatbot.entity.ChatbotConfig;
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

    private ChatbotConfig config;
    private MessageInBox inBox;
    private InetSocketAddress selfAddress;
    private ConcurrentHashMap<InetSocketAddress, RoutableMessageClient> clientMap = new ConcurrentHashMap<>();
    private MessageDispatcher messageDispatcher;

    public MessageSendBox(ChatbotConfig config, MessageInBox inBox, InetSocketAddress selfAddress, MessageDispatcher messageDispatcher) {
        super(config.getMailBoxQueueSize());

        this.config = config;
        this.inBox = inBox;
        this.selfAddress = selfAddress;
        this.messageDispatcher = messageDispatcher;
    }

    @Override
    public void onMessageReceived(RoutableMessage msg) {
        if (Objects.isNull(msg)) {
            throw new ChatbotException("The input routableMessage is null!");
        }

        if (Objects.isNull(msg.getTargetServiceInstance())) {
            throw new ChatbotException("The input routableMessage.targetServiceInstance is null!");
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
```
## 2.5 使用Caffeine缓存用户最近会话
在博文[《本地缓存代码实例及常见缓存淘汰策略简介》](https://blog.csdn.net/camelials/article/details/129406247)我曾提到过：要把redis用好，涉及到的方方面面也不少，另外本地缓存相对使用redis之类的中间件在稳定性、在私有化部署、国产化适配、跨平台等方面就具备天生优势。因此，这里我优先考虑本地缓存方案。不过有一点需要说明的是：用户最近会话对数据的高可用性要求不高，因为当前我采取的方式是：只关联用户最近时间的N条对话（时间/条数可配置），聊着聊着后面的会话就把前面的会话给覆盖了，因此即使服务器重启，本地缓存丢失，用户接着聊几句会话关联能力就又恢复了，而且也不是每个对话都需要进行上下文会话关联。

上下文关联器ChatContextCorrelator实现代码如下：

```java
import com.beem.chat.robot.api.entity.ChatRobotConfig;
import com.beem.chat.robot.api.entity.im.ImMessage;
import com.beem.chat.robot.api.entity.request.OpenAiCompletionRequest;
import com.beem.chat.robot.api.exception.ChatbotException;
import com.beem.chat.robot.provider.common.ConcurrentCircularList;
import com.beem.chat.robot.provider.utils.ChatRobotUtils;
import com.beem.chat.robot.provider.utils.CircularListCodecUtils;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static com.beem.chat.robot.api.constant.ChatRobotConstant.SERVICE_AI_MODEL_TXT;
import static com.beem.chat.robot.api.constant.ChatRobotConstant.SERVICE_CHOICE_COUNT;

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
            throw new ChatbotException("The input ImMessage is null!");
        }

        if (StringUtils.isEmpty(message.getBusChannel()) || StringUtils.isEmpty(message.getFromUserId())) {
            throw new ChatbotException("ImMessage.busChannel or fromUserId is empty!");
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
            throw new ChatbotException("chatContextList is null or empty!");
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

```
## 2.6 使用环形List的数据结构表达用户最近会话
上面说了上下文会话关联采取的方式是只关联用户最近时间的N条对话（时间/条数可配置），聊着聊着后面的对话就把前面的对话给覆盖了这就是一个典型的环形List数据结构。既然是写GPT聊天机器人服务，那么我索性让GPT帮我写了这段代码：[一个用聊天的方式让ChatGPT写的线程安全的环形List](https://blog.csdn.net/camelials/article/details/129824903)

```java
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * ConcurrentCircularList
 *
 * @author chenx
 */
public class ConcurrentCircularList<T> implements Iterable<T> {

    private Object[] elements;
    private int size;
    private int headIndex;
    private int tailIndex;
    private Lock lock = new ReentrantLock();

    public ConcurrentCircularList(int capacity) {
        if (capacity < 1) {
            throw new IllegalArgumentException("Capacity must be at least 1");
        }

        this.elements = new Object[capacity];
        this.size = 0;
        this.headIndex = 0;
        this.tailIndex = 0;
    }

    /**
     * add
     *
     * @param element
     */
    public void add(T element) {
        this.lock.lock();
        try {
            this.elements[this.tailIndex] = element;
            if (this.size == this.elements.length) {
                this.headIndex = (this.headIndex + 1) % this.elements.length;
            } else {
                this.size++;
            }

            this.tailIndex = (this.tailIndex + 1) % this.elements.length;
        } finally {
            this.lock.unlock();
        }
    }

    /**
     * get
     *
     * @param index
     * @return
     */
    public T get(int index) {
        this.lock.lock();
        try {
            if (index < 0 || index >= this.size) {
                throw new IndexOutOfBoundsException("Index " + index + " is out of bounds");
            }

            int i = (this.headIndex + index) % this.elements.length;
            return (T) this.elements[i];
        } finally {
            this.lock.unlock();
        }
    }

    /**
     * size
     *
     * @return
     */
    public int size() {
        this.lock.lock();
        try {
            return this.size;
        } finally {
            this.lock.unlock();
        }
    }

    /**
     * capacity
     *
     * @return
     */
    public int capacity() {
        return this.elements.length;
    }

    /**
     * isEmpty
     *
     * @return
     */
    public boolean isEmpty() {
        this.lock.lock();
        try {
            return this.size == 0;
        } finally {
            this.lock.unlock();
        }
    }

    @Override
    public Iterator<T> iterator() {
        return new CircularListIterator();
    }

    private class CircularListIterator implements Iterator<T> {

        private int current;
        private boolean removable;
        private int remaining;

        public CircularListIterator() {
            this.current = ConcurrentCircularList.this.headIndex;
            this.removable = false;
            this.remaining = ConcurrentCircularList.this.size;
        }

        @Override
        public boolean hasNext() {
            return this.remaining > 0;
        }

        @Override
        public T next() {
            if (!this.hasNext()) {
                throw new NoSuchElementException();
            }

            T element = (T) ConcurrentCircularList.this.elements[this.current];
            this.removable = true;
            this.current = (this.current + 1) % ConcurrentCircularList.this.elements.length;
            this.remaining--;

            return element;
        }

        @Override
        public void remove() {
            if (!this.removable) {
                throw new IllegalStateException();
            }

            int deleteIndex = (this.current - 1 + ConcurrentCircularList.this.elements.length) % ConcurrentCircularList.this.elements.length;
            this.current = (this.current - 1 + ConcurrentCircularList.this.elements.length) % ConcurrentCircularList.this.elements.length;
            ConcurrentCircularList.this.elements[deleteIndex] = null;
            ConcurrentCircularList.this.headIndex = this.current;
            ConcurrentCircularList.this.size--;
            this.remaining--;
            this.removable = false;
        }
    }
}

```

## 2.7 使用紧凑自定义方式序列化用户最近会话
不管用redis之类的分布式缓存中间件还是本地缓存去存储用户最近会话，都需要考虑尽量节省内存的问题。八股文中经常会看到什么一个空对象占用多少字节，对象头里有啥（对象hashCode、和GC相关的……），如何补码……，这个那个的，反正要想说的很全，真的需要提前准备。这里我采用ByteArrayInputStream和ByteArrayOutputStream对 CircularList< String >去做自定义的紧凑序列化，从下面的代码可以看出：除了String类型的会话内容外（当然utf8String自身的数据结构是前2字节存储字符串长度），只多用了2个字节（1个字节存储：circularListSize，1个字节存储circularListCapacity）。效率方面，我简单测试了一下：好像是1万次10条对话的环形list序列化+反序列化 80多毫秒左右吧（公司发的17年破笔记本运行）。

```java
import cn.bossfriday.chatbot.common.ChatbotException;
import cn.bossfriday.chatbot.common.ConcurrentCircularList;
import lombok.experimental.UtilityClass;

import java.io.*;

import static cn.bossfriday.chatbot.common.ChatbotConstant.SERVICE_MAX_CONTEXT_CACHE_RING_SIZE;

/**
 * CircularListCodecUtils
 *
 * @author chenx
 */
@UtilityClass
public class CircularListCodecUtils {

    /**
     * encodeStringList
     *
     * @param circularList
     * @param circularListCapacity
     * @return
     * @throws IOException
     */
    public static byte[] encodeStringList(ConcurrentCircularList<String> circularList, int circularListCapacity) throws IOException {
        if (circularList == null || circularList.isEmpty()) {
            throw new ChatbotException("The input chatContextList is null or empty!");
        }

        try (ByteArrayOutputStream out = new ByteArrayOutputStream();
             DataOutputStream dos = new DataOutputStream(out)) {
            int circularListSize = circularList.size();
            if (circularListCapacity < 0 || circularListCapacity > SERVICE_MAX_CONTEXT_CACHE_RING_SIZE) {
                throw new IllegalArgumentException("The input chatContextList.size() must be between 0 and 256!");
            }

            dos.writeByte((byte) circularListSize);
            dos.writeByte((byte) circularListCapacity);
            for (String item : circularList) {
                dos.writeUTF(item);
            }

            return out.toByteArray();
        }
    }

    /**
     * decodeStringList
     *
     * @param bytes
     * @return
     * @throws IOException
     */
    public static ConcurrentCircularList<String> decodeStringList(byte[] bytes) throws IOException {
        try (ByteArrayInputStream in = new ByteArrayInputStream(bytes);
             DataInputStream dis = new DataInputStream(in)) {

            int circularListSize = Byte.toUnsignedInt(dis.readByte());
            int circularListCapacity = Byte.toUnsignedInt(dis.readByte());
            if (circularListSize > circularListCapacity) {
                throw new IllegalArgumentException("circularListSize must <= circularListCapacity!");
            }

            ConcurrentCircularList<String> circularList = new ConcurrentCircularList<>(circularListCapacity);
            for (int i = 0; i < circularListSize; i++) {
                circularList.add(dis.readUTF());
            }

            return circularList;
        }
    }
}
```

## 2.8 使用Murmur64哈希算法实现一致性路由及线程一致性保障
哈希算法很多，可能大家都爱用MD5,SHA1等，虽然他们的离散性都很好（特别是MD5就是一个高离散度的哈希算法），但其计算效率并不是太高。Murmur哈希算法则是平衡了离散性和计算效率。
在配套本地缓存的一致性性路由、一问一答串行保障上的处理，我均是将一个资源ID（例如：组织ID+用户ID）进行Murmur64之后取绝对值然后再取余来处理的（觉得这里没有必要去搞一个一致性哈希环，去支持通过配置虚拟节点数去控制其权重了）。我测试了一下，用这种简单的实现方式：1万个请求，2个节点进行路由，最后2个节点请求总数一般都是1个5000多几十，另一个5000少几十，基本上2个节点负载均衡差异在1%左右。

```java
import cn.bossfriday.chatbot.common.ChatbotException;
import org.apache.commons.lang3.StringUtils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

/**
 * MurmurHashUtils
 *
 * @author chenx
 */
public class MurmurHashUtils {

    private static final int INT_BYTE_LENGTH = 4;
    private static final int LONG_BYTE_LENGTH = 8;

    private MurmurHashUtils() {

    }

    /**
     * hash64
     *
     * @param key
     * @return
     */
    public static long hash64(String key) {
        if (StringUtils.isEmpty(key)) {
            throw new ChatbotException("input key is null or empty!");
        }

        return hash64(key.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * hash64
     *
     * @param key
     * @return
     */
    public static long hash64(byte[] key) {
        return hash64A(key, 0x1234ABCD);
    }

    /**
     * hash32
     *
     * @param key
     * @return
     */
    public static int hash32(String key) {
        if (StringUtils.isEmpty(key)) {
            throw new ChatbotException("input key is null or empty!");
        }

        return hash(key.getBytes(StandardCharsets.UTF_8), 0x1234ABCD);
    }

    /**
     * hash32
     *
     * @param key
     * @return
     */
    public static int hash32(byte[] key) {
        return hash(key, 0x1234ABCD);
    }

    /**
     * Hashes bytes in an array.
     *
     * @param data The bytes to hash.
     * @param seed The seed for the hash.
     * @return The 32 bit hash of the bytes in question.
     */
    public static int hash(byte[] data, int seed) {
        return hash(ByteBuffer.wrap(data), seed);
    }

    /**
     * Hashes bytes in part of an array.
     *
     * @param data   The data to hash.
     * @param offset Where to start munging.
     * @param length How many bytes to process.
     * @param seed   The seed to start with.
     * @return The 32-bit hash of the data in question.
     */
    public static int hash(byte[] data, int offset, int length, int seed) {
        return hash(ByteBuffer.wrap(data, offset, length), seed);
    }

    /**
     * Hashes the bytes in a buffer from the current position to the limit.
     *
     * @param buf  The bytes to hash.
     * @param seed The seed for the hash.
     * @return The 32 bit murmur hash of the bytes in the buffer.
     */
    public static int hash(ByteBuffer buf, int seed) {
        // save byte order for later restoration
        ByteOrder byteOrder = buf.order();
        buf.order(ByteOrder.LITTLE_ENDIAN);

        int m = 0x5bd1e995;
        int r = 24;

        int h = seed ^ buf.remaining();

        int k;
        while (buf.remaining() >= INT_BYTE_LENGTH) {
            k = buf.getInt();

            k *= m;
            k ^= k >>> r;
            k *= m;

            h *= m;
            h ^= k;
        }

        if (buf.remaining() > 0) {
            ByteBuffer finish = ByteBuffer.allocate(INT_BYTE_LENGTH).order(ByteOrder.LITTLE_ENDIAN);
            finish.put(buf).rewind();
            h ^= finish.getInt();
            h *= m;
        }

        h ^= h >>> 13;
        h *= m;
        h ^= h >>> 15;

        buf.order(byteOrder);
        return h;
    }

    /**
     * hash64A
     *
     * @param data
     * @param seed
     * @return
     */
    public static long hash64A(byte[] data, int seed) {
        return hash64A(ByteBuffer.wrap(data), seed);
    }

    /**
     * hash64A
     *
     * @param data
     * @param offset
     * @param length
     * @param seed
     * @return
     */
    public static long hash64A(byte[] data, int offset, int length, int seed) {
        return hash64A(ByteBuffer.wrap(data, offset, length), seed);
    }

    /**
     * hash64A
     *
     * @param buf
     * @param seed
     * @return
     */
    public static long hash64A(ByteBuffer buf, int seed) {
        ByteOrder byteOrder = buf.order();
        buf.order(ByteOrder.LITTLE_ENDIAN);

        long m = 0xc6a4a7935bd1e995L;
        int r = 47;

        long h = seed ^ (buf.remaining() * m);

        long k;
        while (buf.remaining() >= LONG_BYTE_LENGTH) {
            k = buf.getLong();

            k *= m;
            k ^= k >>> r;
            k *= m;

            h ^= k;
            h *= m;
        }

        if (buf.remaining() > 0) {
            ByteBuffer finish = ByteBuffer.allocate(LONG_BYTE_LENGTH).order(ByteOrder.LITTLE_ENDIAN);
            finish.put(buf).rewind();
            h ^= finish.getLong();
            h *= m;
        }

        h ^= h >>> r;
        h *= m;
        h ^= h >>> r;

        buf.order(byteOrder);

        return h;
    }
}
```
## 2.9 确保与ChatGPT的交互一定是一问一答的串行方式
如果你的1个问题中包含了几个关系不大的问题，有时候ChatGPT直接就让你一个一个的问了。而且如果你仔细一点，你
也会发现当你提出一个问题之后，在ChatGPT没有回答完毕之前，提问发送按钮和快捷键都是不可用状态的。因此聊天机器人的交互需要保障：一问一答。对于客户端要求他们实现问答padding的UI交互限制，那么服务端其实最好也双重保障一下，方式其实很简单：保障处理线程的一致性即可。具体来说就是构建一个单线程池的数组，每次拿线程的时候根据哈希取余后的结果去拿对应下标的线程。之前面试别人的时候，在多线程相关的问题上，我曾问过单线程池的使用，印象中没有碰到能说出来的。

```java
import cn.bossfriday.chatbot.entity.ChatbotConfig;
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

    public MessageDispatcher(ChatbotConfig config) {
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

```
# 3. 总结
现在基于GPT各种小程序，小应用太多了，也算是热点了，因此写了此文给大家抄作业吧。GPT自身API封装的非常简单，单说功能实现，我相信多数人都没有什么障碍，但是我还是建议大家写东西无论大小，要先思而后动，一定要把场景，可能发生的问题通通考虑清楚，并且注重编程思想，不要功能测试阶段啥问题没有，一到商用，各种问题这个那个的接踵而来。最后附上源码地址：https://github.com/bossfriday/bossfriday-chatbot，
其实代码并不多，我大概用了6/7个工作日完成吧，主要是边写边重构边自测啥的，然后还的搭着干一些杂事。大家下载源码后，本地需要启动一个nacos，启动后通过ChatRobotController.http中receiveImMsg就可以自测了。目前上游服务（ImServer）和GPT-API都是有挡板的（地址可配，当前直接配置而本地mock地址）。
![在这里插入图片描述](https://img-blog.csdnimg.cn/359a44ccf3834b7aad1868cb480fd648.png)


