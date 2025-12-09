package com.mrzeng.backend.game.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mrzeng.backend.game.entity.Game;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface GameMapper extends BaseMapper<Game> {
}
