package com.game.app;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;

/**
 * 客户端应用启动类。
 * 仅承载 HTTP 适配；业务在 common.trading、common.user。
 * 排除 Spring Security 默认的内存用户：本项目用 JWT 认证，不需要它，也避免启动时打印随机密码。
 */
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
@ComponentScan(basePackages = {"com.game.app", "com.game.common"})
@MapperScan({"com.game.common.trading.mapper", "com.game.common.user.mapper", "com.game.common.order.mapper"})
public class AppApplication {

    public static void main(String[] args) {
        SpringApplication.run(AppApplication.class, args);
    }
}
