package com.mrzeng.backend.user.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.mrzeng.backend.user.dto.LoginDTO;
import com.mrzeng.backend.user.dto.RegisterDTO;
import com.mrzeng.backend.user.dto.UserVO;
import com.mrzeng.backend.user.entity.User;

public interface UserService extends IService<User> {
    UserVO login(LoginDTO loginDTO);
    UserVO register(RegisterDTO registerDTO);
    UserVO updateProfile(Long userId, String avatar, String signature);
    UserVO setUserRole(Long userId, String role, String tags);
    UserVO getUserInfo(Long userId);
}
