package org.group8project.patterns.observer;
import org.group8project.model.MarketData;

public interface MarketObserver {
    void update(MarketData marketData);
}
