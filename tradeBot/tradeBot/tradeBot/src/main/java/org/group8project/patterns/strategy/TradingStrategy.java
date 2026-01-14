package org.group8project.patterns.strategy;

import org.group8project.model.MarketData;
import org.group8project.model.TradeSignal;

public interface TradingStrategy {
    TradeSignal analyze(MarketData data);
    String getStrategyName();

    // Factory için ID
    String getStrategyType();

    // YENİ EKLENEN: Stratejinin vadesi (Scalping, Long Term vs.)
    StrategyTerm getTerm();
}