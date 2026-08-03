package com.juridico.sistema_juridico.service;

import com.juridico.sistema_juridico.config.MicrosoftAuthProperties;
import com.juridico.sistema_juridico.dto.microsoft.MicrosoftTokenResponse;
import com.juridico.sistema_juridico.dto.microsoft.MicrosoftUserProfile;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;


@Service
public class MicrosoftGraphClient {

    private static final String GRAPH_ME_SELECT = "id,userPrincipalName,businessPhones,displayName,jobTitle,mail,mobilePhone,officeLocation";

    private final MicrosoftAuthProperties properties;
    private final RestTemplate restTemplate = new RestTemplate();

    public MicrosoftGraphClient(MicrosoftAuthProperties properties) {
        this.properties = properties;
    }

    public String buildAuthorizationUrl(String state) {
        return buildAuthorizationUrl(state, properties.getRedirectUri(), properties.getScopes());
    }

    private String buildAuthorizationUrl(String state, String redirectUri, java.util.List<String> scopes) {
        return UriComponentsBuilder
                .fromHttpUrl(properties.getBaseUri() + "/" + properties.getTenantId() + "/oauth2/v2.0/authorize")
                .queryParam("client_id", properties.getClientId())
                .queryParam("response_type", "code")
                .queryParam("redirect_uri", redirectUri)
                .queryParam("response_mode", "query")
                .queryParam("scope", String.join(" ", scopes))
                .queryParam("state", state)
                .queryParam("prompt", "select_account")
                .build()
                .toUriString();
    }

    public MicrosoftTokenResponse exchangeCode(String code) {
        return exchangeCode(code, properties.getRedirectUri(), properties.getScopes());
    }

    private MicrosoftTokenResponse exchangeCode(String code, String redirectUri, java.util.List<String> scopes) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("client_id", properties.getClientId());
        body.add("client_secret", properties.getClientSecret());
        body.add("code", code);
        body.add("redirect_uri", redirectUri);
        body.add("grant_type", "authorization_code");
        body.add("scope", String.join(" ", scopes));

        ResponseEntity<MicrosoftTokenResponse> response = restTemplate.postForEntity(
                properties.getBaseUri() + "/" + properties.getTenantId() + "/oauth2/v2.0/token",
                new HttpEntity<>(body, headers),
                MicrosoftTokenResponse.class);

        return response.getBody();
    }

    public MicrosoftUserProfile fetchMe(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setAccept(java.util.List.of(MediaType.APPLICATION_JSON));

        String url = UriComponentsBuilder
                .fromHttpUrl("https://graph.microsoft.com/v1.0/me")
                .queryParam("$select", GRAPH_ME_SELECT)
                .build()
                .toUriString();

        ResponseEntity<MicrosoftUserProfile> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                MicrosoftUserProfile.class);

        return response.getBody();
    }

}
