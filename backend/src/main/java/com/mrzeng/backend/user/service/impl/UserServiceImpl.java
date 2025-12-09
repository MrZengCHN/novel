package com.mrzeng.backend.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mrzeng.backend.user.dto.LoginDTO;
import com.mrzeng.backend.user.dto.RegisterDTO;
import com.mrzeng.backend.user.dto.UserVO;
import com.mrzeng.backend.user.entity.User;
import com.mrzeng.backend.user.mapper.UserMapper;
import com.mrzeng.backend.user.service.UserService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

import cn.hutool.crypto.digest.BCrypt;
import com.mrzeng.backend.common.JwtUtils;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Override
    public UserVO login(LoginDTO loginDTO) {
        User user = this.getOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, loginDTO.getUsername()));
        
        if (user == null || !BCrypt.checkpw(loginDTO.getPassword(), user.getPassword())) {
            throw new RuntimeException("用户名或密码错误");
        }
        UserVO userVO = convertToVO(user);
        userVO.setToken(JwtUtils.createToken(user.getId(), user.getUsername()));
        return userVO;
    }

    @Override
    public UserVO register(RegisterDTO registerDTO) {
        long count = this.count(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, registerDTO.getUsername()));
        if (count > 0) {
            throw new RuntimeException("用户名已存在");
        }

        User user = new User();
        user.setUsername(registerDTO.getUsername());
        user.setPassword(BCrypt.hashpw(registerDTO.getPassword()));
        user.setRole("USER"); // Default role
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        
        this.save(user);
        UserVO userVO = convertToVO(user);
        userVO.setToken(JwtUtils.createToken(user.getId(), user.getUsername()));
        return userVO;
    }

    @Override
    public UserVO updateProfile(Long userId, String avatar, String signature) {
        User user = this.getById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        
        if (avatar != null) user.setAvatar(avatar);
        if (signature != null) user.setSignature(signature);
        user.setUpdateTime(LocalDateTime.now());
        
        this.updateById(user);
        return convertToVO(user);
    }

    @Override
    public UserVO setUserRole(Long userId, String role, String tags) {
        User user = this.getById(userId);
         if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        user.setRole(role);
        user.setTags(tags);
        user.setUpdateTime(LocalDateTime.now());
        this.updateById(user);
        return convertToVO(user);
    }
    
    @Override
    public UserVO getUserInfo(Long userId) {
        User user = this.getById(userId);
        if (user == null) return null;
        return convertToVO(user);
    }

    private UserVO convertToVO(User user) {
        UserVO vo = new UserVO();
        BeanUtils.copyProperties(user, vo);
        return vo;
    }
}
