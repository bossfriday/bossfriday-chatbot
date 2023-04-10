package cn.bossfriday.chatbot.utils;

import cn.bossfriday.chatbot.common.ChatRobotRuntimeException;
import cn.bossfriday.chatbot.common.enums.ImMessageType;
import cn.bossfriday.chatbot.entity.im.ImMessage;
import cn.bossfriday.chatbot.entity.im.rcmsg.RcImgTextMsg;
import cn.bossfriday.chatbot.entity.im.rcmsg.RcTxtMsg;
import com.alibaba.fastjson.JSON;
import lombok.experimental.UtilityClass;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpHeaders;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;

import static cn.bossfriday.chatbot.common.ChatRobotConstant.*;

/**
 * ChatRobotUtils
 *
 * @author chenx
 */
@UtilityClass
public class ChatRobotUtils {

    /**
     * getImMessageRouterAccessLogInfo
     *
     * @param msg
     * @return
     */
    public static String getImMessageRouterAccessLogInfo(ImMessage msg) {
        if (Objects.isNull(msg)) {
            return "";
        }

        return "fromUserId='" + msg.getFromUserId() + '\'' +
                ", msgUID='" + msg.getMsgUID() + '\'' +
                ", busChannel='" + msg.getBusChannel();
    }

    /**
     * getOpenAiApiAuth
     *
     * @param apiAuth
     * @return
     */
    public static String getOpenAiApiAuth(String apiAuth) {
        if (StringUtils.isEmpty(apiAuth)) {
            throw new ChatRobotRuntimeException("apiAuth is null or empty!");
        }

        return "Bearer " + apiAuth;
    }

    /**
     * getImMessageContent
     *
     * @param message
     * @return
     */
    public static String getImMessageContent(ImMessage message) {
        if (Objects.isNull(message)) {
            throw new ChatRobotRuntimeException("The input ImRoutingMessage is null!");
        }

        ImMessageType msgType = ImMessageType.parse(message.getObjectName());
        if (msgType.getCode() == ImMessageType.RC_MSG_TXT.getCode()) {
            return JSON.parseObject(message.getContent(), RcTxtMsg.class).getContent();
        } else if (msgType.getCode() == ImMessageType.RC_MSG_IMG_TXT.getCode()) {
            return JSON.parseObject(message.getContent(), RcImgTextMsg.class).getContent();
        } else {
            throw new ChatRobotRuntimeException("unsupported ImMessageType!");
        }
    }

    /**
     * setImServerApiAuthHeaders
     *
     * @param headers
     * @param appKey
     * @param appSecret
     * @throws NoSuchAlgorithmException
     */
    public static void setImServerApiAuthHeaders(HttpHeaders headers, String appKey, String appSecret) throws NoSuchAlgorithmException {
        if (Objects.isNull(headers)) {
            throw new ChatRobotRuntimeException("headers is null!");
        }

        if (StringUtils.isEmpty(appKey)) {
            throw new ChatRobotRuntimeException("appKey is null!");
        }

        if (StringUtils.isEmpty(appSecret)) {
            throw new ChatRobotRuntimeException("appSecret is null!");
        }

        String nonce = String.valueOf(Math.random() * 1000000);
        String timestamp = String.valueOf(System.currentTimeMillis() / 1000);
        StringBuilder toSign = new StringBuilder(appSecret).append(nonce).append(timestamp);
        String sign = hexSha1(toSign.toString());

        headers.set(IM_SERVER_API_HEADER_APP_KEY, appKey);
        headers.set(IM_SERVER_API_HEADER_NONCE, nonce);
        headers.set(IM_SERVER_API_HEADER_SIGNATURE, sign);
        headers.set(IM_SERVER_API_HEADER_TIMESTAMP, timestamp);
    }

    /**
     * hexSha1
     *
     * @param value
     * @return
     * @throws NoSuchAlgorithmException
     */
    private static String hexSha1(String value) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance(SERVICE_ALGORITHM_NAME_SHA1);
        md.update(value.getBytes(StandardCharsets.UTF_8));
        byte[] digest = md.digest();

        return String.valueOf(Hex.encodeHex(digest));
    }
}
