package org.group8project.patterns.strategy;

import org.group8project.model.MarketData;
import org.group8project.model.TradeSignal;
import org.springframework.stereotype.Component;

@Component
public class MacdStrategy implements TradingStrategy {

    // --- Değişkenler (State) ---
    private double ema12 = 0;       // Hızlı EMA
    private double ema26 = 0;       // Yavaş EMA
    private double signalLine = 0;  // MACD'nin 9'luk EMA'sı (Trigger Line)
    
    // Önceki değerleri sakla (Kesişimi anlamak için)
    private double prevMacd = 0;
    private double prevSignal = 0;

    // Isınma Sayacı (EMA'nın oturması için gerekli)
    private int warmUpCount = 0;
    private static final int MIN_WARMUP = 35; // En az 35 veri bekle

    // --- Sabit Katsayılar ---
    private static final double ALPHA_12 = 2.0 / (12 + 1);
    private static final double ALPHA_26 = 2.0 / (26 + 1);
    private static final double ALPHA_9 =  2.0 / (9 + 1); // Sinyal hattı için

    @Override
    public TradeSignal analyze(MarketData data) {
        double price = data.getPrice();

        // 1. İlk Başlatma (Seed)
        if (warmUpCount == 0) {
            ema12 = price;
            ema26 = price;
            // İlk başta MACD 0 olacağı için signalLine da 0 başlar
            signalLine = 0; 
            warmUpCount++;
            return TradeSignal.HOLD;
        }

        // 2. EMA Hesaplamaları (Recursive)
        ema12 = (price - ema12) * ALPHA_12 + ema12;
        ema26 = (price - ema26) * ALPHA_26 + ema26;

        // 3. MACD Hattı Hesabı
        double currentMacd = ema12 - ema26;

        // 4. Sinyal Hattı Hesabı (MACD'nin 9 EMA'sı)
        // Not: Sinyal hattı hesaplamak için MACD verisinin oluşmasını bekliyoruz
        if (warmUpCount < 26) {
            // Henüz 26'lık EMA oturmadı, signalLine'ı sadece güncelle (init)
            signalLine = currentMacd; 
            warmUpCount++;
            return TradeSignal.HOLD;
        }

        // Sinyal hattını güncelle
        double currentSignal = (currentMacd - signalLine) * ALPHA_9 + signalLine;

        // Isınma süresi bitmediyse sinyal üretme, sadece kaydet
        if (warmUpCount < MIN_WARMUP) {
            signalLine = currentSignal;
            warmUpCount++;
            return TradeSignal.HOLD;
        }

        // --- SİNYAL MANTIĞI (CROSSOVER) ---
        
        // Önceki değerleri güncellemeden önce sinyal kontrolü yap
        // (Burada prevMacd ve prevSignal bir önceki 'analyze' çağrısından kalanlar)
        
        TradeSignal result = TradeSignal.HOLD;

        // Kesişim Kontrolü
        // AL: MACD hattı, Sinyal hattını AŞAĞIDAN YUKARI kesti
        boolean crossUp = prevMacd < prevSignal && currentMacd > currentSignal;
        
        // SAT: MACD hattı, Sinyal hattını YUKARIDAN AŞAĞI kesti
        boolean crossDown = prevMacd > prevSignal && currentMacd < currentSignal;

        if (crossUp) {
            result = TradeSignal.BUY;
        } else if (crossDown) {
            result = TradeSignal.SELL;
        }

        // State'i güncelle (Gelecek tur için)
        prevMacd = currentMacd;
        prevSignal = currentSignal;
        signalLine = currentSignal; // Recursive yapı için state güncellemesi

        return result;
    }

    @Override
    public String getStrategyName() {
        return "Standard MACD";
    }

    @Override
    public String getStrategyType() {
        return "MOMENTUM_TREND";
    }

    @Override
    public StrategyTerm getTerm() {
        return StrategyTerm.MID_TERM;
    }
}