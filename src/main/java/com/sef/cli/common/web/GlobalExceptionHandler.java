package com.sef.cli.common.web;

import com.sef.cli.common.ApiResponse;
import com.sef.cli.common.exception.ForbiddenException;
import com.sef.cli.common.exception.InvalidPlatformException;
import com.sef.cli.common.exception.InvalidSocialUrlException;
import com.sef.cli.common.exception.InvalidTopicIdException;
import com.sef.cli.common.exception.NoOtherTopicAvailableException;
import com.sef.cli.common.exception.NoTopicAvailableException;
import com.sef.cli.common.exception.ProfileAlreadyExistsException;
import com.sef.cli.common.exception.ProfileNotFoundException;
import com.sef.cli.common.exception.SocialLinkNotFoundException;
import com.sef.cli.common.exception.TagAlreadyAssociatedException;
import com.sef.cli.common.exception.TagJunctionNotFoundException;
import com.sef.cli.common.exception.TagLimitExceededException;
import com.sef.cli.image.web.exception.PayloadTooLargeException;
import com.sef.cli.image.web.exception.UnsupportedMediaTypeException;

import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final String GENERIC_500_MESSAGE = "系統暫時無法處理您的請求，請稍後再試";
    private static final String GENERIC_404_MESSAGE = "找不到資源";

    private final com.sef.cli.common.web.error.CustomErrorPageRenderer errorPageRenderer;

    public GlobalExceptionHandler(com.sef.cli.common.web.error.CustomErrorPageRenderer errorPageRenderer) {
        this.errorPageRenderer = errorPageRenderer;
    }

    private static String newTraceId() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    private boolean wantsHtml(HttpServletRequest req) {
        String accept = req.getHeader("Accept");
        return accept != null && accept.contains(org.springframework.http.MediaType.TEXT_HTML_VALUE);
    }

    private ResponseEntity<ApiResponse<Object>> respond(HttpStatus status, String code) {
        return ResponseEntity.status(status).body(ApiResponse.fail(status.value(), code));
    }

    private ResponseEntity<?> respondAcceptAware(HttpStatus status, String jsonMessage, HttpServletRequest req) {
        if (wantsHtml(req)) {
            return ResponseEntity.status(status)
                    .contentType(new MediaType("text", "html", StandardCharsets.UTF_8))
                    .body(errorPageRenderer.render(status.value(), req.getRequestURI(), req.getContextPath()));
        }
        return ResponseEntity.status(status)
                .body(ApiResponse.fail(status.value(), jsonMessage));
    }

    @ExceptionHandler(ProfileNotFoundException.class)
    public ResponseEntity<ApiResponse<Object>> profileNotFound(ProfileNotFoundException e) {
        return respond(HttpStatus.NOT_FOUND, "profile_not_found");
    }

    @ExceptionHandler(ProfileAlreadyExistsException.class)
    public ResponseEntity<ApiResponse<Object>> profileAlreadyExists(ProfileAlreadyExistsException e) {
        return respond(HttpStatus.CONFLICT, "profile_already_exists");
    }

    @ExceptionHandler(TagAlreadyAssociatedException.class)
    public ResponseEntity<ApiResponse<Object>> tagAlreadyAssociated(TagAlreadyAssociatedException e) {
        return respond(HttpStatus.CONFLICT, "tag_already_associated");
    }

    @ExceptionHandler(TagLimitExceededException.class)
    public ResponseEntity<ApiResponse<Object>> tagLimitExceeded(TagLimitExceededException e) {
        return respond(HttpStatus.BAD_REQUEST, "tag_limit_exceeded");
    }

    @ExceptionHandler(TagJunctionNotFoundException.class)
    public ResponseEntity<ApiResponse<Object>> tagJunctionNotFound(TagJunctionNotFoundException e) {
        return respond(HttpStatus.NOT_FOUND, "tag_junction_not_found");
    }

    @ExceptionHandler(SocialLinkNotFoundException.class)
    public ResponseEntity<ApiResponse<Object>> socialLinkNotFound(SocialLinkNotFoundException e) {
        return respond(HttpStatus.NOT_FOUND, "social_link_not_found");
    }

    @ExceptionHandler(InvalidPlatformException.class)
    public ResponseEntity<ApiResponse<Object>> invalidPlatform(InvalidPlatformException e) {
        return respond(HttpStatus.BAD_REQUEST, "invalid_platform");
    }

    @ExceptionHandler(InvalidSocialUrlException.class)
    public ResponseEntity<ApiResponse<Object>> invalidSocialUrl(InvalidSocialUrlException e) {
        return respond(HttpStatus.BAD_REQUEST, e.getErrorCode());
    }

    @ExceptionHandler(NoTopicAvailableException.class)
    public ResponseEntity<ApiResponse<Object>> noTopicAvailable(NoTopicAvailableException e) {
        return respond(HttpStatus.NOT_FOUND, "no_topic_available");
    }

    @ExceptionHandler(NoOtherTopicAvailableException.class)
    public ResponseEntity<ApiResponse<Object>> noOtherTopic(NoOtherTopicAvailableException e) {
        return respond(HttpStatus.CONFLICT, "no_other_topic_available");
    }

    @ExceptionHandler(InvalidTopicIdException.class)
    public ResponseEntity<ApiResponse<Object>> invalidTopicId(InvalidTopicIdException e) {
        return respond(HttpStatus.BAD_REQUEST, "invalid_topic_id");
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ApiResponse<Object>> forbidden(ForbiddenException e) {
        return respond(HttpStatus.FORBIDDEN, "forbidden");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<?> illegalArgument(IllegalArgumentException e, HttpServletRequest req) {
        return respondAcceptAware(HttpStatus.BAD_REQUEST, e.getMessage(), req);
    }

    @ExceptionHandler(org.springframework.web.bind.MethodArgumentNotValidException.class)
    public ResponseEntity<?> methodArgumentNotValid(
            org.springframework.web.bind.MethodArgumentNotValidException e,
            HttpServletRequest req) {
        String summary = e.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + " " + fe.getDefaultMessage())
                .reduce((a, b) -> a + "; " + b)
                .orElse("validation_failed");
        return respondAcceptAware(HttpStatus.BAD_REQUEST, summary, req);
    }

    @ExceptionHandler(jakarta.validation.ConstraintViolationException.class)
    public ResponseEntity<?> constraintViolation(
            jakarta.validation.ConstraintViolationException e,
            HttpServletRequest req) {
        String summary = e.getConstraintViolations().stream()
                .map(cv -> cv.getPropertyPath() + " " + cv.getMessage())
                .reduce((a, b) -> a + "; " + b)
                .orElse("constraint_violation");
        return respondAcceptAware(HttpStatus.BAD_REQUEST, summary, req);
    }

    @ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException.class)
    public ResponseEntity<?> messageNotReadable(
            org.springframework.http.converter.HttpMessageNotReadableException e,
            HttpServletRequest req) {
        return respondAcceptAware(HttpStatus.BAD_REQUEST, "malformed_request_body", req);
    }

    @ExceptionHandler(org.springframework.web.bind.MissingServletRequestParameterException.class)
    public ResponseEntity<?> missingRequestParameter(
            org.springframework.web.bind.MissingServletRequestParameterException e,
            HttpServletRequest req) {
        return respondAcceptAware(HttpStatus.BAD_REQUEST, e.getMessage(), req);
    }

    @ExceptionHandler(org.springframework.web.HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<?> methodNotSupported(
            org.springframework.web.HttpRequestMethodNotSupportedException e,
            HttpServletRequest req) {
        return respondAcceptAware(HttpStatus.METHOD_NOT_ALLOWED, e.getMessage(), req);
    }

    @ExceptionHandler(org.springframework.web.HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<?> mediaTypeNotSupported(
            org.springframework.web.HttpMediaTypeNotSupportedException e,
            HttpServletRequest req) {
        return respondAcceptAware(HttpStatus.UNSUPPORTED_MEDIA_TYPE, e.getMessage(), req);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Object>> dataIntegrity(DataIntegrityViolationException e) {
        // race-condition layer-2 防禦：UNIQUE constraint 撞到
        return respond(HttpStatus.CONFLICT, "constraint_violation");
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<?> unauthenticated(AuthenticationException e, HttpServletRequest req) {
        return respondAcceptAware(HttpStatus.UNAUTHORIZED, "unauthenticated", req);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<?> accessDenied(AccessDeniedException e, HttpServletRequest req) {
        return respondAcceptAware(HttpStatus.FORBIDDEN, "forbidden", req);
    }

    @ExceptionHandler(PayloadTooLargeException.class)
    public ResponseEntity<ApiResponse<Object>> payloadTooLarge(PayloadTooLargeException e) {
        log.warn("413 payload too large: {} (maxSizeMB={})", e.getMessage(), e.getMaxSizeMb());
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(new ApiResponse<>(
                HttpStatus.PAYLOAD_TOO_LARGE.value(),
                e.getMessage(),
                Map.of("maxSizeMB", e.getMaxSizeMb())
        ));
    }

    @ExceptionHandler(UnsupportedMediaTypeException.class)
    public ResponseEntity<ApiResponse<Object>> unsupportedMedia(UnsupportedMediaTypeException e) {
        log.warn("415 unsupported media type: {}", e.getMessage());
        return respond(HttpStatus.UNSUPPORTED_MEDIA_TYPE, e.getMessage());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<?> noResourceFound(NoResourceFoundException e, HttpServletRequest req) {
        String traceId = newTraceId();
        log.info("[{}] 404 not found path={}", traceId, req.getRequestURI());
        if (wantsHtml(req)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .contentType(new org.springframework.http.MediaType("text", "html", java.nio.charset.StandardCharsets.UTF_8))
                    .body(errorPageRenderer.render(404, req.getRequestURI(), req.getContextPath()));
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.failWithTrace(HttpStatus.NOT_FOUND.value(), GENERIC_404_MESSAGE, traceId));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> unhandled(Exception e, HttpServletRequest req) {
        String traceId = newTraceId();
        log.error("[{}] 500 unhandled path={}", traceId, req.getRequestURI(), e);
        if (wantsHtml(req)) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .contentType(new org.springframework.http.MediaType("text", "html", java.nio.charset.StandardCharsets.UTF_8))
                    .body(errorPageRenderer.render(500, req.getRequestURI(), req.getContextPath()));
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.failWithTrace(HttpStatus.INTERNAL_SERVER_ERROR.value(), GENERIC_500_MESSAGE, traceId));
    }
}
