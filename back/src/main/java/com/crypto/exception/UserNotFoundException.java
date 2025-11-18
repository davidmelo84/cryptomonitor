package com.crypto.exception;

public class UserNotFoundException extends RuntimeException {
    public UserNotFoundException(String username) {
        super("Usuário não encontrado: " + username);
    }
}
