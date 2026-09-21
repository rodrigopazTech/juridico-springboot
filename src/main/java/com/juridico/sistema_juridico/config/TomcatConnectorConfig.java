package com.juridico.sistema_juridico.config;

import org.apache.coyote.http11.Http11Nio2Protocol;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TomcatConnectorConfig {

    @Bean
    @ConditionalOnProperty(name = "app.server.nio2", havingValue = "true")
    WebServerFactoryCustomizer<TomcatServletWebServerFactory> nio2TomcatConnector() {
        return factory -> factory.setProtocol(Http11Nio2Protocol.class.getName());
    }
}
