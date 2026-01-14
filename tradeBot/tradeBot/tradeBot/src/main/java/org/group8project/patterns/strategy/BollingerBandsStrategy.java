package org.group8project.patterns.strategy;

import org.group8project.model.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class BollingerBandsStrategy implements TradingStrategy {

    private final List<Double> history = new ArrayList<>();
    private static final int WINDOW = 20;
    private static final double K = 2.0; 
    private static final double MIN_WIDTH = 0.005; // En az %0.5 kar marjı olsun

    @Override
    public TradeSignal analyze(MarketData d) {
        if (history.size() >= WINDOW) history.remove(0);
        history.add(d.getPrice());

        if (history.size() < WINDOW) return TradeSignal.HOLD;

        // 1. Manuel Hesaplama (Stream'den daha hızlı)
        double sum = 0;
        for (double p : history) sum += p;
        double mean = sum / WINDOW;

        double variance = 0;
        for (double p : history) variance += Math.pow(p - mean, 2);
        double stdDev = Math.sqrt(variance / WINDOW);

        // 2. Bantları Belirle
        double upper = mean + (K * stdDev);
        double lower = mean - (K * stdDev);
        double price = d.getPrice();

        // 3. Bant Genişliği Kontrolü (Filtre)
        // Eğer bantlar çok darsa (Volatility Squeeze), patlama yakındır.
        // Tersi işlem yapmak (Reversion) tehlikelidir, bekle.
        double bandWidth = (upper - lower) / mean;
        if (bandWidth < MIN_WIDTH) return TradeSignal.HOLD;

        // 4. Sinyaller
        // Fiyat alt bandı deldiyse -> Ucuzluk -> AL
        if (price < lower) return TradeSignal.BUY;

        // Fiyat üst bandı deldiyse -> Pahalılık -> SAT
        if (price > upper) return TradeSignal.SELL;

        return TradeSignal.HOLD;
    }

    @Override public String getStrategyName() { return "Smart Bollinger"; }
    @Override public String getStrategyType() { return "VOLATILITY"; }
    @Override public StrategyTerm getTerm() { return StrategyTerm.SCALPING; }
}