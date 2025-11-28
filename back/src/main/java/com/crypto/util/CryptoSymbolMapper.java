
package com.crypto.util;

import java.util.HashMap;
import java.util.Map;


public class CryptoSymbolMapper {

    private static final Map<String, String> SYMBOL_TO_COIN_ID = new HashMap<>();

    static {
        SYMBOL_TO_COIN_ID.put("BTC", "bitcoin");
        SYMBOL_TO_COIN_ID.put("ETH", "ethereum");
        SYMBOL_TO_COIN_ID.put("BNB", "binancecoin");
        SYMBOL_TO_COIN_ID.put("XRP", "ripple");
        SYMBOL_TO_COIN_ID.put("ADA", "cardano");
        SYMBOL_TO_COIN_ID.put("DOGE", "dogecoin");
        SYMBOL_TO_COIN_ID.put("SOL", "solana");
        SYMBOL_TO_COIN_ID.put("TRX", "tron");
        SYMBOL_TO_COIN_ID.put("DOT", "polkadot");
        SYMBOL_TO_COIN_ID.put("MATIC", "matic-network");
        SYMBOL_TO_COIN_ID.put("LTC", "litecoin");
        SYMBOL_TO_COIN_ID.put("SHIB", "shiba-inu");
        SYMBOL_TO_COIN_ID.put("AVAX", "avalanche-2");
        SYMBOL_TO_COIN_ID.put("UNI", "uniswap");
        SYMBOL_TO_COIN_ID.put("LINK", "chainlink");
        SYMBOL_TO_COIN_ID.put("XLM", "stellar");
        SYMBOL_TO_COIN_ID.put("ATOM", "cosmos");
        SYMBOL_TO_COIN_ID.put("ETC", "ethereum-classic");
        SYMBOL_TO_COIN_ID.put("XMR", "monero");
        SYMBOL_TO_COIN_ID.put("BCH", "bitcoin-cash");
        SYMBOL_TO_COIN_ID.put("ALGO", "algorand");
        SYMBOL_TO_COIN_ID.put("VET", "vechain");
        SYMBOL_TO_COIN_ID.put("FIL", "filecoin");
        SYMBOL_TO_COIN_ID.put("APT", "aptos");
        SYMBOL_TO_COIN_ID.put("HBAR", "hedera-hashgraph");
    }


    public static String toCoinId(String symbol) {
        if (symbol == null || symbol.isEmpty()) {
            throw new IllegalArgumentException("Símbolo não pode ser nulo ou vazio");
        }

        String upperSymbol = symbol.toUpperCase().trim();

        if (symbol.contains("-") || symbol.length() > 5) {
            return symbol.toLowerCase();
        }

        return SYMBOL_TO_COIN_ID.getOrDefault(upperSymbol, symbol.toLowerCase());
    }


    public static boolean isSupported(String symbol) {
        return SYMBOL_TO_COIN_ID.containsKey(symbol.toUpperCase().trim());
    }


    public static Map<String, String> getAllMappings() {
        return new HashMap<>(SYMBOL_TO_COIN_ID);
    }
}