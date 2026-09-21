package com.juridico.sistema_juridico.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.juridico.sistema_juridico.config.SmtpOAuth2Properties;
import org.springframework.http.MediaType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.time.Instant;

@Component
public class SmtpOAuth2AccessTokenProvider {

    private final SmtpOAuth2Properties properties;
    private final RestClient restClient;
    private volatile CachedToken cachedToken;

    @Autowired
    public SmtpOAuth2AccessTokenProvider(SmtpOAuth2Properties properties) {
        this(properties, RestClient.builder().requestFactory(new SimpleClientHttpRequestFactory()));
    }

    SmtpOAuth2AccessTokenProvider(SmtpOAuth2Properties properties, RestClient.Builder restClientBuilder) {
        this.properties = properties;
        this.restClient = restClientBuilder.build();
    }

    public String getAccessToken() {
        CachedToken current = cachedToken;
        if (current != null && current.isUsable()) {
            return current.value();
        }
        return refreshToken();
    }

    private synchronized String refreshToken() {
        CachedToken current = cachedToken;
        if (current != null && current.isUsable()) {
            return current.value();
        }

        properties.validate();
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id", properties.getClientId().trim());
        form.add("client_secret", properties.getClientSecret());
        form.add("grant_type", "client_credentials");
        form.add("scope", properties.getScope());

        TokenResponse response = restClient.post()
                .uri(properties.resolvedTokenUri())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(TokenResponse.class);

        if (response == null || response.accessToken() == null || response.accessToken().isBlank()) {
            throw new IllegalStateException("Microsoft Entra no devolvio un token SMTP OAuth2.");
        }

        long lifetimeSeconds = response.expiresIn() > 0 ? response.expiresIn() : 300;
        cachedToken = new CachedToken(response.accessToken(), Instant.now().plusSeconds(lifetimeSeconds));
        return cachedToken.value();
    }

    record TokenResponse(@JsonProperty("access_token") String accessToken,
                         @JsonProperty("expires_in") long expiresIn) {
    }

    private record CachedToken(String value, Instant expiresAt) {
        private boolean isUsable() {
            return expiresAt.isAfter(Instant.now().plusSeconds(60));
        }
    }
}
