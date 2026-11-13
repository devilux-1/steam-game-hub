package com.steamhub.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.baomidou.mybatisplus.spring.MybatisSqlSessionFactoryBean;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

/**
 * MyBatis-Plus 装配配置。
 * 官方未提供适配 Spring Boot 3 的 starter（本地离线仓库也没有），因此手动装配
 * SqlSessionFactory 并通过 @MapperScan 注册 mapper，行为与官方 starter 等价。
 */
@Configuration
@MapperScan("com.steamhub.mapper")
public class MybatisPlusConfig {

    /**
     * 构建 MyBatis-Plus 的 SqlSessionFactory，替代 mybatis 原生工厂以启用 MP 特性。
     * 必须把 MP 拦截器链显式 setPlugins 进工厂：手动装配时官方 starter 不会自动注入，
     * 漏掉这一步会导致分页插件不生效（不执行 count 查询，total 恒为 0）。
     *
     * @param dataSource           数据源，由 spring-boot-starter-jdbc 按配置文件自动装配，非空
     * @param mybatisPlusInterceptor MP 拦截器链（含分页插件），非空
     * @return 已注入数据源与拦截器的 SqlSessionFactoryBean，交给容器完成工厂创建
     */
    @Bean
    public MybatisSqlSessionFactoryBean mybatisSqlSessionFactoryBean(DataSource dataSource,
                                                                     MybatisPlusInterceptor mybatisPlusInterceptor) {
        MybatisSqlSessionFactoryBean factoryBean = new MybatisSqlSessionFactoryBean();
        factoryBean.setDataSource(dataSource);
        factoryBean.setPlugins(mybatisPlusInterceptor);
        return factoryBean;
    }

    /**
     * 注册 MP 插件链，当前包含分页插件，供搜索与后台列表分页使用。
     *
     * @return 已添加分页插件的拦截器链
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        // 本项目固定使用 MySQL，分页方言按 MySQL 生成 LIMIT 语句
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }
}
