package com.lxk.project.common.db;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.HashMap;
import java.util.Map;

/**
 * @Author macos·lxk
 * @create 2021/5/4 下午7:35
 */

/**
 * @author macos
 * @Description  允许覆盖bean  beanName相同 以后者为准
 *               参见
 *                org.springframework.cloud.client.HostInfoEnvironmentPostProcessor
 * @ClassName AllowBeanDefinitionOverridingEnvironmentProcessor
 */
public class AllowBeanDefinitionOverridingEnvironmentProcessor implements EnvironmentPostProcessor {
    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        Map<String, Object> paramMap = new HashMap<>(4);
        paramMap.put("spring.main.allow-bean-definition-overriding","true");
        MapPropertySource mapPropertySource = new MapPropertySource("allowBeanDefinitionOverridingPropSource", paramMap);
        environment.getPropertySources().addLast(mapPropertySource);
    }
}
