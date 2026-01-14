package org.group8project.patterns.factory;

import org.group8project.patterns.strategy.TradingStrategy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class StrategyFactory {

    private final Map<String, TradingStrategy> strategyMap = new HashMap<>();

    @Autowired
    public StrategyFactory(List<TradingStrategy> strategies) {
        System.out.println("--- FACTORY BAŞLATILIYOR ---");
        for (TradingStrategy strategy : strategies) {
            String key = strategy.getStrategyType().toUpperCase();
            strategyMap.put(key, strategy);
            System.out.println("Yüklenen Strateji: " + key + " -> " + strategy.getStrategyName());
        }
        if (strategyMap.containsKey("MOVING_AVERAGE_CROSSOVER") && !strategyMap.containsKey("MA")) {
            strategyMap.put("MA", strategyMap.get("MOVING_AVERAGE_CROSSOVER"));
        }
        System.out.println("----------------------------");
    }

    public TradingStrategy getStrategy(String type) {
        if (type == null) {
            System.out.println("UYARI: Null strateji istendi. Varsayılan (MA) dönülüyor.");
            return strategyMap.getOrDefault("MA", strategyMap.get("MOVING_AVERAGE_CROSSOVER"));
        }
        if (!strategyMap.containsKey(type.toUpperCase())) {
            System.err.println("HATA: '" + type + "' stratejisi BULUNAMADI! Mevcut stratejiler: " + strategyMap.keySet());
            return strategyMap.getOrDefault("MA", strategyMap.get("MOVING_AVERAGE_CROSSOVER"));
        }

        return strategyMap.get(type.toUpperCase());
    }
}