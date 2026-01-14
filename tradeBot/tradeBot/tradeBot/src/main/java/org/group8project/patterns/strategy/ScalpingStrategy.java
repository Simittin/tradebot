package org.group8project.patterns.strategy;

import org.group8project.model.*;
import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.List;

@Component
public class ScalpingStrategy implements TradingStrategy {

    // LinkedList yerine ArrayList (O(1) erişim hızı için)
    private final List<Double> prices = new ArrayList<>();
    private final List<Double> volHist = new ArrayList<>();
    
    private static final int FAST = 3;
    private static final int SLOW = 8;
    private static final int VOLATILITY_PERIOD = 20; // Volatilite için daha geniş pencere

    @Override
    public TradeSignal analyze(MarketData d) {
        // Hafıza Yönetimi
        if (prices.size() >= VOLATILITY_PERIOD + 1) prices.remove(0);
        prices.add(d.getPrice());

        // Yeterli veri yoksa bekle
        if (prices.size() <= SLOW) return TradeSignal.HOLD;

        // 1. Hesaplamalar
        double maFast = calculateSMA(FAST);
        double maSlow = calculateSMA(SLOW);
        
        // Volatiliteyi FAST periyoduna göre değil, biraz daha geniş bir aralığa göre hesapla
        // (3 barlık standart sapma çok hatalı sonuç verir)
        double currentVol = calculateStdDev(FAST); 

        // Volatilite Geçmişi Yönetimi
        if (volHist.size() >= 20) volHist.remove(0);
        volHist.add(currentVol);

        // 2. Piyasa Canlı mı? (Stream API yerine hızlı döngü)
        double sumVol = 0;
        for(Double v : volHist) sumVol += v;
        double avgVol = volHist.isEmpty() ? 0 : sumVol / volHist.size();

        // Eğer mevcut oynaklık, ortalamanın %80'inden azsa piyasa "ölü" demektir.
        if (currentVol < avgVol * 0.8) return TradeSignal.HOLD;

        // 3. Sinyal Mantığı (Momentum Scalping)
        double currentPrice = d.getPrice();

        // AL: Kısa ortalama Uzunu kesti VE Fiyat ortalamanın üzerinde (Teyitli)
        if (maFast > maSlow && currentPrice > maFast) {
            return TradeSignal.BUY;
        }

        // SAT: Kısa ortalama Uzunun altında VE Fiyat ortalamanın altında
        if (maFast < maSlow && currentPrice < maFast) {
            return TradeSignal.SELL;
        }

        return TradeSignal.HOLD;
    }

    // Basit Hareketli Ortalama (Optimize Edilmiş Döngü)
    private double calculateSMA(int p) {
        double sum = 0;
        int size = prices.size();
        // Tersten döngü veya size-p'den başlama
        for (int i = size - p; i < size; i++) {
            sum += prices.get(i); // ArrayList olduğu için çok hızlı
        }
        return sum / p;
    }

    // Standart Sapma
    private double calculateStdDev(int p) {
        if (prices.size() < p) return 0;
        
        double mean = calculateSMA(p);
        double sumSqDiff = 0;
        int size = prices.size();
        
        for (int i = size - p; i < size; i++) {
            double diff = prices.get(i) - mean;
            sumSqDiff += diff * diff;
        }
        return Math.sqrt(sumSqDiff / p);
    }

    @Override public String getStrategyName() { return "Optimized Momentum Scalper"; }
    @Override public String getStrategyType() { return "SCALPING"; }
    @Override public StrategyTerm getTerm() { return StrategyTerm.SCALPING; }
}