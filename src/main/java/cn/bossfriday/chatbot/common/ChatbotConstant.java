package cn.bossfriday.chatbot.common;

import lombok.experimental.UtilityClass;

/**
 * ChatbotConstant
 *
 * @author chenx
 */
@UtilityClass
public class ChatbotConstant {

    /**
     * Service
     */
    public static final int SERVICE_MAX_CONTEXT_CACHE_RING_SIZE = 255;
    public static final String SERVICE_AI_MODEL_TXT = "text-davinci-003";
    public static final int SERVICE_CHOICE_COUNT = 1;
    public static final String SERVICE_ALGORITHM_NAME_SHA1 = "SHA-1";

    /**
     * URL
     */
    public static final String URL_PREFIX = "/chatRobot";
    public static final String URL_VERSION = "/v1";
    public static final String URL_MOCK = "/mock";
    public static final String URL_BASE = URL_PREFIX + URL_VERSION;
    public static final String URL_MOCK_BASE = URL_PREFIX + URL_MOCK;
    public static final String URL_RECEIVE_IM_MSG = URL_BASE + "/receiveImMsg";
    public static final String URL_RECEIVE_ROUTED_MSG = URL_BASE + "/receiveRoutedMsg";
    public static final String URL_MOCK_OPENAI_COMPLETIONS = URL_MOCK_BASE + "/completions";
    public static final String URL_MOCK_IM_PUBLISH = URL_MOCK_BASE + "/publish.json";

    /**
     * IM
     */
    public static final String IM_MESSAGE_TYPE_TXT = "RC:TxtMsg";
    public static final String IM_MESSAGE_TYPE_IMG_TXT = "RC:ImgTextMsg";
    public static final int IM_SERVER_API_RESULT_CODE_OK = 200;
    public static final String IM_SERVER_API_HEADER_APP_KEY = "RC-App-Key";
    public static final String IM_SERVER_API_HEADER_NONCE = "RC-Nonce";
    public static final String IM_SERVER_API_HEADER_SIGNATURE = "RC-Signature";
    public static final String IM_SERVER_API_HEADER_TIMESTAMP = "RC-Timestamp";
}
