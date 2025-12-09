package com.mrzeng.backend.user.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class UserVO {
    private Long id;
    private String username;
    private String role;
    private String avatar;
    private String signature;
    private String tags;
    private String token;
    private LocalDateTime createTime;
}
