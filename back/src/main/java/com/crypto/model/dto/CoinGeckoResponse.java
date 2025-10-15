package com.crypto.model.dto;

import com.crypto.dto.CryptoCurrency;
import lombok.Data;
import java.util.List;

@Data
public class CoinGeckoResponse {
    private List<CryptoCurrency> cryptocurrencies;
}
