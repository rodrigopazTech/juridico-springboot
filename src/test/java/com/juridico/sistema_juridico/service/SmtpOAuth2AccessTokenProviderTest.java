package com.juridico.sistema_juridico.service;

import com.juridico.sistema_juridico.config.SmtpOAuth2Properties;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class SmtpOAuth2AccessTokenProviderTest {

    @Test
    void obtainsAndCachesClientCredentialsToken() {
        SmtpOAuth2Properties properties = new SmtpOAuth2Properties();
        properties.setTenantId("tenant-id");
        properties.setClientId("client-id");
        properties.setClientSecret("client-secret");

        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("https://login.microsoftonline.com/tenant-id/oauth2/v2.0/token"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("grant_type=client_credentials")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("scope=https%3A%2F%2Foutlook.office365.com%2F.default")))
                .andRespond(withSuccess("{\"access_token\":\"token-value\",\"expires_in\":3600}",
                        MediaType.APPLICATION_JSON));

        SmtpOAuth2AccessTokenProvider provider = new SmtpOAuth2AccessTokenProvider(properties, builder);

        assertEquals("token-value", provider.getAccessToken());
        assertEquals("token-value", provider.getAccessToken());
        server.verify();
    }
}
