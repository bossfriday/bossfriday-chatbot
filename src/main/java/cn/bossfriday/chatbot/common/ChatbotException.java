package cn.bossfriday.chatbot.common;

import cn.bossfriday.chatbot.entity.result.ResultCode;

/**
 * ChatbotException
 *
 * @author chenx
 */
public class ChatbotException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ChatbotException(Exception e) {
        super(e);
    }

    public ChatbotException(String msg) {
        super(msg);
    }

    public ChatbotException(ResultCode resultCode) {
        this(resultCode.getMessage() + "(" + resultCode.getCode() + ")");
    }

    public ChatbotException(String msg, Exception e) {
        super(msg, e);
    }

    @Override
    public synchronized Throwable fillInStackTrace() {
        return null;
    }
}
