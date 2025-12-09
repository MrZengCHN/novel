package com.mrzeng.backend.common;

import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.sql.SQLException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    public Result<String> handleException(Exception e) {
        e.printStackTrace(); // Print stack trace to console for debugging
        return Result.error(e.getMessage());
    }
    
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public Result<String> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException e) {
        return Result.error("文件大小超过限制");
    }

    @ExceptionHandler(SQLException.class)
    public Result<String> handleSQLException(SQLException e) {
        e.printStackTrace();
        return Result.error("数据库操作异常");
    }
}
