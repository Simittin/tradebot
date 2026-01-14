package org.group8project.service;

import org.group8project.model.MarketData;
import org.group8project.patterns.observer.MarketObserver;
import org.group8project.patterns.singleton.MarketDataFeed;
import org.springframework.stereotype.Service;

import java.util.LinkedList;
import java.util.List;

@Service
public class AutoPilotService implements MarketObserver {

    private final TradingEngine tradingEngine;
    private final MarketDataFeed marketDataFeed;

    private boolean isAutoPilotEnabled = false;
    private final List<Double> priceHistory = new LinkedList<>();

    private static final int ANALYSIS_WINDOW = 15;

    public AutoPilotService(TradingEngine tradingEngine) {
        this.tradingEngine = tradingEngine;
        this.marketDataFeed = MarketDataFeed.getInstance();
        marketDataFeed.attach(this);
    }

    public void setAutoPilot(boolean enable) {
        this.isAutoPilotEnabled = enable;
        System.out.println("[AutoPilot] Durum: " + (enable ? "AKTİF 🟢 (Akıllı Mod)" : "PASİF 🔴"));
    }

    @Override
    public void update(MarketData data) {
        if (!isAutoPilotEnabled) return;

        priceHistory.add(data.getPrice());
        if (priceHistory.size() > ANALYSIS_WINDOW) {
            priceHistory.remove(0);
        }

        if (priceHistory.size() < 5) return;

        analyzeAndSwitchStrategy();
    }

    private void analyzeAndSwitchStrategy() {
        double volatility = calculateVolatility();
        String currentStrategyType = tradingEngine.getCurrentStrategyType();
        String targetStrategy = "";
        String reason = "";

        if (volatility > 2.5) {
            targetStrategy = "SCALPING"; // Momentum Scalper
            reason = "Yüksek Volatilite (Fırtına) ⛈️";
        }
        else if (volatility > 1.0) {
            targetStrategy = "VOLATILITY";
            reason = "Orta Volatilite (Dalgalı) 🌊";
        }

        else {
            if (priceHistory.size() >= 15) {
                targetStrategy = "MOVING_AVERAGE_CROSSOVER";
                reason = "Sakin Piyasa + Uzun Veri 🐢";
            }
            else {
                double avg = calculateAverage();
                double lastPrice = priceHistory.get(priceHistory.size() - 1);

                if (lastPrice > avg) {
                    targetStrategy = "MOMENTUM_TREND"; // MACD
                    reason = "Yükseliş Trendi 🚀";
                } else {
                    targetStrategy = "RSI"; // RSI
                    reason = "Düşüş/Tepki Beklentisi 📉";
                }
            }
        }

        if (!currentStrategyType.equalsIgnoreCase(targetStrategy) && !targetStrategy.isEmpty()) {
            System.out.println("[AutoPilot] Analiz: " + reason + " -> Geçiş: " + targetStrategy);
            tradingEngine.switchStrategy(targetStrategy);
        }
    }

    private double calculateVolatility() {
        double mean = calculateAverage();
        double variance = 0;
        for (double p : priceHistory) variance += Math.pow(p - mean, 2);
        return Math.sqrt(variance / priceHistory.size());
    }

    private double calculateAverage() {
        double sum = 0;
        for (double p : priceHistory) sum += p;
        return sum / priceHistory.size();
    }
}