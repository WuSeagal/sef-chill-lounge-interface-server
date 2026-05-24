package com.sef.cli.common.web;

import com.sef.cli.common.ApiResponse;
import com.sef.cli.common.exception.ForbiddenException;
import com.sef.cli.common.exception.InvalidTopicIdException;
import com.sef.cli.common.exception.NoOtherTopicAvailableException;
import com.sef.cli.common.exception.NoTopicAvailableException;
import com.sef.cli.common.exception.ProfileAlreadyExistsException;
import com.sef.cli.common.exception.ProfileNotFoundException;
import com.sef.cli.common.exception.SocialLinkNotFoundException;
import com.sef.cli.common.exception.TagAlreadyAssociatedException;
import com.sef.cli.common.exception.TagJunctionNotFoundException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {

    private ResponseEntity<ApiResponse<Object>> respond(HttpStatus status, String code) {
        return ResponseEntity.status(status).body(ApiResponse.fail(status.value(), code));
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

    @ExceptionHandler(TagJunctionNotFoundException.class)
    public ResponseEntity<ApiResponse<Object>> tagJunctionNotFound(TagJunctionNotFoundException e) {
        return respond(HttpStatus.NOT_FOUND, "tag_junction_not_found");
    }

    @ExceptionHandler(SocialLinkNotFoundException.class)
    public ResponseEntity<ApiResponse<Object>> socialLinkNotFound(SocialLinkNotFoundException e) {
        return respond(HttpStatus.NOT_FOUND, "social_link_not_found");
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
    public ResponseEntity<ApiResponse<Object>> illegalArgument(IllegalArgumentException e) {
        return respond(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Object>> dataIntegrity(DataIntegrityViolationException e) {
        // race-condition layer-2 防禦：UNIQUE constraint 撞到
        return respond(HttpStatus.CONFLICT, "constraint_violation");
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<Object>> unauthenticated(AuthenticationException e) {
        return respond(HttpStatus.UNAUTHORIZED, "unauthenticated");
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Object>> accessDenied(AccessDeniedException e) {
        return respond(HttpStatus.FORBIDDEN, "forbidden");
    }
}
