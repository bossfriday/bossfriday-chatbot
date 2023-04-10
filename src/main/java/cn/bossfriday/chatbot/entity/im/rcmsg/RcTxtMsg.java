package cn.bossfriday.chatbot.entity.im.rcmsg;

import lombok.*;

/**
 * RcTxtMsg
 *
 * @author chenx
 */
@ToString
@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class RcTxtMsg {

    private String content;

    private String extra;

}
