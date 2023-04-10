package cn.bossfriday.chatbot.controller;

import cn.bossfriday.chatbot.entity.im.ImMessage;
import cn.bossfriday.chatbot.entity.result.Result;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import static cn.bossfriday.chatbot.common.ChatRobotConstant.URL_RECEIVE_IM_MSG;

/**
 * ChatbotController
 *
 * @author chenx
 */
@RestController
@Slf4j
public class ChatbotController {

    @ApiOperation(value = "Receive IM Message")
    @PostMapping(value = URL_RECEIVE_IM_MSG, consumes = {MediaType.APPLICATION_FORM_URLENCODED_VALUE})
    public Result<Object> onImMessageReceived(@Validated ImMessage request) {
        log.info(request.toString());
        return Result.ok();
    }
}
