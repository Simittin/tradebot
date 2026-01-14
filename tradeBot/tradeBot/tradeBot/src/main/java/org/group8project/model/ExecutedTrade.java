package org.group8project.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ExecutedTrade {
    private String type;   // BUY veya SELL
    private double price;  // Fiyat
    private double amount; // Adet (BTC)
    private double total;  // Toplam Tutar ($)
    private String time;   // İşlem Saati

    public ExecutedTrade(String type, double price, double amount, double total) {
        this.type = type;
        this.price = price;
        this.amount = amount;
        this.total = total;
        this.time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
    }

    // Getter Metotları (Frontend'in okuması için ŞART)
    public String getType() { return type; }
    public double getPrice() { return price; }
    public double getAmount() { return amount; }
    public double getTotal() { return total; }
    public String getTime() { return time; }
}