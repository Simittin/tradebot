package org.group8project.service;

import org.group8project.patterns.singleton.MarketDataFeed;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * GERÇEK ZAMANLI PİYASA VERİSİ SERVİSİ
 * Binance WebSocket API kullanarak anlık Bitcoin fiyatı alır.
 * Throttle mekanizması ile akıcı güncelleme sağlar.
 * API Key gerektirmez, ücretsizdir.
 */
@Service
public class RealMarketService {

    private final MarketDataFeed marketDataFeed;
    private boolean active = false;
    private double lastRealPrice = 0.0;
    private double latestPrice = 0.0; // En son gelen fiyat (buffer)
    private BinanceWebSocket webSocketClient;

    // Throttle ayarları
    private long lastUpdateTime = 0;
    private static final long UPDATE_INTERVAL_MS = 500; // Her 500ms'de bir güncelle (saniyede 2 kez)

    // Konsol log throttle
    private long lastLogTime = 0;
    private static final long LOG_INTERVAL_MS = 1000; // Konsola 1 saniyede bir yaz

    // Debug: Mesaj sayacı
    private long messageCount = 0;
    private long lastMessageCountLog = 0;

    // Binance WebSocket - Gerçek zamanlı trade stream
    private static final String BINANCE_WS_URL = "wss://stream.binance.com:9443/ws/btcusdt@trade";
    
    // Binance REST API - Historical Klines
    private static final String BINANCE_KLINES_URL = "https://api.binance.com/api/v3/klines";

    public RealMarketService() {
        this.marketDataFeed = MarketDataFeed.getInstance();
    }

    /**
     * Candlestick (Mum) Verisi - İç sınıf
     */
    public static class Candlestick {
        public long openTime;
        public double open;
        public double high;
        public double low;
        public double close;
        public double volume;
        public long closeTime;

        public Candlestick(long openTime, double open, double high, double low, 
                          double close, double volume, long closeTime) {
            this.openTime = openTime;
            this.open = open;
            this.high = high;
            this.low = low;
            this.close = close;
            this.volume = volume;
            this.closeTime = closeTime;
        }
    }

