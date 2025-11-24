package com.boot.ict05_final_user.domain.inventory.config;

import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

@Configuration
public class InventoryMessageSourceConfig {

    /** inventory 도메인 전용 메시지 번들 */
    @Bean
    public MessageSource inventoryMessageSource() {
        ReloadableResourceBundleMessageSource ms = new ReloadableResourceBundleMessageSource();
        // inventory 패키지 아래 리소스만 본다 (전역 application.yml 변경 불필요)
        ms.setBasenames(
                "classpath:com/boot/ict05_final_user/domain/inventory/ValidationMessages",
                "classpath:com/boot/ict05_final_user/domain/inventory/messages"
        );
        ms.setDefaultEncoding("UTF-8");
        ms.setFallbackToSystemLocale(false);
        return ms;
    }

    /** inventory 도메인 전용 Validator (위 messageSource 사용) */
    @Bean
    public LocalValidatorFactoryBean inventoryValidator(MessageSource inventoryMessageSource) {
        LocalValidatorFactoryBean lvfb = new LocalValidatorFactoryBean();
        lvfb.setValidationMessageSource(inventoryMessageSource);
        return lvfb;
    }
}
