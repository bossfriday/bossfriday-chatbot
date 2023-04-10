package cn.bossfriday.chatbot.entity.im;

import io.swagger.annotations.ApiModelProperty;
import lombok.*;

import javax.validation.constraints.NotEmpty;

/**
 * ImMessage
 * Reference: https://doc.rongcloud.cn/imserver/server/v1/message/sync
 *
 * @author chenx
 */
@ToString
@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ImMessage {

    @ApiModelProperty(value = "fromUserId", required = true)
    @NotEmpty(message = "fromUserId mustn't be empty or null!")
    private String fromUserId;

    @ApiModelProperty(value = "toUserId", required = true)
    @NotEmpty(message = "toUserId mustn't be empty or null!")
    private String toUserId;

    @ApiModelProperty(value = "objectName", required = true)
    @NotEmpty(message = "objectName mustn't be empty or null!")
    private String objectName;

    @ApiModelProperty(value = "content", required = true)
    @NotEmpty(message = "content mustn't be empty or null!")
    private String content;

    @ApiModelProperty(value = "channelType")
    private String channelType;

    @ApiModelProperty(value = "msgTimestamp")
    private Long msgTimestamp;

    @ApiModelProperty(value = "msgUID")
    private String msgUID;

    @ApiModelProperty(value = "originalMsgUID")
    private String originalMsgUID;

    @ApiModelProperty(value = "sensitiveType")
    private Integer sensitiveType;

    @ApiModelProperty(value = "source")
    private String source;

    @ApiModelProperty(value = "busChannel", required = true)
    @NotEmpty(message = "busChannel mustn't be empty or null!")
    private String busChannel;

    @ApiModelProperty(value = "groupUserIds")
    private String[] groupUserIds;
}
