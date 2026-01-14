package org.group8project.patterns.strategy;

import org.group8project.model.MarketData;
import org.group8project.model.TradeSignal;
import org.springframework.stereotype.Component;
import java.util.*;

/**
 * Geliştirilmiş RSI Stratejisi
 * - RSI momentum analizi
 * - Trend yönü doğrulaması
 * - Dinamik threshold'lar
 * - Sinyal filtreleme
 * - RSI divergence tespiti
 */
@Component
public class RSIStrategy implements TradingStrategy {

    private final List<Double> closingPrices = new ArrayList<>();
    private final List<Double> rsiHistory = new ArrayList<>(); // RSI geçmişi için
    private static final int PERIOD = 14;
    private static final int MAX_HISTORY = 100;
    
    // Geliştirilmiş threshold'lar
    private static final double OVERSOLD_THRESHOLD = 30.0;
    private static final double OVERBOUGHT_THRESHOLD = 70.0;
    private static final double EXTREME_OVERSOLD = 20.0; // Aşırı aşırı satım
    private static final double EXTREME_OVERBOUGHT = 80.0; // Aşırı aşırı alım
    
    // Momentum analizi için
    private static final double MOMENTUM_THRESHOLD = 5.0; // RSI değişim hızı
    
    private double prevAvgGain = 0;
    private double prevAvgLoss = 0;
    private boolean isFirstRSI = true; // İlk hesaplama kontrolü
    
    private static final int WARMUP_PERIOD = PERIOD * 3;
    
    @Override
    public TradeSignal analyze(MarketData data) {
        // Hafıza Yönetimi
        if (closingPrices.size() >= MAX_HISTORY) {
            closingPrices.remove(0);
        }
        closingPrices.add(data.getPrice());

        // Yeterli veri yoksa bekle (En az Period + 1 veri lazım ki değişim hesaplansın)
        if (closingPrices.size() <= PERIOD) {
            return TradeSignal.HOLD;
        }

        // 1. Doğru Matematik ile RSI Hesapla
        double rsi = calculateWilderRSI();
        
        // RSI geçmişini güncelle
        if (rsiHistory.size() >= 20) {
            rsiHistory.remove(0);
        }
        rsiHistory.add(rsi);

        // Momentum analizi için biraz daha veri birikmesini bekle
        if (rsiHistory.size() < WARMUP_PERIOD) {
            return TradeSignal.HOLD;
        }

        // --- Sinyal Mantığı ---

        TradeSignal baseSignal = getBaseRSISignal(rsi);
        TradeSignal momentumSignal = analyzeMomentum(rsi);
        boolean trendConfirmation = confirmTrend(baseSignal);
        
        // Filtreleme - Yanlış sinyalleri azalt
        if (!isSignalValid(baseSignal, rsi)) {
            return TradeSignal.HOLD;
        }
        
        // Güçlü Sinyal: Temel Sinyal + Momentum + Trend aynı fikirde
        if (baseSignal == momentumSignal && trendConfirmation) {
            return baseSignal;
        }
        
        // Acil Durumlar: Aşırı uçlarda (20 altı / 80 üstü) agresif işlem
        if (rsi < EXTREME_OVERSOLD) return TradeSignal.BUY;
        if (rsi > EXTREME_OVERBOUGHT) return TradeSignal.SELL;
        
        // Momentum sinyali varsa ve ana sinyal nötr ise, momentuma güven
        if (momentumSignal != TradeSignal.HOLD && baseSignal == TradeSignal.HOLD) {
            return momentumSignal;
        }
        
        return baseSignal;
    }

    /**
     * Temel RSI sinyali
     */
    private TradeSignal getBaseRSISignal(double rsi) {
        if (rsi < OVERSOLD_THRESHOLD) return TradeSignal.BUY;
        if (rsi > OVERBOUGHT_THRESHOLD) return TradeSignal.SELL;
        return TradeSignal.HOLD;
    }

    /**
     * RSI momentum analizi - RSI'nin değişim hızını takip eder
     */
    private TradeSignal analyzeMomentum(double currentRSI) {
        if (rsiHistory.size() < 2) return TradeSignal.HOLD;
        
        double prevRSI = rsiHistory.get(rsiHistory.size() - 2);
        double rsiChange = currentRSI - prevRSI;
        
        // Hızlı yükseliş momentumu
        if (rsiChange > MOMENTUM_THRESHOLD && currentRSI > 60) return TradeSignal.SELL;
        // Hızlı düşüş momentumu
        if (rsiChange < -MOMENTUM_THRESHOLD && currentRSI < 40) return TradeSignal.BUY;
        
        // RSI Dönüş Sinyalleri (Cross-Back)
        // Aşırı satımdan (30 altından) yukarı çıkış -> GÜÇLÜ AL
        if (prevRSI < OVERSOLD_THRESHOLD && currentRSI > OVERSOLD_THRESHOLD) {
            return TradeSignal.BUY; 
        }
        // Aşırı alımdan (70 üstünden) aşağı iniş -> GÜÇLÜ SAT
        if (prevRSI > OVERBOUGHT_THRESHOLD && currentRSI < OVERBOUGHT_THRESHOLD) {
            return TradeSignal.SELL; 
        }
        
        return TradeSignal.HOLD;
    }

