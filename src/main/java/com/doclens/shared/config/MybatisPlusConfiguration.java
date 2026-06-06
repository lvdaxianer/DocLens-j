package com.doclens.shared.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus infrastructure configuration.
 *
 * @author lvdaxianerplus
 * @date 2026-06-07
 */
@Configuration
public class MybatisPlusConfiguration {

    /**
     * Registers pagination support for repository query limits.
     *
     * @return MyBatis-Plus interceptor chain
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.POSTGRE_SQL));
        return interceptor;
    }
}
