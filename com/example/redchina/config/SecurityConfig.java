package com.example.redchina.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.logout.HeaderWriterLogoutHandler;
import org.springframework.security.web.header.writers.ClearSiteDataHeaderWriter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true) // 开启方法级权限控制
public class SecurityConfig {

    // 密码加密器（必须配置，否则认证失败）
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // 核心安全过滤链
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // 关闭CSRF（基础版简化，生产环境需开启）
                .csrf(csrf -> csrf.disable())
                // 权限规则配置
                .authorizeHttpRequests(auth -> auth
                        // 登录、注册页面允许匿名访问
                        .requestMatchers("/login", "/register", "/static/**", "/error").permitAll()
                        // 管理员接口仅管理员可访问
                        .requestMatchers("/admin/**").hasAnyRole("ADMIN")
                        // 其他接口需登录
                        .anyRequest().authenticated()
                )
                // 登录配置
                .formLogin(form -> form
                        .loginPage("/login") // 自定义登录页面
                        .loginProcessingUrl("/doLogin") // 登录提交接口
                        .usernameParameter("username") // 表单用户名参数
                        .passwordParameter("password") // 表单密码参数
                        .defaultSuccessUrl("/index", true) // 登录成功跳转首页
                        .failureUrl("/login?error=true") // 登录失败跳转
                        .permitAll()
                )
                // 退出配置
                .logout(logout -> logout
                        .logoutUrl("/logout") // 退出接口
                        .logoutSuccessUrl("/login?logout=true") // 退出成功跳转
                        .addLogoutHandler(new HeaderWriterLogoutHandler(new ClearSiteDataHeaderWriter(ClearSiteDataHeaderWriter.Directive.ALL)))
                        .invalidateHttpSession(true) // 销毁会话
                        .permitAll()
                );

        return http.build();
    }
}