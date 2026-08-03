package com.juridico.sistema_juridico.service;

import com.juridico.sistema_juridico.config.MicrosoftAuthProperties;
import com.juridico.sistema_juridico.dto.microsoft.MicrosoftTokenResponse;
import com.juridico.sistema_juridico.dto.microsoft.MicrosoftUserProfile;
import com.juridico.sistema_juridico.dto.microsoft.OutlookEventResponse;

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

import java.util.Map;

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

    public String buildAuthorizationUrl(String state, java.util.List<String> scopes) {
        return buildAuthorizationUrl(state, properties.getRedirectUri(), scopes);
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

    public String buildCalendarAuthorizationUrl(String state) {
        return UriComponentsBuilder
                .fromHttpUrl(properties.getBaseUri() + "/" + properties.getTenantId() + "/oauth2/v2.0/authorize")
                .queryParam("client_id", properties.getClientId())
                .queryParam("response_type", "code")
                .queryParam("redirect_uri", properties.getCalendarRedirectUri())
                .queryParam("response_mode", "query")
                .queryParam("scope", String.join(" ", properties.getCalendarScopes()))
                .queryParam("state", state)
                .queryParam("prompt", "select_account")
                .build()
                .toUriString();
    }

    public MicrosoftTokenResponse exchangeCode(String code) {
        return exchangeCode(code, properties.getRedirectUri(), properties.getScopes());
    }

    public MicrosoftTokenResponse exchangeCode(String code, java.util.List<String> scopes) {
        return exchangeCode(code, properties.getRedirectUri(), scopes);
    }

    public MicrosoftTokenResponse exchangeCalendarCode(String code) {
        return exchangeCode(code, properties.getCalendarRedirectUri(), properties.getCalendarScopes());
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

    public MicrosoftTokenResponse refreshCalendarAccessToken(String refreshToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("client_id", properties.getClientId());
        body.add("client_secret", properties.getClientSecret());
        body.add("refresh_token", refreshToken);
        body.add("grant_type", "refresh_token");
        body.add("scope", String.join(" ", properties.getCalendarScopes()));

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

    public OutlookEventResponse createCalendarEvent(String accessToken, Map<String, Object> payload) {
        HttpHeaders headers = calendarHeaders(accessToken);
        ResponseEntity<OutlookEventResponse> response = restTemplate.postForEntity(
                "https://graph.microsoft.com/v1.0/me/events",
                new HttpEntity<>(payload, headers),
                OutlookEventResponse.class);
        return response.getBody();
    }

    public OutlookEventResponse updateCalendarEvent(String accessToken, String eventId, Map<String, Object> payload) {
        HttpHeaders headers = calendarHeaders(accessToken);
        ResponseEntity<OutlookEventResponse> response = restTemplate.exchange(
                "https://graph.microsoft.com/v1.0/me/events/" + eventId,
                HttpMethod.PATCH,
                new HttpEntity<>(payload, headers),
                OutlookEventResponse.class);
        return response.getBody();
    }

    public void deleteCalendarEvent(String accessToken, String eventId) {
        HttpHeaders headers = calendarHeaders(accessToken);
        restTemplate.exchange(
                "https://graph.microsoft.com/v1.0/me/events/" + eventId,
                HttpMethod.DELETE,
                new HttpEntity<>(headers),
                Void.class);
    }

    private HttpHeaders calendarHeaders(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setAccept(java.util.List.of(MediaType.APPLICATION_JSON));
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }
}
