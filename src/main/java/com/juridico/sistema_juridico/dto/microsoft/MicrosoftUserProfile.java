package com.juridico.sistema_juridico.dto.microsoft;

import java.util.List;

public record MicrosoftUserProfile(
        String id,
        String userPrincipalName,
        List<String> businessPhones,
        String displayName,
        String jobTitle,
        String mail,
        String mobilePhone,
        String officeLocation) implements java.io.Serializable {

    public String preferredEmail() {
        if (mail != null && !mail.isBlank()) {
            return mail.trim().toLowerCase();
        }
        if (userPrincipalName != null && !userPrincipalName.isBlank()) {
            return userPrincipalName.trim().toLowerCase();
        }
        return null;
    }

    public String joinedBusinessPhones() {
        if (businessPhones == null || businessPhones.isEmpty()) {
            return null;
        }
        String joined = String.join(", ", businessPhones.stream()
                .filter(phone -> phone != null && !phone.isBlank())
                .map(String::trim)
                .toList());
        return joined.isBlank() ? null : joined;
    }
}
