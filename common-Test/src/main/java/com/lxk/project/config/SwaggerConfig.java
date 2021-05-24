package com.lxk.project.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

/**
 * @Description TODO
 * @Author liuxiaokun@e6yun.com
 * @Created Date: 2021/5/24 15:43
 * @ClassName SwaggerConfig
 * @Remark
 */
@Configuration
@EnableSwagger2
public class SwaggerConfig {
    private ApiInfo apiInfo() {
        return new ApiInfoBuilder().title("API接口文档")
                .description("用户信息管理")
                .version("1.0.0")
                .build();
    }

    @Bean
    public Docket createRestApi() {
        return new Docket(DocumentationType.SWAGGER_2)
                .apiInfo(apiInfo())
                .select()
                .apis(RequestHandlerSelectors.basePackage("com.lxk.project"))
                .paths(PathSelectors.any())
                .build();
    }
}
