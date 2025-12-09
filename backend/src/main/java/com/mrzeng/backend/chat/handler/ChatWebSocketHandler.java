package com.mrzeng.backend.chat.handler;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mrzeng.backend.chat.entity.Mute;
import com.mrzeng.backend.chat.mapper.MuteMapper;
import com.mrzeng.backend.common.JwtUtils;
import com.mrzeng.backend.user.entity.User;
import com.mrzeng.backend.user.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private MuteMapper muteMapper;

    @Autowired
    private org.springframework.data.redis.core.StringRedisTemplate redisTemplate;

    // Channel -> Set of Sessions
    private static final Map<String, Set<WebSocketSession>> channelSessions = new ConcurrentHashMap<>();
    // Session ID -> User Info (for quick access)
    private static final Map<String, User> sessionUsers = new ConcurrentHashMap<>();
    // Session ID -> Channel ID
    private static final Map<String, String> sessionChannels = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        // Parse Query Params: ?channel=eft&token=...
        URI uri = session.getUri();
        String query = uri.getQuery();
        Map<String, String> params = parseQuery(query);
        
        String token = params.get("token");
        String channel = params.get("channel");

        if (token == null || channel == null) {
            session.close(CloseStatus.BAD_DATA);
            return;
        }

        // Validate Token
        if (!JwtUtils.validateToken(token)) {
            session.close(CloseStatus.POLICY_VIOLATION);
            return;
        }

        Long userId = JwtUtils.getUserId(token);
        User user = userMapper.selectById(userId);
        if (user == null) {
            session.close(CloseStatus.POLICY_VIOLATION);
            return;
        }

        // Store Session Info
        sessionUsers.put(session.getId(), user);
        sessionChannels.put(session.getId(), channel);

        channelSessions.computeIfAbsent(channel, k -> new CopyOnWriteArraySet<>()).add(session);

        // Broadcast JOIN only if first session
        if (getUserSessionCount(channel, user.getId()) == 1) {
            broadcast(channel, "JOIN", user, null, null);
        }

        // Send Initial User List to current session
        sendInitialList(session, channel);
        
        // Send History
        sendHistory(session, channel);
        
        // Check and Send Mute Status
        checkAndSendMuteStatus(session, user, channel);
    }
    
    private void checkAndSendMuteStatus(WebSocketSession session, User user, String channel) throws IOException {
         Mute mute = muteMapper.selectOne(new LambdaQueryWrapper<Mute>()
                .eq(Mute::getUserId, user.getId())
                .eq(Mute::getChannelId, channel)
                .gt(Mute::getExpireTime, LocalDateTime.now())
                .last("LIMIT 1"));
         
         JSONObject msg = new JSONObject();
         msg.set("type", "MUTE_STATUS");
         msg.set("muted", mute != null);
         if (mute != null) {
             msg.set("expireTime", mute.getExpireTime().toString());
         }
         session.sendMessage(new TextMessage(msg.toString()));
    }
    
    public void notifyUserMuteStatus(Long userId, String channel, boolean muted, LocalDateTime expireTime) {
        // Find session for this user in this channel
        Set<WebSocketSession> sessions = channelSessions.get(channel);
        if (sessions == null) return;
        
        for (WebSocketSession s : sessions) {
             User u = sessionUsers.get(s.getId());
             if (u != null && u.getId().equals(userId)) {
                 try {
                     JSONObject msg = new JSONObject();
                     msg.set("type", "MUTE_STATUS");
                     msg.set("muted", muted);
                     if (muted && expireTime != null) {
                         msg.set("expireTime", expireTime.toString());
                     }
                     s.sendMessage(new TextMessage(msg.toString()));
                 } catch (IOException e) {
                     e.printStackTrace();
                 }
             }
        }
    }

    private void sendInitialList(WebSocketSession session, String channel) throws IOException {
        Set<WebSocketSession> sessions = channelSessions.get(channel);
        if (sessions == null) return;

        java.util.List<JSONObject> userList = new java.util.ArrayList<>();
        Set<Long> addedUserIds = new java.util.HashSet<>();
        
        for (WebSocketSession s : sessions) {
            User u = sessionUsers.get(s.getId());
            if (u != null && !addedUserIds.contains(u.getId())) {
                JSONObject userJson = new JSONObject();
                userJson.set("id", u.getId());
                userJson.set("username", u.getUsername());
                userJson.set("avatar", u.getAvatar() != null ? "data:image/png;base64," + cn.hutool.core.codec.Base64.encode(u.getAvatar()) : null);
                userJson.set("role", u.getRole());
                userList.add(userJson);
                addedUserIds.add(u.getId());
            }
        }

        JSONObject msg = new JSONObject();
        msg.set("type", "INITIAL_LIST");
        msg.set("users", userList);
        
        session.sendMessage(new TextMessage(msg.toString()));
    }

    private void sendHistory(WebSocketSession session, String channel) throws IOException {
        String key = "chat:history:" + channel;
        // Fetch last 50 messages
        java.util.List<String> history = redisTemplate.opsForList().range(key, -50, -1);
        if (history == null || history.isEmpty()) return;

        JSONObject msg = new JSONObject();
        msg.set("type", "HISTORY");
        msg.set("messages", history.stream().map(JSONUtil::parseObj).collect(java.util.stream.Collectors.toList()));
        
        session.sendMessage(new TextMessage(msg.toString()));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        User user = sessionUsers.get(session.getId());
        String channel = sessionChannels.get(session.getId());
        
        if (user == null || channel == null) return;

        // Check Mute
        Long muteCount = muteMapper.selectCount(new LambdaQueryWrapper<Mute>()
                .eq(Mute::getUserId, user.getId())
                .eq(Mute::getChannelId, channel)
                .gt(Mute::getExpireTime, LocalDateTime.now()));
        
        if (muteCount > 0) {
            // Send system message only to this user
            JSONObject msg = new JSONObject();
            msg.set("type", "ERROR");
            msg.set("content", "您已被禁言");
            session.sendMessage(new TextMessage(msg.toString()));
            return;
        }

        // Try to parse as JSON first (for commands like RECALL)
        JSONObject payloadJson = null;
        try {
            if (JSONUtil.isTypeJSON(message.getPayload())) {
                payloadJson = JSONUtil.parseObj(message.getPayload());
            }
        } catch (Exception e) {
            // Not JSON, treat as string
        }

        if (payloadJson != null && "RECALL".equals(payloadJson.getStr("type"))) {
            handleRecallMessage(channel, user, payloadJson.getStr("messageId"));
            return;
        }

        // Broadcast Message
        String content = message.getPayload();
        // If it was a JSON payload but not RECALL/known command, treat likely as content? 
        // Or if client sends JSON for future CHAT message structure.
        // For now assume client sends plain text for CHAT, or if JSON check 'content' field?
        // Let's keep specific logic: if 'type' is missing, it's CHAT content.
        // NOTE: Previous frontend sends raw string.
        
        String messageId = java.util.UUID.randomUUID().toString();
        broadcast(channel, "CHAT", user, content, messageId);
        
        // Save to Redis
        saveToHistory(channel, user, content, messageId);
    }
    
    private void handleRecallMessage(String channel, User user, String messageId) {
        if (messageId == null) return;
        
        String key = "chat:history:" + channel;
        // Scan recent messages
        java.util.List<String> history = redisTemplate.opsForList().range(key, -200, -1);
        if (history == null) return;
        
        for (String msgStr : history) {
            JSONObject msg = JSONUtil.parseObj(msgStr);
            if (messageId.equals(msg.getStr("messageId"))) {
                // Check Permission
                boolean isSender = user.getId().toString().equals(msg.getStr("userId"));
                boolean isAdmin = "ADMIN".equals(user.getRole());
                boolean targetIsAdmin = "ADMIN".equals(msg.getStr("role"));
                
                if (isSender || (isAdmin && !targetIsAdmin)) {
                    // Remove from Redis
                    redisTemplate.opsForList().remove(key, 1, msgStr);
                    
                    // Broadcast Recall
                    try {
                        JSONObject json = new JSONObject();
                        json.set("type", "RECALL");
                        json.set("messageId", messageId);
                        TextMessage textMessage = new TextMessage(json.toString());
                        
                        Set<WebSocketSession> sessions = channelSessions.get(channel);
                        if (sessions != null) {
                            for (WebSocketSession s : sessions) {
                                if (s.isOpen()) s.sendMessage(textMessage);
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                return; // Found and handled
            }
        }
    }
    
    private void saveToHistory(String channel, User user, String content, String messageId) {
        String key = "chat:history:" + channel;
        JSONObject json = new JSONObject();
        json.set("type", "CHAT");
        json.set("messageId", messageId);
        json.set("userId", user.getId());
        json.set("username", user.getUsername());
        json.set("avatar", user.getAvatar() != null ? "data:image/png;base64," + cn.hutool.core.codec.Base64.encode(user.getAvatar()) : null);
        json.set("role", user.getRole());
        json.set("content", content);
        json.set("time", LocalDateTime.now().toString());
        
        redisTemplate.opsForList().rightPush(key, json.toString());
        // Limit to 10000
        redisTemplate.opsForList().trim(key, -10000, -1);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String channel = sessionChannels.remove(session.getId());
        User user = sessionUsers.remove(session.getId());
        
        if (channel != null && channelSessions.containsKey(channel)) {
            channelSessions.get(channel).remove(session);
            if (user != null) {
                // Broadcast LEAVE only if no sessions left
                if (getUserSessionCount(channel, user.getId()) == 0) {
                    broadcast(channel, "LEAVE", user, null, null);
                }
            }
        }
    }
    
    private long getUserSessionCount(String channel, Long userId) {
        Set<WebSocketSession> sessions = channelSessions.get(channel);
        if (sessions == null) return 0;
        return sessions.stream()
            .map(s -> sessionUsers.get(s.getId()))
            .filter(u -> u != null && u.getId().equals(userId))
            .count();
    }

    private void broadcast(String channel, String type, User user, String content, String messageId) throws IOException {
        Set<WebSocketSession> sessions = channelSessions.get(channel);
        if (sessions == null) return;

        JSONObject json = new JSONObject();
        json.set("type", type);
        json.set("messageId", messageId);
        json.set("userId", user.getId());
        json.set("username", user.getUsername());
        json.set("avatar", user.getAvatar() != null ? "data:image/png;base64," + cn.hutool.core.codec.Base64.encode(user.getAvatar()) : null);
        json.set("role", user.getRole());
        json.set("content", content);
        json.set("time", LocalDateTime.now().toString());

        TextMessage textMessage = new TextMessage(json.toString());
        for (WebSocketSession s : sessions) {
            if (s.isOpen()) {
                s.sendMessage(textMessage);
            }
        }
    }

    private Map<String, String> parseQuery(String query) {
        Map<String, String> map = new java.util.HashMap<>();
        if (query == null) return map;
        String[] pairs = query.split("&");
        for (String pair : pairs) {
            String[] kv = pair.split("=");
            if (kv.length == 2) {
                map.put(kv[0], kv[1]);
            }
        }
        return map;
    }
}
