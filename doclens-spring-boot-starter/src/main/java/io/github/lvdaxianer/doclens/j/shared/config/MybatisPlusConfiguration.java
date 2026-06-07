package io.github.lvdaxianer.doclens.j.shared.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus 基础设施配置。
 *
 * @author lvdaxianerplus
 * @date 2026-06-07
 */
@Configuration
public class MybatisPlusConfiguration {

    /**
     * 注册仓储查询限制所需的分页支持。
     *
     * @return MyBatis-Plus 拦截器链
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
