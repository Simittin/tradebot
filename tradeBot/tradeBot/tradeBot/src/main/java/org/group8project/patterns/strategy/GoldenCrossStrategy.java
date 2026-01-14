package org.group8project.patterns.strategy;

import org.group8project.model.MarketData;
import org.group8project.model.TradeSignal;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class GoldenCrossStrategy implements TradingStrategy {

    // Uzun vadeli analiz için geniş bir hafıza gerekir
    private final List<Double> priceHistory = new ArrayList<>();
    
    // Gerçek piyasada standart: 50 ve 200'dür.
    // Ancak test ederken sonucunu hızlı görmek için 10 ve 30 yapıyoruz.
    // Gerçek kullanım için bunları 50 ve 200 olarak güncelleyebilirsin.
    private static final int SHORT_PERIOD = 10; // Örn: 50 Günlük
    private static final int LONG_PERIOD = 30;  // Örn: 200 Günlük

    @Override
    public TradeSignal analyze(MarketData data) {
        

        // Hafıza optimizasyonu: İhtiyacımız olandan fazlasını tutmayalım
        if (priceHistory.size() >= LONG_PERIOD + 5) {
            priceHistory.remove(0);
        }
        priceHistory.add(data.getPrice());


        // Uzun dönem veri toplanana kadar bekle
        if (priceHistory.size() < LONG_PERIOD+1) {
            System.out.println("[Golden Cross] Veri toplanıyor... (" + priceHistory.size() + "/" + LONG_PERIOD + ")");
            return TradeSignal.HOLD;
        }

        // 1. Şimdiki Ortalamaları Hesapla
        double currentShortSMA = calculateSMA(SHORT_PERIOD, 0); // Son elemana göre
        double currentLongSMA = calculateSMA(LONG_PERIOD, 0);

        // 2. Bir Önceki Ortalamaları Hesapla (Kesişimi tespit etmek için)
        // offset 1: sondan bir önceki veri seti
        double prevShortSMA = calculateSMA(SHORT_PERIOD, 1);
        double prevLongSMA = calculateSMA(LONG_PERIOD, 1);

        System.out.println("[Golden Cross] ShortSMA: " + currentShortSMA + " | LongSMA: " + currentLongSMA);

        // --- SİNYAL MANTIĞI ---

        // GOLDEN CROSS (AL): Kısa vade, Uzun vadeyi AŞAĞIDAN YUKARI kesti
        // (Önceki adımda alttaydı, şimdi üstte)
        if (prevShortSMA <= prevLongSMA && currentShortSMA > currentLongSMA) {
            System.out.println("!!! GOLDEN CROSS TESPİT EDİLDİ - BOĞA SEZONU !!!");
            return TradeSignal.BUY;
        }

        // DEATH CROSS (SAT): Kısa vade, Uzun vadeyi YUKARIDAN AŞAĞI kesti
        // (Önceki adımda üstteydi, şimdi altta)
        if (prevShortSMA >= prevLongSMA && currentShortSMA < currentLongSMA) {
            System.out.println("!!! DEATH CROSS TESPİT EDİLDİ - AYI SEZONU !!!");
            return TradeSignal.SELL;
        }

        // Kesişim yoksa mevcut trendi koru
        return TradeSignal.HOLD;
    }

    // Basit Hareketli Ortalama (SMA) Hesaplayıcı
    private double calculateSMA(int period, int offset) {
        double sum = 0;
        int startIndex = priceHistory.size() - period - offset;
        
        // Liste sınır kontrolü
        if (startIndex < 0) return 0;

        for (int i = 0; i < period; i++) {
            sum += priceHistory.get(startIndex + i);
        }
        return sum / period;
    }

    @Override
    public String getStrategyName() {
        return "Golden Cross Strategy";
    }

    @Override
    public String getStrategyType() {
        return "MOVING_AVERAGE_CROSSOVER";
    }

    @Override
    public StrategyTerm getTerm() {
        return StrategyTerm.LONG_TERM;
    }
}