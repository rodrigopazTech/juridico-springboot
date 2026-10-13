package com.juridico.sistema_juridico.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MicrosoftAuthPropertiesTest {

    @Test
    void microsoftButtonIsVisibleWhenLoginIsFullyConfigured() {
        MicrosoftAuthProperties properties = configuredProperties();

        assertTrue(properties.isConfigured());
        assertTrue(properties.isVisibleInUi());
    }

    @Test
    void microsoftButtonIsHiddenWhenIntegrationIsIncomplete() {
        MicrosoftAuthProperties properties = configuredProperties();
        properties.setClientSecret(" ");

        assertFalse(properties.isConfigured());
        assertFalse(properties.isVisibleInUi());
    }

    @Test
    void redirectUriNormalizesLegacyDomainToSiajFinabien() {
        MicrosoftAuthProperties properties = configuredProperties();
        properties.setRedirectUri("https://siaj.finabien.gob.mx/auth/microsoft/callback");

        assertEquals("https://siaj.finabien/auth/microsoft/callback", properties.getRedirectUri());
    }

    private MicrosoftAuthProperties configuredProperties() {
        MicrosoftAuthProperties properties = new MicrosoftAuthProperties();
        properties.setEnabled(true);
        properties.setTenantId("tenant");
        properties.setClientId("client");
        properties.setClientSecret("secret");
        properties.setRedirectUri("http://localhost:8080/auth/microsoft/callback");
        return properties;
    }
}
