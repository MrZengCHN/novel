package com.mrzeng.backend.chat.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mrzeng.backend.chat.entity.Mute;
import com.mrzeng.backend.chat.mapper.MuteMapper;
import com.mrzeng.backend.common.Result;
import com.mrzeng.backend.user.entity.User;
import com.mrzeng.backend.user.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/chat")
public class ChatController {

    @Autowired
    private MuteMapper muteMapper;

    @Autowired
    private UserService userService;
    
    @Autowired
    private com.mrzeng.backend.chat.handler.ChatWebSocketHandler chatWebSocketHandler;

    @PostMapping("/mute")
    public Result<String> muteUser(@RequestParam Long userId, @RequestParam String channelId, @RequestParam(required = false, defaultValue = "10") Integer durationMinutes) {
        // Validation: Target user exist?
        User targetUser = userService.getById(userId);
        if (targetUser == null) {
            return Result.error("用户不存在");
        }

        // Rule: Cannot mute Admin
        if ("ADMIN".equals(targetUser.getRole())) {
            return Result.error("无法禁言管理员");
        }
        
        // Rule: Duration limit 24h
        if (durationMinutes > 1440) {
            return Result.error("禁言时间不能超过24小时");
        }
        if (durationMinutes <= 0) {
            return Result.error("禁言时间必须大于0");
        }

        // Cleanup expired records
        muteMapper.delete(new LambdaQueryWrapper<Mute>()
                 .eq(Mute::getUserId, userId)
                 .eq(Mute::getChannelId, channelId)
                 .lt(Mute::getExpireTime, LocalDateTime.now()));

        // Check if still muted (active mute)
        Long count = muteMapper.selectCount(new LambdaQueryWrapper<Mute>()
                .eq(Mute::getUserId, userId)
                .eq(Mute::getChannelId, channelId));
        
        if (count > 0) {
            return Result.error("该用户已被禁言");
        }

        Mute mute = new Mute();
        mute.setUserId(userId);
        mute.setChannelId(channelId);
        mute.setCreateTime(LocalDateTime.now());
        mute.setExpireTime(LocalDateTime.now().plusMinutes(durationMinutes));
        muteMapper.insert(mute);
        
        // Notify via WS
        chatWebSocketHandler.notifyUserMuteStatus(userId, channelId, true, mute.getExpireTime());

        return Result.success("禁言成功");
    }
    
    @PostMapping("/unmute")
    public Result<String> unmuteUser(@RequestParam Long userId, @RequestParam String channelId) {
        muteMapper.delete(new LambdaQueryWrapper<Mute>()
                .eq(Mute::getUserId, userId)
                .eq(Mute::getChannelId, channelId));
                
        // Notify via WS
        chatWebSocketHandler.notifyUserMuteStatus(userId, channelId, false, null);
        
        return Result.success("解除禁言成功");
    }
}
