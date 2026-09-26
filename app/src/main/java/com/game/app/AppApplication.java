package com.game.app;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * 客户端应用启动类。
 * 仅承载 HTTP 适配；业务在 common.trading。
 */
@SpringBootApplication
@ComponentScan(basePackages = {"com.game.app", "com.game.common"})
@MapperScan("com.game.common.trading.mapper")
public class AppApplication {

    public static void main(String[] args) {
        SpringApplication.run(AppApplication.class, args);
    }
}
