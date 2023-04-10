package cn.bossfriday.chatbot.entity.im;

import io.swagger.annotations.ApiModelProperty;
import lombok.*;

import javax.validation.constraints.NotEmpty;

/**
 * PublishMessage
 *
 * @author chenx
 */
@ToString
@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ImPublishMessage {

    @ApiModelProperty(value = "fromUserId", required = true)
    @NotEmpty(message = "fromUserId mustn't be empty or null!")
    private String fromUserId;

    @ApiModelProperty(value = "toUserId", required = true)
    @NotEmpty(message = "toUserId mustn't be empty or null!")
    private String[] toUserId;

    @ApiModelProperty(value = "objectName", required = true)
    @NotEmpty(message = "objectName mustn't be empty or null!")
    private String objectName;

    @ApiModelProperty(value = "content", required = true)
    @NotEmpty(message = "content mustn't be empty or null!")
    private String content;
}
