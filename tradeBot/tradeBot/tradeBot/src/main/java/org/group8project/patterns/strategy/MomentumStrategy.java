package org.group8project.patterns.strategy;

import org.group8project.model.*;
import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.List;

@Component
public class MomentumStrategy implements TradingStrategy {

    // 1. Performans: LinkedList -> ArrayList (Hızlı erişim için)
    private final List<Double> prices = new ArrayList<>();
    private final List<Double> momHistory = new ArrayList<>();

    private static final int SHORT = 5;
    private static final int LONG = 10;
    
    // Sabit oranlar yerine parametrik olması daha iyidir ama şimdilik OK.
    private static final double THRESHOLD = 0.02; // %2

    @Override
    public TradeSignal analyze(MarketData data) {
        // Hafıza Yönetimi
        if (prices.size() >= 20) prices.remove(0);
        prices.add(data.getPrice());

        if (prices.size() <= LONG) return TradeSignal.HOLD;

        double shortMom = calculateMomentum(SHORT);
        double longMom = calculateMomentum(LONG);

        // Momentum geçmişini sakla
        if (momHistory.size() >= 5) momHistory.remove(0);
        momHistory.add(shortMom);

        // Sinyal Üretimi
        TradeSignal signal = generateSignal(shortMom, longMom);

        // 2. Mantıksal Düzeltme: Breakout (Kopuş) sinyali onaya takılmamalı
        // Eğer çok güçlü bir momentum varsa (Breakout), geçmiş onayı arama.
        boolean isBreakout = (shortMom > 0.04 && Math.abs(longMom) < 0.01);
        
        if (isBreakout) {
            return signal; // Direkt İlet
        }

        // Normal trend takibi için onay mekanizmasını çalıştır
        if (isConfirmed(signal)) {
            return signal;
        }

        return TradeSignal.HOLD;
    }

    private double calculateMomentum(int period) {
        // ArrayList olduğu için .get() işlemi çok hızlıdır (O(1))
        double current = prices.get(prices.size() - 1);
        double old = prices.get(prices.size() - 1 - period);
        
        // Sıfıra bölünme hatasını önle (Fiyat 0 olamaz ama güvenli kod prensibi)
        if (old == 0) return 0;
        
        return (current - old) / old;
    }

    private TradeSignal generateSignal(double shortMom, double longMom) {
        // Breakout (Erken Sinyal)
        if (shortMom > 0.04 && Math.abs(longMom) < 0.01) return TradeSignal.BUY;
        
        // Güçlü Yükseliş
        if (shortMom > THRESHOLD && longMom > 0) return TradeSignal.BUY;

        // Güçlü Düşüş
        if (shortMom < -THRESHOLD && longMom < 0) return TradeSignal.SELL;

        return TradeSignal.HOLD;
    }

    private boolean isConfirmed(TradeSignal signal) {
        if (signal == TradeSignal.HOLD) return true;
        if (momHistory.size() < 3) return true; // Yeterli geçmiş yoksa güven ve geç

        int supportCount = 0;
        // Son 3 veriye bak
        for (int i = momHistory.size() - 3; i < momHistory.size(); i++) {
            double m = momHistory.get(i);
            if ((signal == TradeSignal.BUY && m > 0) || 
                (signal == TradeSignal.SELL && m < 0)) {
                supportCount++;
            }
        }
        return supportCount >= 2;
    }

    @Override public String getStrategyName() { return "Optimized Pro Momentum"; }
    @Override public String getStrategyType() { return "MOMENTUM"; }
    @Override public StrategyTerm getTerm() { return StrategyTerm.SHORT_TERM; }
}