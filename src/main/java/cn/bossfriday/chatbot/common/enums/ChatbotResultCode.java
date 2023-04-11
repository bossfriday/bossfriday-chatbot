package cn.bossfriday.chatbot.common.enums;


import cn.bossfriday.chatbot.entity.result.ResultCode;

/**
 * ChatbotResultCode
 *
 * @author chenx
 */
public enum ChatbotResultCode implements ResultCode {

    // OK
    SUCCESS(0, "ok"),

    // ChatGpt Error(437XX)
    CHAT_GPT_API_HTTP_STATUS_CODE_ERROR(43701, "ChatGpt API Http Status Code Error"),
    CHAT_GPT_API_RESPONSE_BODY_IS_NULL(43702, "ChatGpt API Response Body Is Null"),
    CHAT_GPT_API_RESPONSE_CHOICES_IS_EMPTY(43703, "ChatGpt API Response Choices Is Empty"),

    // Router Error(438XX)
    NO_AVAILABLE_SERVER(43801, "No Available Server"),

    // Other Errors(439XX)
    INTERNAL_ERROR(43999, "System Internal Error");

    private int code;
    private String msg;

    ChatbotResultCode(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    @Override
    public int getCode() {
        return this.code;
    }

    @Override
    public String getMessage() {
        return this.msg;
    }
}
