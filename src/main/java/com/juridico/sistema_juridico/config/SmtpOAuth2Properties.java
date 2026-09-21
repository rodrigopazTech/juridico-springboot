package com.juridico.sistema_juridico.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.email.oauth2")
public class SmtpOAuth2Properties {

    private boolean enabled = true;
    private String tenantId;
    private String clientId;
    private String clientSecret;
    private String tokenUri = "https://login.microsoftonline.com/{tenant}/oauth2/v2.0/token";
    private String scope = "https://outlook.office365.com/.default";

    public void validate() {
        require("MAIL_OAUTH2_TENANT_ID", tenantId);
        require("MAIL_OAUTH2_CLIENT_ID", clientId);
        require("MAIL_OAUTH2_CLIENT_SECRET", clientSecret);
    }

    public String resolvedTokenUri() {
        validate();
        return tokenUri.replace("{tenant}", tenantId.trim());
    }

    private void require(String setting, String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Falta configurar " + setting + " para SMTP OAuth2.");
        }
    }
}
