package com.juridico.sistema_juridico.service;

import org.springframework.stereotype.Service;

@Service
public class PasswordPolicyService {

    public static final int MIN_LENGTH = 8;
    public static final String DESCRIPTION =
            "La contrasena debe tener minimo 8 caracteres, una mayuscula, un numero, un simbolo y no contener espacios.";

    public boolean isValid(String password) {
        if (password == null || password.length() < MIN_LENGTH || password.chars().anyMatch(Character::isWhitespace)) {
            return false;
        }
        boolean upper = password.chars().anyMatch(Character::isUpperCase);
        boolean digit = password.chars().anyMatch(Character::isDigit);
        boolean special = password.chars().anyMatch(ch -> "!@#$%^&*()_+-=[]{};':\"\\|,.<>/?".indexOf(ch) >= 0);
        return upper && digit && special;
    }

    public String description() {
        return DESCRIPTION;
    }
}
