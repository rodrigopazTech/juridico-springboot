package com.juridico.sistema_juridico.dto.microsoft;

import com.fasterxml.jackson.annotation.JsonProperty;

public record MicrosoftTokenResponse(
        @JsonProperty("token_type") String tokenType,
        @JsonProperty("scope") String scope,
        @JsonProperty("expires_in") Long expiresIn,
        @JsonProperty("ext_expires_in") Long extExpiresIn,
        @JsonProperty("access_token") String accessToken,
        @JsonProperty("refresh_token") String refreshToken,
        @JsonProperty("id_token") String idToken) {
}
