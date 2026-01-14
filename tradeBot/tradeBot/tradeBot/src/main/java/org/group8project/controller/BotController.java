package org.group8project.controller;

import org.group8project.patterns.singleton.MarketDataFeed;
import org.group8project.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/bot")
public class BotController {

    private final MarketDataFeed marketDataFeed;
    private final TradingEngine tradingEngine;

    @Autowired
    private WalletService walletService;

    @Autowired
    private AutoPilotService autoPilotService;

    @Autowired
    private SimulationService simulationService;

    @Autowired
    private RealMarketService realMarketService;

    @Autowired
    private WalletLogger walletLogger;

    public BotController(TradingEngine tradingEngine) {
        this.marketDataFeed = MarketDataFeed.getInstance();
        this.tradingEngine = tradingEngine;
    }

    @PostMapping("/price")
    public String updatePrice(@RequestParam String symbol, @RequestParam double price) {
        marketDataFeed.publishPrice(symbol, price);
        return "Fiyat işlendi: " + price;
    }

    @GetMapping("/price")
    public double getCurrentPrice() {
        return marketDataFeed.getLastPrice();
    }

    @PostMapping("/strategy")
    public String setStrategy(@RequestParam String type) {
        tradingEngine.switchStrategy(type);
        return "Strateji güncellendi: " + type;
    }

    @GetMapping("/balance")
    public double getBalance() {
        return walletService.getUsdBalance();
    }

    @GetMapping("/assets")
    public double getAssetAmount(@RequestParam String symbol) {
        return walletService.getAssetAmount(symbol);
    }

    @PostMapping("/autopilot")
    public String toggleAutoPilot(@RequestParam boolean enable) {
        autoPilotService.setAutoPilot(enable);
        return enable ? "Otomatik Pilot AKTİF" : "Otomatik Pilot PASİF";
    }

    @PostMapping("/simulation")
    public String toggleSimulation(@RequestParam boolean active) {
        simulationService.toggleSimulation(active);
        return active ? "Simülasyon BAŞLATILDI 🚀" : "Simülasyon DURDURULDU 🛑";
    }

    @PostMapping("/realmarket")
    public String toggleRealMarket(@RequestParam boolean active) {
        realMarketService.toggleRealMarket(active);
        return active ? "GERÇEK PİYASA AKTİF 🌐 (CoinGecko)" : "Gerçek piyasa DURDURULDU 🔴";
    }

    // Gerçek piyasa durumu
    @GetMapping("/realmarket/status")
    public boolean isRealMarketActive() {
        return realMarketService.isActive();
    }

    // WebSocket bağlantı durumu (debug)
    @GetMapping("/realmarket/connection")
    public String getConnectionStatus() {
        return realMarketService.getConnectionStatus();
    }

    // Mesaj sayısı (debug)
    @GetMapping("/realmarket/messages")
    public long getMessageCount() {
        return realMarketService.getMessageCount();
    }

    /**
     * Historical Klines (Candlestick) Verisi
     * @param interval Zaman dilimi: 1m, 5m, 15m, 30m, 1h, 4h, 1d
     * @param limit Kaç mum (varsayılan 100, max 1000)
     * @return JSON array of candlesticks
     */
    @GetMapping("/klines")
    public List<Map<String, Object>> getKlines(
            @RequestParam(defaultValue = "5m") String interval,
            @RequestParam(defaultValue = "100") int limit) {

        List<RealMarketService.Candlestick> candles = realMarketService.getKlines(interval, limit);

        // Frontend için JSON formatına dönüştür
        return candles.stream().map(c -> {
            Map<String, Object> map = new HashMap<>();
            map.put("time", c.openTime);
            map.put("open", c.open);
            map.put("high", c.high);
            map.put("low", c.low);
            map.put("close", c.close);
            map.put("volume", c.volume);
            return map;
        }).collect(Collectors.toList());
    }


    @GetMapping("/wallet-logs")
    public List<Map<String, Object>> getWalletLogs() {
        return walletLogger.getLogs();
    }
}