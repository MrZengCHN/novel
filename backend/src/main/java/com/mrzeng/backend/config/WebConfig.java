package com.mrzeng.backend.config;

import com.mrzeng.backend.common.JwtUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @org.springframework.beans.factory.annotation.Autowired
    private com.mrzeng.backend.user.mapper.UserMapper userMapper;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new LoginInterceptor())
                .addPathPatterns("/user/**") // Protect user routes
                .excludePathPatterns("/user/login", "/user/register"); // Exclude public routes

        registry.addInterceptor(new AdminInterceptor(userMapper))
                .addPathPatterns("/game/**"); // Game routes protection strategy inside interceptor
    }

    @Override
    public void addCorsMappings(org.springframework.web.servlet.config.annotation.CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true);
    }

    @Override
    public void addResourceHandlers(org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/images/**").addResourceLocations("file:./uploads/");
    }

    public class LoginInterceptor implements HandlerInterceptor {
        @Override
        public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
            if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
                return true;
            }

            String token = request.getHeader("Authorization");
            if (token == null || !token.startsWith("Bearer ")) {
                response.setStatus(401);
                return false;
            }

            token = token.substring(7);
            if (!JwtUtils.validateToken(token)) {
                response.setStatus(401);
                return false;
            }

            return true;
        }
    }

    public class AdminInterceptor implements HandlerInterceptor {
        private com.mrzeng.backend.user.mapper.UserMapper userMapper;

        public AdminInterceptor(com.mrzeng.backend.user.mapper.UserMapper userMapper) {
            this.userMapper = userMapper;
        }

        @Override
        public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
            // Allow CORS Preflight
            if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
                return true;
            }

            // Allow GET requests (public)
            if ("GET".equalsIgnoreCase(request.getMethod())) {
                return true;
            }

            // Check Token
            String token = request.getHeader("Authorization");
            if (token == null || !token.startsWith("Bearer ")) {
                response.setStatus(401);
                return false;
            }

            token = token.substring(7);
            if (!JwtUtils.validateToken(token)) {
                response.setStatus(401);
                return false;
            }

            // Check Role
            Long userId = JwtUtils.getUserId(token);
            com.mrzeng.backend.user.entity.User user = userMapper.selectById(userId);
            if (user == null || !"ADMIN".equals(user.getRole())) {
                response.setStatus(403); // Forbidden
                return false;
            }

            return true;
        }
    }
}
