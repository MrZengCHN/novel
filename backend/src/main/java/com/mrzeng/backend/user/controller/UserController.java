package com.mrzeng.backend.user.controller;

import com.mrzeng.backend.common.Result;
import com.mrzeng.backend.user.dto.LoginDTO;
import com.mrzeng.backend.user.dto.RegisterDTO;
import com.mrzeng.backend.user.dto.UserVO;
import com.mrzeng.backend.user.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user")
@CrossOrigin // Allow frontend access
public class UserController {

    @Autowired
    private UserService userService;

    @PostMapping("/login")
    public Result<UserVO> login(@RequestBody LoginDTO loginDTO) {
        try {
            UserVO userVO = userService.login(loginDTO);
            return Result.success(userVO);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/register")
    public Result<UserVO> register(@RequestBody RegisterDTO registerDTO) {
        try {
            UserVO userVO = userService.register(registerDTO);
            return Result.success(userVO);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PutMapping("/update")
    public Result<UserVO> updateProfile(@RequestParam Long userId,
                                      @RequestParam(required = false) String avatar,
                                      @RequestParam(required = false) String signature) {
        try {
            UserVO userVO = userService.updateProfile(userId, avatar, signature);
            return Result.success(userVO);
        } catch (Exception e) {
             return Result.error(e.getMessage());
        }
    }
    
    // For admin use (simplified)
    @PostMapping("/role")
    public Result<UserVO> setRole(@RequestParam Long userId,
                                @RequestParam String role,
                                @RequestParam(required = false) String tags) {
         try {
            UserVO userVO = userService.setUserRole(userId, role, tags);
            return Result.success(userVO);
        } catch (Exception e) {
             return Result.error(e.getMessage());
        }
    }
    
    @GetMapping("/info")
    public Result<UserVO> getUserInfo(@RequestParam Long userId) {
        return Result.success(userService.getUserInfo(userId));
    }
}
