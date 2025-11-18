package com.crypto.exception;

public class CryptoNotFoundException extends RuntimeException {
    public CryptoNotFoundException(String coinId) {
        super("Criptomoeda não encontrada: " + coinId);
    }
}
