package cn.bossfriday.chatbot.controller;

import cn.bossfriday.chatbot.entity.im.ImMessage;
import cn.bossfriday.chatbot.entity.result.Result;
import cn.bossfriday.chatbot.servcie.ChatRobotService;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import static cn.bossfriday.chatbot.common.ChatRobotConstant.URL_RECEIVE_IM_MSG;
import static cn.bossfriday.chatbot.common.ChatRobotConstant.URL_RECEIVE_ROUTED_MSG;

/**
 * ChatbotController
 *
 * @author chenx
 */
@RestController
@Slf4j
public class ChatbotController {

    @Autowired
    private ChatRobotService chatRobotService;

    /**
     * onImMessageReceived
     *
     * @param request
     * @return
     */
    @ApiOperation(value = "Receive IM Message")
    @PostMapping(value = URL_RECEIVE_IM_MSG, consumes = {MediaType.APPLICATION_FORM_URLENCODED_VALUE})
    public Result<Object> onImMessageReceived(@Validated ImMessage request) {
        this.chatRobotService.onImMessageReceived(request);
        return Result.ok();
    }

    /**
     * onRoutedMessageReceived
     *
     * @param request
     * @return
     */
    @ApiOperation(value = "Receive RPC Message")
    @PostMapping(value = URL_RECEIVE_ROUTED_MSG, consumes = MediaType.APPLICATION_JSON_VALUE)
    public Result<Object> onRoutedMessageReceived(@RequestBody @Validated ImMessage request) {
        this.chatRobotService.onRoutedMessageReceived(request);
        return Result.ok();
    }
}
