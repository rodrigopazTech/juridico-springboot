package com.juridico.sistema_juridico.service;

import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SmtpOAuth2MailSenderFactoryTest {

    @Test
    void configuresJavaMailForXoauth2Only() {
        SmtpOAuth2AccessTokenProvider tokenProvider = mock(SmtpOAuth2AccessTokenProvider.class);
        when(tokenProvider.getAccessToken()).thenReturn("oauth-token");
        SmtpOAuth2MailSenderFactory factory = new SmtpOAuth2MailSenderFactory(tokenProvider);
        ReflectionTestUtils.setField(factory, "host", "smtp.office365.com");
        ReflectionTestUtils.setField(factory, "port", 587);
        ReflectionTestUtils.setField(factory, "username", "siaj@finabien.gob.mx");
        ReflectionTestUtils.setField(factory, "connectionTimeout", 10000);
        ReflectionTestUtils.setField(factory, "readTimeout", 10000);
        ReflectionTestUtils.setField(factory, "writeTimeout", 10000);

        JavaMailSenderImpl sender = (JavaMailSenderImpl) factory.create();

        assertEquals("oauth-token", sender.getPassword());
        assertEquals("XOAUTH2", sender.getJavaMailProperties().getProperty("mail.smtp.auth.mechanisms"));
        assertEquals("true", sender.getJavaMailProperties().getProperty("mail.smtp.auth.login.disable"));
        assertEquals("true", sender.getJavaMailProperties().getProperty("mail.smtp.auth.plain.disable"));
    }
}
