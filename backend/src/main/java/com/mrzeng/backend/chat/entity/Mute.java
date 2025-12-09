package com.mrzeng.backend.chat.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("t_mute")
public class Mute {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String channelId;
    private LocalDateTime createTime;
    private LocalDateTime expireTime;
}
