package org.group8project.model;

import java.time.LocalDateTime;

public class MarketData {
    private String symbol;
    private double price;
    private LocalDateTime timestamp;

    public MarketData(String symbol, double price) {
        this.symbol = symbol;
        this.price = price;
        this.timestamp = LocalDateTime.now();
    }

    // Getterlar
    public String getSymbol() { return symbol; }
    public double getPrice() { return price; }
    public LocalDateTime getTimestamp() { return timestamp; }

    @Override
    public String toString() {
        return "MarketData{symbol='" + symbol + "', price=" + price + "}";
    }
}
