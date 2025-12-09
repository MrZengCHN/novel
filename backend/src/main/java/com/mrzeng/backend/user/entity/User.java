package com.mrzeng.backend.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_user")
public class User {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String username;
    private String password;
    private String role; // ADMIN, VIP, USER
    private byte[] avatar;
    private String signature;
    private String tags; // Comma separated tags
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
