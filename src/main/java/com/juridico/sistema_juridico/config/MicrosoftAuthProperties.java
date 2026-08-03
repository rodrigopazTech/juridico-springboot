package com.juridico.sistema_juridico.config;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.microsoft")
public class MicrosoftAuthProperties {

    private boolean enabled = false;
    private String baseUri = "https://login.microsoftonline.com";
    private String tenantId;
    private String clientId;
    private String clientSecret;
    private String redirectUri;
    private String calendarRedirectUri;
    private String tokenEncryptionSecret;
    private List<String> scopes = new ArrayList<>(List.of(
            "openid",
            "profile",
            "email",
            "https://graph.microsoft.com/User.Read"));
    private List<String> calendarScopes = new ArrayList<>(List.of(
            "offline_access",
            "https://graph.microsoft.com/Calendars.ReadWrite"));

    public boolean isConfigured() {
        return enabled
                && hasText(baseUri)
                && hasText(tenantId)
                && hasText(clientId)
                && hasText(clientSecret)
                && hasText(redirectUri);
    }

    public boolean isCalendarConfigured() {
        return isConfigured() && hasText(calendarRedirectUri) && hasText(tokenEncryptionSecret);
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getBaseUri() {
        return baseUri;
    }

    public void setBaseUri(String baseUri) {
        this.baseUri = baseUri;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    public String getRedirectUri() {
        return redirectUri;
    }

    public void setRedirectUri(String redirectUri) {
        this.redirectUri = redirectUri;
    }

    public String getCalendarRedirectUri() {
        return calendarRedirectUri;
    }

    public void setCalendarRedirectUri(String calendarRedirectUri) {
        this.calendarRedirectUri = calendarRedirectUri;
    }

    public String getTokenEncryptionSecret() {
        return tokenEncryptionSecret;
    }

    public void setTokenEncryptionSecret(String tokenEncryptionSecret) {
        this.tokenEncryptionSecret = tokenEncryptionSecret;
    }

    public List<String> getScopes() {
        return scopes;
    }

    public void setScopes(List<String> scopes) {
        this.scopes = scopes;
    }

    public List<String> getCalendarScopes() {
        return calendarScopes;
    }

    public void setCalendarScopes(List<String> calendarScopes) {
        this.calendarScopes = calendarScopes;
    }
}
