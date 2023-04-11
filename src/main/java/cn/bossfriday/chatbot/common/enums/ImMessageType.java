package cn.bossfriday.chatbot.common.enums;

import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import static cn.bossfriday.chatbot.common.ChatbotConstant.IM_MESSAGE_TYPE_IMG_TXT;
import static cn.bossfriday.chatbot.common.ChatbotConstant.IM_MESSAGE_TYPE_TXT;

/**
 * ImMessageType
 *
 * @author chenx
 */
public enum ImMessageType {

    /**
     * RC:TxtMsg
     */
    RC_MSG_TXT(1, IM_MESSAGE_TYPE_TXT),

    /**
     * RC:ImgTextMsg
     */
    RC_MSG_IMG_TXT(2, IM_MESSAGE_TYPE_IMG_TXT),

    /**
     * Other Message Type: ignore and do nothing.
     */
    RC_MSG_OTHERS(0, "");

    @Getter
    private int code;

    @Getter
    private String name;

    ImMessageType(int code, String name) {
        this.code = code;
        this.name = name;
    }

    @Override
    public String toString() {
        return this.name;
    }

    /**
     * isIgnore
     *
     * @return
     */
    public boolean isIgnore() {
        return this.getCode() == RC_MSG_OTHERS.getCode();
    }

    /**
     * parse
     *
     * @param name
     * @return
     */
    public static ImMessageType parse(String name) {
        if (StringUtils.isEmpty(name)) {
            return ImMessageType.RC_MSG_OTHERS;
        }

        if (name.equals(IM_MESSAGE_TYPE_TXT)) {
            return ImMessageType.RC_MSG_TXT;
        }

        return ImMessageType.RC_MSG_OTHERS;
    }
}
