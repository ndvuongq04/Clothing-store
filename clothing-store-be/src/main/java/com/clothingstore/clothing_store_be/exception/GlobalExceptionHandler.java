package com.clothingstore.clothing_store_be.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import com.clothingstore.clothing_store_be.dto.response.RestResponse;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

        @ExceptionHandler(MethodArgumentNotValidException.class)
        public ResponseEntity<RestResponse<Object>> handleValidation(MethodArgumentNotValidException ex) {
                var errors = ex.getBindingResult().getFieldErrors().stream()
                                .collect(Collectors.toMap(
                                                e -> e.getField(),
                                                e -> e.getDefaultMessage() != null ? e.getDefaultMessage()
                                                                : "Invalid"));

                RestResponse<Object> res = new RestResponse<>();
                res.setStatusCode(HttpStatus.BAD_REQUEST.value());
                res.setError("Validation failed");
                res.setMessage(errors);

                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(res);
        }

        @ExceptionHandler(AppException.class)
        public ResponseEntity<RestResponse<Object>> handleAppException(AppException ex) {
                RestResponse<Object> res = new RestResponse<>();
                res.setStatusCode(ex.getStatus().value());
                res.setError(ex.getStatus().getReasonPhrase());
                res.setMessage(ex.getMessage());

                return ResponseEntity.status(ex.getStatus()).body(res);
        }

        @ExceptionHandler(BadCredentialsException.class)
        public ResponseEntity<RestResponse<Object>> handleBadCredentials(BadCredentialsException ex) {
                RestResponse<Object> res = new RestResponse<>();
                res.setStatusCode(HttpStatus.UNAUTHORIZED.value());
                res.setError("Unauthorized");
                res.setMessage("Email hoac mat khau khong chinh xac");

                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(res);
        }

        @ExceptionHandler(DisabledException.class)
        public ResponseEntity<RestResponse<Object>> handleDisabled(DisabledException ex) {
                RestResponse<Object> res = new RestResponse<>();
                res.setStatusCode(HttpStatus.UNAUTHORIZED.value());
                res.setError("Unauthorized");
                res.setMessage("Tai khoan bi vo hieu hoa");

                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(res);
        }

        @ExceptionHandler(LockedException.class)
        public ResponseEntity<RestResponse<Object>> handleLocked(LockedException ex) {
                RestResponse<Object> res = new RestResponse<>();
                res.setStatusCode(HttpStatus.UNAUTHORIZED.value());
                res.setError("Unauthorized");
                res.setMessage("Tai khoan bi khoa");

                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(res);
        }

        // Bắt lỗi @PreAuthorize từ chối truy cập — trả 403
        @ExceptionHandler(AuthorizationDeniedException.class)
        public ResponseEntity<RestResponse<Object>> handleAuthorizationDenied(AuthorizationDeniedException ex) {
                RestResponse<Object> res = new RestResponse<>();
                res.setStatusCode(HttpStatus.FORBIDDEN.value());
                res.setError("Forbidden");
                res.setMessage("Bạn không có quyền thực hiện thao tác này");

                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(res);
        }

        @ExceptionHandler(MaxUploadSizeExceededException.class)
        public ResponseEntity<RestResponse<Object>> handleMaxUploadSizeExceededException(
                        MaxUploadSizeExceededException ex) {
                RestResponse<Object> res = new RestResponse<>();
                res.setStatusCode(HttpStatus.PAYLOAD_TOO_LARGE.value());
                res.setError("Payload Too Large");
                res.setMessage("Kích thước file tải lên vượt quá giới hạn cho phép (tối đa 10MB)");

                return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(res);
        }

        @ExceptionHandler(Exception.class)
        public ResponseEntity<RestResponse<Object>> handleException(Exception ex) {
                Throwable cause = ex;
                while (cause != null) {
                        if (cause instanceof AppException) {
                                return handleAppException((AppException) cause);
                        }
                        cause = cause.getCause();
                }

                ex.printStackTrace();
                RestResponse<Object> res = new RestResponse<>();
                res.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
                res.setError("Internal Server Error");
                res.setMessage("Lỗi hệ thống, vui lòng thử lại sau (" + ex.getMessage() + ")");

                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(res);
        }
}