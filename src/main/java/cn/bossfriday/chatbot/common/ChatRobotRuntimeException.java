package cn.bossfriday.chatbot.common;

/**
 * ChatRobotRuntimeException
 *
 * @author chenx
 */
public class ChatRobotRuntimeException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ChatRobotRuntimeException(Exception e) {
        super(e);
    }

    public ChatRobotRuntimeException(String msg) {
        super(msg);
    }

    public ChatRobotRuntimeException(String msg, Exception e) {
        super(msg, e);
    }

    @Override
    public synchronized Throwable fillInStackTrace() {
        return null;
    }
}
