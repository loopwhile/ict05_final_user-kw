package com.boot.ict05_final_user.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "hq")
@Getter @Setter
public class HqProps {
    private String userApiBaseUrl;
    private String cookieDomain;
    private boolean cookieSecure;
}
