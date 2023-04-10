package cn.bossfriday.chatbot.entity.im.rcmsg;

import lombok.*;

/**
 * RcImgTextMsg
 *
 * @author chenx
 */
@ToString
@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class RcImgTextMsg {

    private String title;

    private String content;

    private String url;

    private String extra;
}
