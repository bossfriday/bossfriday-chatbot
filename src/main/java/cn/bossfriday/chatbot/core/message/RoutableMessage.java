package cn.bossfriday.chatbot.core.message;

import cn.bossfriday.chatbot.common.ChatRobotRuntimeException;
import lombok.Getter;
import lombok.Setter;
import org.springframework.cloud.client.ServiceInstance;

import java.util.Objects;

/**
 * RoutableMessage
 *
 * @author chenx
 */
public abstract class RoutableMessage<T> {

    @Getter
    @Setter
    private ServiceInstance targetServiceInstance;

    private Long hashCode;

    @Getter
    private T payload;

    protected RoutableMessage(T payload) {
        if (Objects.isNull(payload)) {
            throw new ChatRobotRuntimeException("The input payload is null!");
        }

        this.payload = payload;
        this.hashCode = Long.MIN_VALUE;
    }

    /**
     * getHashCode
     *
     * @return
     */
    public Long getHashCode() {
        try {
            if (this.hashCode == Long.MIN_VALUE) {
                this.hashCode = this.calculateHashCode();
            }

            return this.hashCode;
        } catch (Exception ex) {
            throw new ChatRobotRuntimeException("RoutableMessage.getHashCode() error! msg: " + ex.getMessage());
        }
    }

    /**
     * calculateHashCode
     *
     * @return
     */
    protected abstract Long calculateHashCode();

    @Override
    public String toString() {
        return this.payload.toString();
    }
}
