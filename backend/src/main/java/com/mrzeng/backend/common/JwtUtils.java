package com.mrzeng.backend.common;

import cn.hutool.jwt.JWT;
import cn.hutool.jwt.JWTUtil;
import cn.hutool.jwt.signers.JWTSignerUtil;

import java.util.HashMap;
import java.util.Map;

public class JwtUtils {

    // Should be in config, hardcoded for simplicity as per request context
    private static final byte[] KEY = "MrZengNovelProjectSecretKey".getBytes();

    public static String createToken(Long userId, String username) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("userId", userId);
        payload.put("username", username);
        payload.put("expire_time", System.currentTimeMillis() + 1000 * 60 * 60 * 24); // 24 hours
        
        return JWTUtil.createToken(payload, JWTSignerUtil.hs256(KEY));
    }

    public static boolean validateToken(String token) {
        try {
            return JWTUtil.verify(token, JWTSignerUtil.hs256(KEY));
        } catch (Exception e) {
            return false;
        }
    }

    public static Long getUserId(String token) {
        final JWT jwt = JWTUtil.parseToken(token);
        return Long.valueOf(jwt.getPayload("userId").toString());
    }
}
