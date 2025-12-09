package com.mrzeng.backend.game.controller;

import cn.hutool.core.codec.Base64;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mrzeng.backend.common.Result;
import com.mrzeng.backend.game.dto.GameVO;
import com.mrzeng.backend.game.entity.Game;
import com.mrzeng.backend.game.service.GameService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/game")
public class GameController {

    @Autowired
    private GameService gameService;

    @PostMapping("/add")
    public Result<String> addGame(
            @RequestParam("name") String name,
            @RequestParam(value = "cover", required = false) MultipartFile cover,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "tags", required = false) String tags,
            @RequestParam(value = "chatroomLink", required = false) String chatroomLink,
            @RequestParam(value = "lobbyLink", required = false) String lobbyLink
    ) throws IOException {
        gameService.addGame(name, cover, description, tags, chatroomLink, lobbyLink);
        return Result.success("添加成功");
    }

    @PutMapping("/update")
    public Result<String> updateGame(
            @RequestParam("id") Long id,
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "cover", required = false) MultipartFile cover,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "tags", required = false) String tags,
            @RequestParam(value = "chatroomLink", required = false) String chatroomLink,
            @RequestParam(value = "lobbyLink", required = false) String lobbyLink,
            @RequestParam(value = "status", required = false) Integer status
    ) throws IOException {
        gameService.updateGame(id, name, cover, description, tags, chatroomLink, lobbyLink, status);
        return Result.success("更新成功");
    }

    @DeleteMapping("/{id}")
    public Result<String> deleteGame(@PathVariable Long id) {
        gameService.removeById(id);
        return Result.success("删除成功");
    }

    @GetMapping("/list")
    public Result<List<GameVO>> listGames() {
        // List only active games or all? Admin might want all.
        // For simple home page impl, let's list all or active.
        // Assuming this is for Home Page display mostly, maybe filter by status=1?
        // But user said "Management Functionality".
        // Let's return all for now, frontend can filter. Or just active for now.
        // Let's return all so admin can manage.
        List<Game> list = gameService.list();
        List<GameVO> vos = list.stream().map(this::convertToVO).collect(Collectors.toList());
        return Result.success(vos);
    }

    @GetMapping("/{id}")
    public Result<GameVO> getGame(@PathVariable Long id) {
        Game game = gameService.getById(id);
        if (game == null) return Result.error("游戏不存在");
        return Result.success(convertToVO(game));
    }

    private GameVO convertToVO(Game game) {
        GameVO vo = new GameVO();
        BeanUtils.copyProperties(game, vo, "cover");
        if (game.getCover() != null) {
            String base64 = Base64.encode(game.getCover());
            vo.setCover("data:image/jpeg;base64," + base64); // Assuming jpeg/png, prefix generic or specific?
            // "data:image/png;base64," was used in UserServiceImpl.
            // Let's stick to a generic logic or just hardcode provided simple constraint usage.
            // But verify UserServiceImpl used "data:image/png;base64,".
            // The file upload stores raw bytes.
            // Let's use image/png for consistency or maybe try to detect?
            // UserServiceImpl hardcoded "data:image/png;base64,". I will do the same or jpeg.
            // Ideally should store mime type. For now, hardcode "data:image/png;base64," (most compatible/common assumption in this project context if generic).
            // Actually let's use a method that works for both usually if browser handles it, or just png.
        }
        return vo;
    }
}
