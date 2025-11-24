package com.boot.ict05_final_user.config;

import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;


import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * QueryDSL 사용을 위해 사용하는 JPAQueryFactory
 */
@EnableConfigurationProperties(HqProps.class)
@Configuration
public class AppConfig {

    /**
     * QueryDSL 사용을 위해 사용하는 JPAQueryFactory
     */
    @Bean
    public JPAQueryFactory jpaQueryFactory(EntityManager entityManager) {
        return new JPAQueryFactory(entityManager);
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

}
