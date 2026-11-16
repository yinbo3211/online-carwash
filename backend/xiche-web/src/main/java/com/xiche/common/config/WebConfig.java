package com.xiche.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.multipart.support.StandardServletMultipartResolver;

/** 文件上传解析：证据图上传所需，使用 Servlet 3.0 内置实现，无第三方依赖。 */
@Configuration
public class WebConfig {
    @Bean(name = "multipartResolver")
    public StandardServletMultipartResolver multipartResolver() {
        return new StandardServletMultipartResolver();
    }
}
