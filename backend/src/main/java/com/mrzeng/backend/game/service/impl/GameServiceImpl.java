package com.mrzeng.backend.game.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mrzeng.backend.game.entity.Game;
import com.mrzeng.backend.game.mapper.GameMapper;
import com.mrzeng.backend.game.service.GameService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;

@Service
public class GameServiceImpl extends ServiceImpl<GameMapper, Game> implements GameService {

    @Override
    public void addGame(String name, MultipartFile cover, String description, String tags, String chatroomLink, String lobbyLink) throws IOException {
        Game game = new Game();
        game.setName(name);
        if (cover != null && !cover.isEmpty()) {
            game.setCover(cover.getBytes());
        }
        game.setDescription(description);
        game.setTags(tags);
        game.setChatroomLink(chatroomLink);
        game.setLobbyLink(lobbyLink);
        game.setStatus(1); // Default active
        game.setCreateTime(LocalDateTime.now());
        game.setUpdateTime(LocalDateTime.now());
        this.save(game);
    }

    @Override
    public void updateGame(Long id, String name, MultipartFile cover, String description, String tags, String chatroomLink, String lobbyLink, Integer status) throws IOException {
        Game game = this.getById(id);
        if (game == null) {
            throw new RuntimeException("游戏不存在");
        }
        if (name != null) game.setName(name);
        if (cover != null && !cover.isEmpty()) {
            game.setCover(cover.getBytes());
        }
        if (description != null) game.setDescription(description);
        if (tags != null) game.setTags(tags);
        if (chatroomLink != null) game.setChatroomLink(chatroomLink);
        if (lobbyLink != null) game.setLobbyLink(lobbyLink);
        if (status != null) game.setStatus(status);
        
        game.setUpdateTime(LocalDateTime.now());
        this.updateById(game);
    }
}
