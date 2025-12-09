package com.mrzeng.backend.game.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_game")
public class Game {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private byte[] cover;
    private String description;
    private String tags;
    private String chatroomLink;
    private String lobbyLink;
    private Integer status; // 1: Active, 0: Inactive
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
