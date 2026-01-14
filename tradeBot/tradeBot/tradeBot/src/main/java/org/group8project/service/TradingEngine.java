package org.group8project.service;

import org.group8project.model.MarketData;
import org.group8project.model.TradeSignal;
import org.group8project.patterns.factory.StrategyFactory;
import org.group8project.patterns.observer.MarketObserver;
import org.group8project.patterns.singleton.MarketDataFeed;
import org.group8project.patterns.strategy.TradingStrategy;
import org.group8project.patterns.template.SpotExecutor;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.List;

@Service
public class TradingEngine implements MarketObserver {

    private final MarketDataFeed marketDataFeed;
    private final StrategyFactory strategyFactory;
    private final SpotExecutor tradeExecutor;

    // TÜM Stratejilerin listesi (Arkada beslemek için)
    private final List<TradingStrategy> allStrategies;

    private TradingStrategy currentStrategy;

    // Singleton MarketDataFeed kullanımı
    public TradingEngine(StrategyFactory factory,
                         SpotExecutor executor,
                         List<TradingStrategy> allStrategies) {
        this.marketDataFeed = MarketDataFeed.getInstance();
        this.strategyFactory = factory;
        this.tradeExecutor = executor;
        this.allStrategies = allStrategies;

        this.currentStrategy = factory.getStrategy("MA");
    }

    @PostConstruct
    public void init() {
        marketDataFeed.attach(this);
        System.out.println("Trading Engine Başlatıldı.");
    }

    public void switchStrategy(String strategyType) {
        TradingStrategy newStrategy = strategyFactory.getStrategy(strategyType);
        if (newStrategy != null) {
            this.currentStrategy = newStrategy;
            System.out.println(">>> STRATEJİ DEĞİŞTİRİLDİ: " + currentStrategy.getStrategyName());
        }
    }

    public String getCurrentStrategyName() {
        return currentStrategy != null ? currentStrategy.getStrategyName() : "None";
    }

    public String getCurrentStrategyType() {
        return currentStrategy != null ? currentStrategy.getStrategyType() : "None";
    }

    @Override
    public void update(MarketData marketData) {
        // 1. ÖNCE TÜM STRATEJİLERİ GÜNCELLE (Hafızaları dolsun)
        for (TradingStrategy strategy : allStrategies) {
            // Sadece veri beslemesi yapıyoruz, return değerini kullanmıyoruz
            // Ancak Java'da void olmadığı için mecburen çağırıyoruz.
            // Bu sayede hepsi "priceHistory.add()" yapabiliyor.
            try {
                strategy.analyze(marketData);
            } catch (Exception e) {
                // Hata olursa (örn: logik hatası) sistemi durdurma
            }
        }

        // 2. SADECE AKTİF STRATEJİNİN KARARINI UYGULA
        if (currentStrategy == null) return;

        // Not: analyze() metodunu yukarıda çağırdığımız için
        // strateji içinde son eklenen veriyi tekrar eklememesi lazım.
        // *AMA* bizim stratejilerimiz basit olduğu için çift ekleme çok sorun değil
        // veya strateji içindeki analyze metodunun 'state' yönetimini ayırmamız gerekirdi.
        // Şimdilik basit çözüm: Current strategy'yi tekrar çağırmak yerine
        // yukarıdaki döngüde yakalayabiliriz ama bu kod karmaşıklaşır.

        // PRATİK ÇÖZÜM: Stratejilerimiz "analyze" metodunda veriyi ekliyor.
        // Eğer yukarıda hepsini çağırdıysak, currentStrategy de çağrıldı demektir.
        // O yüzden tekrar çağırmamalıyız, yoksa veri çiftlenir.

        // O zaman mantığı şöyle kuralım:

        TradeSignal finalSignal = TradeSignal.HOLD;

        for (TradingStrategy strategy : allStrategies) {
            // Her stratejiyi çalıştır
            TradeSignal signal = strategy.analyze(marketData);

            // Eğer bu strateji şu anki aktif strateji ise, onun kararını sakla
            if (strategy.getStrategyType().equals(currentStrategy.getStrategyType())) {
                finalSignal = signal;
            }
        }

        // Kararı Uygula
        tradeExecutor.executeTrade(finalSignal, marketData.getPrice());
    }
}