    /**
     * Binance'ten historical klines (candlestick) verisi çek
     * @param interval Zaman dilimi: 1m, 5m, 15m, 30m, 1h, 4h, 1d
     * @param limit Kaç mum çekilecek (max 1000)
     * @return Candlestick listesi
     */
    public List<Candlestick> getKlines(String interval, int limit) {
        List<Candlestick> candles = new ArrayList<>();
        
        try {
            String urlStr = BINANCE_KLINES_URL + 
                "?symbol=BTCUSDT&interval=" + interval + "&limit=" + limit;
            
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);

            int responseCode = conn.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream())
                );
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                // JSON Array parse et
                candles = parseKlines(response.toString());
                
                System.out.println("[KLINES] ✅ " + candles.size() + " adet " + 
                    interval + " mum verisi çekildi.");
            } else {
                System.out.println("[KLINES] ❌ API Hatası: HTTP " + responseCode);
            }
            conn.disconnect();

        } catch (Exception e) {
            System.out.println("[KLINES] ❌ Hata: " + e.getMessage());
        }
        
        return candles;
    }

    /**
     * Binance klines JSON array'ini parse et
     * Format: [[openTime,open,high,low,close,volume,closeTime,...],...]
     */
    private List<Candlestick> parseKlines(String json) {
        List<Candlestick> candles = new ArrayList<>();
        
        try {
            // Basit JSON array parser
            // [[1234567890000,"97000.00","97500.00","96800.00","97200.00","123.456",1234567899999,...],...]
            
            json = json.trim();
            if (!json.startsWith("[[")) return candles;
            
            // Her bir mum array'ini bul
            int index = 1; // İlk [ atla
            while (index < json.length()) {
                int start = json.indexOf("[", index);
                if (start == -1) break;
                
                int end = json.indexOf("]", start);
                if (end == -1) break;
                
                String candleStr = json.substring(start + 1, end);
                String[] parts = candleStr.split(",");
                
                if (parts.length >= 7) {
                    long openTime = Long.parseLong(parts[0].trim());
                    double open = Double.parseDouble(parts[1].replace("\"", "").trim());
                    double high = Double.parseDouble(parts[2].replace("\"", "").trim());
                    double low = Double.parseDouble(parts[3].replace("\"", "").trim());
                    double close = Double.parseDouble(parts[4].replace("\"", "").trim());
                    double volume = Double.parseDouble(parts[5].replace("\"", "").trim());
                    long closeTime = Long.parseLong(parts[6].trim());
                    
                    candles.add(new Candlestick(openTime, open, high, low, close, volume, closeTime));
                }
                
                index = end + 1;
            }
            
        } catch (Exception e) {
            System.out.println("[KLINES] Parse hatası: " + e.getMessage());
        }
        
        return candles;
    }

    /**
     * Gerçek piyasa verisini aç/kapa
     */
    public void toggleRealMarket(boolean isActive) {
        this.active = isActive;
        
        if (isActive) {
            System.out.println("\n╔══════════════════════════════════════════════════════════╗");
            System.out.println("║  🌐 GERÇEK ZAMANLI PİYASA VERİSİ AKTİF!                   ║");
            System.out.println("║  📡 Kaynak: Binance WebSocket (wss://stream.binance.com) ║");
            System.out.println("║  ⚡ Güncelleme: Her 500ms (akıcı görüntü)                ║");
            System.out.println("║  💰 Çift: BTC/USDT                                       ║");
            System.out.println("╚══════════════════════════════════════════════════════════╝\n");
            
            connectWebSocket();
        } else {
            System.out.println("\n[REAL MARKET] 🔴 Gerçek piyasa verisi DURDURULDU.\n");
            disconnectWebSocket();
        }
    }

    /**
     * Binance WebSocket'e bağlan
     */
    private void connectWebSocket() {
        try {
            if (webSocketClient != null && webSocketClient.isOpen()) {
                webSocketClient.close();
            }

            webSocketClient = new BinanceWebSocket(new URI(BINANCE_WS_URL));
            webSocketClient.connect();
            
        } catch (Exception e) {
            System.out.println("[REAL MARKET] ❌ WebSocket Bağlantı Hatası: " + e.getMessage());
        }
    }

    /**
     * WebSocket bağlantısını kapat
     */
    private void disconnectWebSocket() {
        if (webSocketClient != null) {
            webSocketClient.close();
            webSocketClient = null;
        }
    }

    /**
     * Aktif durumu döndür
     */
    public boolean isActive() {
        return active;
    }

    /**
     * Son gerçek fiyatı döndür
     */
    public double getLastRealPrice() {
        return lastRealPrice;
    }

    /**
     * WebSocket bağlantı durumu
     */
    public String getConnectionStatus() {
        if (!active) return "KAPALI";
        if (webSocketClient == null) return "YOK";
        if (webSocketClient.isOpen()) return "BAĞLI ✅";
        if (webSocketClient.isClosing()) return "KAPANIYOR...";
        if (webSocketClient.isClosed()) return "KOPTU ❌";
        return "BEKLİYOR...";
    }

    /**
     * Debug: Toplam mesaj sayısı
     */
    public long getMessageCount() {
        return messageCount;
    }

    /**
     * Binance WebSocket Client - İç sınıf
     * Throttle mekanizması ile fiyatları yayınlar
     */
    private class BinanceWebSocket extends WebSocketClient {

        public BinanceWebSocket(URI serverUri) {
            super(serverUri);
        }

        @Override
        public void onOpen(ServerHandshake handshake) {
            System.out.println("[REAL MARKET] ✅ Binance WebSocket BAĞLANDI!");
            System.out.println("[REAL MARKET] 📊 Gerçek zamanlı BTC/USDT verisi akıyor...\n");
        }

        @Override
        public void onMessage(String message) {
            if (!active) return;

            try {
                // Debug: Mesaj sayacı
                messageCount++;

                // Fiyatı parse et
                double price = parseTradePrice(message);
                if (price <= 0) return;

                // En son fiyatı her zaman güncelle (buffer)
                latestPrice = price;

                long currentTime = System.currentTimeMillis();

                // Her zaman UI'ı güncelle (throttle kaldırıldı - daha akıcı)
                if (currentTime - lastUpdateTime >= UPDATE_INTERVAL_MS) {
                    lastUpdateTime = currentTime;
                    
                    // Her zaman yayınla (fiyat aynı olsa bile UI güncel kalsın)
                    lastRealPrice = latestPrice;
                    marketDataFeed.publishPrice("BTC", lastRealPrice);
                }

                // Konsol logu - debug bilgisiyle
                if (currentTime - lastLogTime >= LOG_INTERVAL_MS) {
                    long messagesPerSecond = messageCount - lastMessageCountLog;
                    lastMessageCountLog = messageCount;
                    lastLogTime = currentTime;
                    
                    System.out.println("[REAL MARKET] 💰 BTC/USDT: $" + 
                        String.format("%,.4f", lastRealPrice) + 
                        " | 📨 " + messagesPerSecond + " msg/sn");
                }

            } catch (Exception e) {
                System.out.println("[REAL MARKET] ⚠️ Parse hatası: " + e.getMessage());
            }
        }

        @Override
        public void onClose(int code, String reason, boolean remote) {
            System.out.println("[REAL MARKET] 🔌 WebSocket Kapandı: " + reason);
            
            // Eğer hala aktifse yeniden bağlanmayı dene
            if (active) {
                System.out.println("[REAL MARKET] 🔄 Yeniden bağlanılıyor...");
                new Thread(() -> {
                    try {
                        Thread.sleep(3000);
                        if (active) {
                            connectWebSocket();
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }).start();
            }
        }

        @Override
        public void onError(Exception ex) {
            System.out.println("[REAL MARKET] ❌ WebSocket Hatası: " + ex.getMessage());
        }
    }

    /**
     * Binance trade mesajından fiyatı parse et
     * Format: {"p":"97432.12345678",...}
     */
    private double parseTradePrice(String json) {
        try {
            int pIndex = json.indexOf("\"p\":\"");
            if (pIndex == -1) return 0;

            int startIndex = pIndex + 5;
            int endIndex = json.indexOf("\"", startIndex);
            
            String priceStr = json.substring(startIndex, endIndex);
            return Double.parseDouble(priceStr);
            
        } catch (Exception e) {
            return 0;
        }
    }
}
