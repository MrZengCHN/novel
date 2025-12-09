package com.mrzeng.backend.game.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class GameVO {
    private Long id;
    private String name;
    private String cover; // Base64 string
    private String description;
    private String tags;
    private String chatroomLink;
    private String lobbyLink;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
