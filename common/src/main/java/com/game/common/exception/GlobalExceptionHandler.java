package com.game.common.exception;

import com.game.common.response.ApiResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.stream.Collectors;

/**
 * 全局异常处理：把异常统一转成 ApiResult，避免直接返回默认错误页。
 * 继承 ResponseEntityExceptionHandler，保留 404 / 405 等框架级异常原有的 HTTP 状态码。
 *
 * <p>错误分层：400 参数/请求错误、401 未登录、403 无权限、404 资源不存在、500 服务端异常。
 * 500 只返回统一提示，内部异常详情只写日志，不返回给调用方。</p>
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 业务异常：按异常自带的错误码作为 HTTP 状态码返回。
     * 业务失败的日志由业务层按场景记录（带 userId / orderNo / username 等上下文），
     * 这里只留 debug，避免同一次失败在两个地方重复打印。
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResult<Void>> handleBusinessException(BusinessException e) {
        if (log.isDebugEnabled()) {
            log.debug("业务异常: code={}, message={}", e.getCode(), e.getMessage());
        }
        HttpStatus status = HttpStatus.resolve(e.getCode());
        return ResponseEntity.status(status == null ? HttpStatus.BAD_REQUEST : status)
                .body(ApiResult.fail(e.getCode(), e.getMessage()));
    }

    /** 兜底异常：记录堆栈，对外只返回统一提示 */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResult<Void>> handleException(Exception e) {
        log.error("系统异常", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResult.fail(HttpStatus.INTERNAL_SERVER_ERROR.value(), "系统异常，请稍后再试"));
    }

    /** 请求体参数校验失败（@Valid）：返回具体是哪个字段不合法 */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode statusCode, WebRequest request) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getDefaultMessage())
                .filter(StringUtils::hasText)
                .distinct()
                .collect(Collectors.joining("；"));
        log.warn("请求体参数校验失败: {}", message);
        return badRequest(message);
    }

    /** 查询参数校验失败（@RequestParam 上的 @Min / @DecimalMin 等） */
    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(
            HandlerMethodValidationException ex, HttpHeaders headers, HttpStatusCode statusCode, WebRequest request) {
        String message = ex.getAllValidationResults().stream()
                .flatMap(result -> result.getResolvableErrors().stream())
                .map(MessageSourceResolvable::getDefaultMessage)
                .filter(StringUtils::hasText)
                .distinct()
                .collect(Collectors.joining("；"));
        log.warn("请求参数校验失败: {}", message);
        return badRequest(message);
    }

    /** 请求体读不出来（JSON 格式错误等）：只说格式不对，不回显任何请求内容 */
    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex, HttpHeaders headers, HttpStatusCode statusCode, WebRequest request) {
        log.warn("请求体解析失败（不打印请求内容，避免泄漏密码等信息）");
        return badRequest("请求体格式不正确");
    }

    /**
     * 框架级异常（404 / 405 / 415 等）统一响应体：状态码保持不变，
     * 提示语换成固定文案，不把框架内部信息（异常 message、类名）返回给调用方。
     */
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception ex, Object body, HttpHeaders headers, HttpStatusCode statusCode, WebRequest request) {
        if (statusCode.is5xxServerError()) {
            log.error("请求处理异常, status={}", statusCode.value(), ex);
        } else {
            log.warn("请求处理失败, status={}, exception={}", statusCode.value(), ex.getClass().getSimpleName());
        }
        return new ResponseEntity<>(ApiResult.fail(statusCode.value(), friendlyMessage(ex, statusCode)), headers, statusCode);
    }

    private static ResponseEntity<Object> badRequest(String message) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResult.fail(HttpStatus.BAD_REQUEST.value(),
                        StringUtils.hasText(message) ? message : "请求参数不合法"));
    }

    /** 按异常类型 / 状态码给一句用户能看懂的话，不暴露内部实现细节 */
    private static String friendlyMessage(Exception ex, HttpStatusCode statusCode) {
        if (ex instanceof MethodArgumentTypeMismatchException || ex instanceof BindException
                || ex instanceof HandlerMethodValidationException || ex instanceof MethodArgumentNotValidException) {
            return "请求参数不合法";
        }
        if (ex instanceof MissingServletRequestParameterException) {
            return "缺少必要的请求参数";
        }
        if (ex instanceof HttpMessageNotReadableException) {
            return "请求体格式不正确";
        }
        if (ex instanceof HttpRequestMethodNotSupportedException) {
            return "请求方法不支持";
        }
        if (ex instanceof HttpMediaTypeNotSupportedException) {
            return "不支持的请求内容类型";
        }
        if (statusCode.is5xxServerError()) {
            return "系统异常，请稍后再试";
        }
        if (statusCode.value() == HttpStatus.NOT_FOUND.value()) {
            return "请求的资源不存在";
        }
        return statusCode.value() == HttpStatus.BAD_REQUEST.value() ? "请求参数不正确" : "请求无法处理";
    }
}
