package com.game.common.exception;

import com.game.common.response.ApiResult;
import com.game.common.user.dto.UserDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.HttpRequestMethodNotSupportedException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 全局异常处理单元测试：错误分层是否清晰、内部异常是否被隐藏。
 */
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    @DisplayName("业务异常：保留自己的错误码与提示")
    void businessExceptionShouldKeepCodeAndMessage() {
        ResponseEntity<ApiResult<Void>> response =
                handler.handleBusinessException(new BusinessException(403, "无权查看该订单"));

        assertThat(response.getStatusCode().value()).isEqualTo(403);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo(403);
        assertThat(response.getBody().getMessage()).isEqualTo("无权查看该订单");
    }

    @Test
    @DisplayName("未预期异常：只返回统一提示，不泄漏内部细节")
    void unexpectedExceptionShouldNotLeakInternalDetail() {
        ResponseEntity<ApiResult<Void>> response = handler.handleException(
                new IllegalStateException("jdbc:mysql://localhost:3306/game, password=1234"));

        assertThat(response.getStatusCode().value()).isEqualTo(500);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo("系统异常，请稍后再试");
        assertThat(response.getBody().getMessage()).doesNotContain("jdbc").doesNotContain("password");
    }

    @Test
    @DisplayName("请求体校验失败：返回可读的字段提示")
    void validationFailureShouldReturnReadableMessage() throws Exception {
        BindException bindException = new BindException(new UserDTO(), "userDTO");
        bindException.rejectValue("username", "NotBlank", "用户名不能为空");
        MethodParameter parameter = new MethodParameter(
                SampleController.class.getDeclaredMethod("register", UserDTO.class), 0);
        MethodArgumentNotValidException exception =
                new MethodArgumentNotValidException(parameter, bindException.getBindingResult());

        ResponseEntity<Object> response = handler.handleMethodArgumentNotValid(
                exception, new HttpHeaders(), HttpStatus.BAD_REQUEST, null);

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(((ApiResult<?>) response.getBody()).getMessage()).contains("用户名不能为空");
    }

    @Test
    @DisplayName("请求体解析失败：不回显请求内容（避免泄漏密码）")
    void unreadableBodyShouldNotEchoContent() {
        ResponseEntity<Object> response = handler.handleHttpMessageNotReadable(
                new HttpMessageNotReadableException("JSON parse error: {\"password\":\"123456\"}"),
                new HttpHeaders(), HttpStatus.BAD_REQUEST, null);

        assertThat(((ApiResult<?>) response.getBody()).getMessage())
                .isEqualTo("请求体格式不正确")
                .doesNotContain("123456");
    }

    @Test
    @DisplayName("框架级异常：状态码不变，提示语固定")
    void frameworkExceptionShouldUseFriendlyMessage() {
        ResponseEntity<Object> response = handler.handleExceptionInternal(
                new HttpRequestMethodNotSupportedException("GET"), null, new HttpHeaders(),
                HttpStatus.METHOD_NOT_ALLOWED, null);

        assertThat(response.getStatusCode().value()).isEqualTo(405);
        assertThat(((ApiResult<?>) response.getBody()).getMessage()).isEqualTo("请求方法不支持");
    }

    /** 仅用于构造 MethodParameter */
    @SuppressWarnings("unused")
    private static class SampleController {
        public void register(UserDTO dto) {
        }
    }
}
