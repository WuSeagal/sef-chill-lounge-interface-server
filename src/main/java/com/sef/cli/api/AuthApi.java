package com.sef.cli.api;

import com.sef.cli.api.response.AuthResponse;
import com.sef.cli.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;
import java.util.Map;

@Tag(name = "權限管理API", description = "")
public interface AuthApi {

    @Operation(summary = "檢查登入狀態", description = "")
    @GetMapping(value = "/check-auth")
    ResponseEntity<ApiResponse<AuthResponse>> checkUser();

    @Operation(summary = "強制登出(刪cookie)", description = "")
    @PostMapping(value = "/logout")
    void logOutAuth(HttpServletRequest request, HttpServletResponse response);

    @Operation(summary = "取得使用者清單", description = "")
    @GetMapping(value = "/get-users")
    @PreAuthorize("hasAnyRole('ADMIN','ROOT')")
    ResponseEntity<ApiResponse<List<AuthResponse>>> getUsers();

    @Operation(summary = "GoogleAuth", description = "")
    @PostMapping(value = "/user/googleAuth")
    ResponseEntity<ApiResponse<String>> googleAuthLogin(HttpServletRequest request, @RequestBody Map<String, String> body);
}
