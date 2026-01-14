package org.group8project.service;

import org.group8project.patterns.singleton.MarketDataFeed;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Random;

@Service
public class SimulationService {

    private final MarketDataFeed marketDataFeed;
    private boolean active = false;
    private double currentPrice = 0.0;
    private final Random random = new Random();

    // Binance API - Güncel fiyat için
    private static final String BINANCE_PRICE_URL = "https://api.binance.com/api/v3/ticker/price?symbol=BTCUSDT";

    // PİYASA MODLARI
    private enum MarketTrend {
        BULLISH, // Yükseliş Trendi (Boğa)
        BEARISH, // Düşüş Trendi (Ayı)
        SIDEWAYS, // Yatay (Sıkıcı)
        CRASH_PUMP // Aşırı Volatilite (Kriz)
    }

    private MarketTrend currentTrend = MarketTrend.SIDEWAYS;
    private int trendDuration = 0;

    public SimulationService() {
        this.marketDataFeed = MarketDataFeed.getInstance();
    }

    public void toggleSimulation(boolean isActive) {
        this.active = isActive;
        
        if (isActive) {
            // Simülasyon başlarken güncel BTC fiyatını çek
            fetchCurrentBitcoinPrice();
            System.out.println("[SIMULATION] ▶️ BAŞLATILDI - Güncel BTC fiyatından: $" + 
                String.format("%,.2f", currentPrice));
        } else {
            System.out.println("[SIMULATION] ⏹️ DURDURULDU");
        }
    }

    /**
     * Binance API'den güncel Bitcoin fiyatını çek
     */
    private void fetchCurrentBitcoinPrice() {
        try {
            URL url = new URL(BINANCE_PRICE_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream())
                );
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                // JSON parse: {"symbol":"BTCUSDT","price":"97432.12345678"}
                String json = response.toString();
                int priceIndex = json.indexOf("\"price\":\"");
                if (priceIndex != -1) {
                    int start = priceIndex + 9;
                    int end = json.indexOf("\"", start);
                    String priceStr = json.substring(start, end);
                    currentPrice = Double.parseDouble(priceStr);
                    
                    System.out.println("[SIMULATION] 📡 Güncel BTC fiyatı alındı: $" + 
                        String.format("%,.4f", currentPrice));
                }
            }
            conn.disconnect();

        } catch (Exception e) {
            // Hata olursa varsayılan fiyat kullan
            if (currentPrice <= 0) {
                currentPrice = 95000.0; // Varsayılan yaklaşık fiyat
                System.out.println("[SIMULATION] ⚠️ Fiyat alınamadı, varsayılan: $" + currentPrice);
            }
        }
    }

    // Her 1000ms (1 saniye) bir çalışır
    @Scheduled(fixedRate = 1000)
    public void generateMarketData() {
        if (!active) return;

        // Fiyat henüz ayarlanmadıysa çek
        if (currentPrice <= 0) {
            fetchCurrentBitcoinPrice();
            return;
        }

        // 1. Trend Değişimi Kararı
        if (trendDuration <= 0) {
            changeMarketTrend();
        }
        trendDuration--;

        // 2. Trende Göre Fiyat Hareketi (Yüzde bazlı - gerçekçi)
        double movementPercent = 0;
        double volatilityPercent = 0;

        switch (currentTrend) {
            case BULLISH:
                movementPercent = 0.0008; // %0.08 artış/saniye
                volatilityPercent = 0.0015; // %0.15 dalgalanma
                break;

            case BEARISH:
                movementPercent = -0.0008;
                volatilityPercent = 0.0015;
                break;

            case SIDEWAYS:
                movementPercent = 0.0;
                volatilityPercent = 0.0005; // %0.05 dalgalanma
                break;

            case CRASH_PUMP:
                movementPercent = (random.nextDouble() - 0.5) * 0.005; // %±0.25
                volatilityPercent = 0.008; // %0.8 dalgalanma
                break;
        }

        // 3. Rastgelelik Faktörü (Yüzde bazlı)
        double noise = (random.nextDouble() - 0.5) * volatilityPercent;

        // Yeni Fiyat = Eski * (1 + hareket + gürültü)
        currentPrice = currentPrice * (1 + movementPercent + noise);

        // Fiyat minimum $1000 altına düşmesin
        if (currentPrice < 1000) currentPrice = 1000;

        // 4. Yayınla (4 ondalık hassasiyetle)
        double roundedPrice = Math.round(currentPrice * 10000.0) / 10000.0;
        marketDataFeed.publishPrice("BTC", roundedPrice);
    }

    private void changeMarketTrend() {
        int pick = random.nextInt(100);

        if (pick < 40) {
            currentTrend = MarketTrend.BULLISH;
            trendDuration = random.nextInt(15) + 10;
            System.out.println("\n>>> PİYASA MODU: BOĞA SEZONU 🐂 (Yükseliş)");
        }
        else if (pick < 80) {
            currentTrend = MarketTrend.BEARISH;
            trendDuration = random.nextInt(15) + 10;
            System.out.println("\n>>> PİYASA MODU: AYI SEZONU 🐻 (Düşüş)");
        }
        else if (pick < 90) {
            currentTrend = MarketTrend.SIDEWAYS;
            trendDuration = 5;
            System.out.println("\n>>> PİYASA MODU: YATAY SEYİR 🦀");
        }
        else {
            currentTrend = MarketTrend.CRASH_PUMP;
            trendDuration = 5;
            System.out.println("\n>>> PİYASA MODU: 🚨 VOLATİLİTE 🚨");
        }
    }
}
