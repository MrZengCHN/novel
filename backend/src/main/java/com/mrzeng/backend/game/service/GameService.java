package com.mrzeng.backend.game.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.mrzeng.backend.game.entity.Game;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;

public interface GameService extends IService<Game> {
    void addGame(String name, MultipartFile cover, String description, String tags, String chatroomLink, String lobbyLink) throws IOException;
    void updateGame(Long id, String name, MultipartFile cover, String description, String tags, String chatroomLink, String lobbyLink, Integer status) throws IOException;
}
