package com.game.common.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus 配置（app / console 共用）。
 */
@Configuration
public class MybatisPlusConfig {

    /** 单页最大条数，避免一次拉取过量数据 */
    private static final long MAX_PAGE_SIZE = 500L;

    /**
     * 分页插件：selectPage / selectMapsPage 依赖它拼接 limit 与 count SQL。
     * 不注册该插件时，分页参数不会生效。
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        PaginationInnerInterceptor pagination = new PaginationInnerInterceptor(DbType.MYSQL);
        pagination.setMaxLimit(MAX_PAGE_SIZE);

        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(pagination);
        return interceptor;
    }
}
