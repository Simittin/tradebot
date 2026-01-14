package org.group8project.patterns.singleton;

import org.group8project.model.MarketData;
import org.group8project.patterns.observer.MarketObserver;

import java.util.ArrayList;
import java.util.List;

/**
 * Singleton Pattern - Thread-Safe Implementation
 * Market verilerini yöneten ve observer'lara bildiren merkezi feed.
 */
public class MarketDataFeed {
    // Volatile: CPU cache yerine ana bellekten okunmasını garanti eder
    private static volatile MarketDataFeed instance;
    
    private final List<MarketObserver> observers = new ArrayList<>();
    private double lastPrice = 0.0;

    /**
     * Private constructor - Dışarıdan instance oluşturulmasını engeller
     */
    private MarketDataFeed() {
        // Reflection saldırılarına karşı koruma
        if (instance != null) {
            throw new IllegalStateException("Singleton instance already exists! Use getInstance() instead.");
        }
    }

    /**
     * Double-Checked Locking ile thread-safe Singleton instance döndürür
     * @return MarketDataFeed singleton instance
     */
    public static MarketDataFeed getInstance() {
        if (instance == null) {
            synchronized (MarketDataFeed.class) {
                if (instance == null) {
                    instance = new MarketDataFeed();
                }
            }
        }
        return instance;
    }

    public void attach(MarketObserver observer) {
        observers.add(observer);
    }

    public void detach(MarketObserver observer) {
        observers.remove(observer);
    }

    public void publishPrice(String symbol, double price) {
        this.lastPrice = price;
        MarketData data = new MarketData(symbol, price);
        System.out.println("\n[MARKET FEED] Yeni Fiyat: " + data);
        notifyObservers(data);
    }

    private void notifyObservers(MarketData data) {
        for (MarketObserver observer : observers) {
            observer.update(data);
        }
    }

    public double getLastPrice() {
        return lastPrice;
    }
}