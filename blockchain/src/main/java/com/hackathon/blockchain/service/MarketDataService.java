package com.hackathon.blockchain.service;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpClientErrorException;

@Service
public class MarketDataService {

    private final String apiUrl = "https://faas-lon1-917a94a7.doserverless.co/api/v1/web/fn-3d8ede30-848f-4a7a-acc2-22ba0cd9a382/default/fake-market-prices";
    private final RestTemplate restTemplate;

    public MarketDataService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public String getMarketPrices() {
        try {
            return restTemplate.getForObject(apiUrl, String.class);
        } catch (HttpClientErrorException e) {
            System.err.println("Error fetching market prices: " + e.getMessage());
            return null; // Manejo de errores simple
        }
    }
}