package cn.bossfriday.chatbot.entity.result;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import static cn.bossfriday.chatbot.common.enums.ChatbotResultCode.SUCCESS;

/**
 * Result
 *
 * @author chenx
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Result<T> {

    private int code;

    private String msg;

    private T data;

    /**
     * isSuccess
     *
     * @return
     */
    @JsonIgnore
    public boolean isSuccess() {
        return SUCCESS.getCode() == this.code;
    }

    /**
     * ok
     *
     * @return
     */
    public static Result<Object> ok() {
        return Result.builder()
                .code(SUCCESS.getCode())
                .msg(SUCCESS.getMessage())
                .build();
    }

    /**
     * ok
     *
     * @param resultCode
     * @return
     */
    public static Result<Object> ok(ResultCode resultCode) {
        return builder().code(resultCode.getCode()).msg(resultCode.getMessage()).build();
    }

    /**
     * ok
     *
     * @param data
     * @return
     */
    public static Result<Object> ok(Object data) {
        return builder()
                .code(SUCCESS.getCode())
                .msg(SUCCESS.getMessage())
                .data(data)
                .build();
    }

    /**
     * error
     *
     * @param resultCode
     * @return
     */
    public static Result<Object> error(ResultCode resultCode) {
        return builder().code(resultCode.getCode()).msg(resultCode.getMessage()).build();
    }
}
