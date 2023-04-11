package cn.bossfriday.chatbot.common;

import cn.bossfriday.chatbot.entity.result.ResultCode;

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

    public ChatRobotRuntimeException(ResultCode resultCode) {
        this(resultCode.getMessage() + "(" + resultCode.getCode() + ")");
    }

    public ChatRobotRuntimeException(String msg, Exception e) {
        super(msg, e);
    }

    @Override
    public synchronized Throwable fillInStackTrace() {
        return null;
    }
}