    /**
     * Trend yönü doğrulaması - Fiyat trendi ile RSI sinyalini doğrular
     */
    private boolean confirmTrend(TradeSignal signal) {
        if (closingPrices.size() < 6) return true;
        
        // Son fiyat ile 5 önceki fiyatı karşılaştır
        double recentPrice = closingPrices.get(closingPrices.size() - 1);
        double olderPrice = closingPrices.get(closingPrices.size() - 6); // index hatasını önlemek için -6
        double priceChange = recentPrice - olderPrice;
        
        // AL sinyali için fiyat düşmüş olmalı (Ucuzluk algısı)
        if (signal == TradeSignal.BUY) {
            return priceChange <= 0 || Math.abs(priceChange) < recentPrice * 0.02;
        }
        // SAT sinyali için fiyat yükselmiş olmalı
        if (signal == TradeSignal.SELL) {
            return priceChange >= 0 || Math.abs(priceChange) < recentPrice * 0.02;
        }
        
        return true;
    }

    /**
     * Sinyal geçerliliği kontrolü - Yanlış sinyalleri filtreler
     */
    private boolean isSignalValid(TradeSignal signal, double rsi) {
        if (signal == TradeSignal.HOLD) return true;
        
        // Nötr bölge (45-55 arası) genellikle gürültüdür
        if (rsi > 45 && rsi < 55) return false;
        
        // Whipsaw (Testere) Koruması: Çok sık sinyal değişimi var mı?
        if (rsiHistory.size() >= 5) {
            int signalChanges = 0;
            for (int i = rsiHistory.size() - 4; i < rsiHistory.size(); i++) {
                TradeSignal s1 = getBaseRSISignal(rsiHistory.get(i));
                TradeSignal s2 = getBaseRSISignal(rsiHistory.get(i-1));
                
                if (s1 != s2 && s1 != TradeSignal.HOLD && s2 != TradeSignal.HOLD) {
                    signalChanges++;
                }
            }
            if (signalChanges >= 2) return false;
        }
        
        return true;
    }

    /**
     * Geliştirilmiş RSI hesaplama (EMA tabanlı - daha hassas)
     */
    private double calculateWilderRSI() {
        // En son eklenen fiyat ile bir önceki fiyat arasındaki fark
        double currentPrice = closingPrices.get(closingPrices.size() - 1);
        double prevPrice = closingPrices.get(closingPrices.size() - 2);
        
        double change = currentPrice - prevPrice;
        double currentGain = change > 0 ? change : 0;
        double currentLoss = change < 0 ? Math.abs(change) : 0;

        // İlk Sefer: Basit Ortalama (SMA) kullanılır
        if (isFirstRSI) {
            double sumGain = 0;
            double sumLoss = 0;
            
            // Son 14 periyodun ortalamasını al (Loop sadece ilk sefer çalışır)
            // Not: closingPrices listesinin son 14 değişimini tarıyoruz
            for (int i = closingPrices.size() - PERIOD; i < closingPrices.size(); i++) {
                 double pNow = closingPrices.get(i);
                 double pPrev = closingPrices.get(i - 1);
                 double ch = pNow - pPrev;
                 if (ch > 0) sumGain += ch;
                 else sumLoss += Math.abs(ch);
            }
            
            prevAvgGain = sumGain / PERIOD;
            prevAvgLoss = sumLoss / PERIOD;
            isFirstRSI = false;
        } 
        else {
            // Sonraki Seferler: Smoothed Moving Average (Wilder's Formula)
            // Formül: ((ÖncekiOrtalama * 13) + ŞimdikiKazanç) / 14
            prevAvgGain = ((prevAvgGain * (PERIOD - 1)) + currentGain) / PERIOD;
            prevAvgLoss = ((prevAvgLoss * (PERIOD - 1)) + currentLoss) / PERIOD;
        }

        if (prevAvgLoss == 0) return 100;
        
        double rs = prevAvgGain / prevAvgLoss;
        return 100 - (100 / (1 + rs));
    }

    @Override
    public String getStrategyName() {
        return "Enhanced RSI Strategy";
    }

    @Override
    public String getStrategyType() {
        return "RSI";
    }

    @Override
    public StrategyTerm getTerm() {
        return StrategyTerm.SHORT_TERM;
    }
}