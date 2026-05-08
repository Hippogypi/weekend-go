package com.weekendgo.amap;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
@EnableConfigurationProperties(AmapProperties.class)
public class AmapConfiguration {

    @Bean
    RestTemplate amapRestTemplate(RestTemplateBuilder builder) {
        return builder.build();
    }

    @Bean
    AmapClient amapClient(RestTemplate amapRestTemplate, AmapProperties properties) {
        return new AmapClient(amapRestTemplate, properties);
    }
}
